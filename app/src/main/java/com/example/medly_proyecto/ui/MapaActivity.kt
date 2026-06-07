package com.example.medly_proyecto.ui

import android.Manifest
import android.app.ActivityOptions
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.Farmacia
import com.example.medly_proyecto.ui.adapter.FarmaciaAdapter
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import java.util.*
import com.airbnb.lottie.LottieAnimationView

class MapaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var farmaciaAdapter: FarmaciaAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var placesClient: PlacesClient
    
    private lateinit var layoutList: LinearLayout
    private lateinit var layoutDetail: LinearLayout
    private lateinit var tvFarmaciaNombre: TextView
    private lateinit var tvDistancia: TextView
    private lateinit var tvTiempo: TextView
    private lateinit var btnComenzar: MaterialButton
    private lateinit var btnBack: ImageButton
    
    private lateinit var pbSearching: ProgressBar
    private lateinit var tvNoResults: TextView

    private var myLocation: Location? = null
    private var selectedFarmacia: Farmacia? = null
    private val listaActualFarmacias = mutableListOf<Farmacia>()

    private var imageUri: Uri? = null
    private var scanType: String = "" // "RECETA" o "HORA"

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
            when (scanType) {
                "RECETA" -> {
                    val intent = Intent(this, RecetaDetalleActivity::class.java).apply {
                        putExtra("RECIPE_IMAGE_URI", imageUri.toString())
                        putExtra("IS_NEW_RECIPE", true)
                    }
                    startActivity(intent, options.toBundle())
                }
                "HORA" -> {
                    val intent = Intent(this, CitaDetalleActivity::class.java).apply {
                        putExtra("APPOINTMENT_IMAGE_URI", imageUri.toString())
                        putExtra("IS_NEW_APPOINTMENT", true)
                    }
                    startActivity(intent, options.toBundle())
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mapa)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyBrLOKVIpuG2oaTddGVU8Xww8kcr5kjqaI")
        }
        placesClient = Places.createClient(this)

        setupUI()
        configurarNavegacion()
        setupOnBackPressed()
        
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (myLocation == null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
                        buscarFarmaciasReales(location)
                    }
                    myLocation = location
                    actualizarCalculosEnTiempoReal(location)
                }
            }
        }

        findViewById<View>(R.id.scanContainer).setOnClickListener {
            mostrarBottomSheetEscaneo()
        }
    }

    private fun mostrarBottomSheetEscaneo() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_scan, null)
        
        view.findViewById<View>(R.id.btnScanReceta).setOnClickListener {
            scanType = "RECETA"
            checkCameraPermission()
            dialog.dismiss()
        }
        
        view.findViewById<View>(R.id.btnScanHora).setOnClickListener {
            scanType = "HORA"
            checkCameraPermission()
            dialog.dismiss()
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val title = if (scanType == "RECETA") "Nueva Receta" else "Cita Médica"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, title)
        }
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        }
        takePictureLauncher.launch(intent)
    }

    private fun setupUI() {
        layoutList = findViewById(R.id.layoutList)
        layoutDetail = findViewById(R.id.layoutDetail)
        tvFarmaciaNombre = findViewById(R.id.tvFarmaciaNombre)
        tvDistancia = findViewById(R.id.tvDistancia)
        tvTiempo = findViewById(R.id.tvTiempo)
        btnComenzar = findViewById(R.id.btnComenzar)
        btnBack = findViewById(R.id.btnBack)
        
        pbSearching = findViewById(R.id.pbSearching)
        tvNoResults = findViewById(R.id.tvNoResultsMapa)

        val bottomSheet = findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = false

        val rvFarmacias = findViewById<RecyclerView>(R.id.rvFarmacias)
        rvFarmacias.layoutManager = LinearLayoutManager(this)
        farmaciaAdapter = FarmaciaAdapter(emptyList()) { farmacia ->
            mostrarDetalleFarmacia(farmacia)
        }
        rvFarmacias.adapter = farmaciaAdapter

        btnBack.setOnClickListener {
            selectedFarmacia = null
            layoutDetail.visibility = View.GONE
            layoutList.visibility = View.VISIBLE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        btnComenzar.setOnClickListener {
            selectedFarmacia?.let {
                val uri = Uri.parse("google.navigation:q=${it.ubicacion.latitude},${it.ubicacion.longitude}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                startActivity(intent)
            }
        }
    }

    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (layoutDetail.visibility == View.VISIBLE) {
                    btnBack.performClick()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun buscarFarmaciasReales(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        
        pbSearching.visibility = View.VISIBLE
        tvNoResults.visibility = View.GONE

        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION
        )

        val restriction = CircularBounds.newInstance(latLng, 5000.0)
        
        val searchNearbyRequest = SearchNearbyRequest.builder(restriction, placeFields)
            .setIncludedTypes(listOf("pharmacy"))
            .setMaxResultCount(15)
            .build()

        placesClient.searchNearby(searchNearbyRequest)
            .addOnSuccessListener { response ->
                pbSearching.visibility = View.GONE
                listaActualFarmacias.clear()
                
                for (place in response.places) {
                    place.location?.let { loc ->
                        listaActualFarmacias.add(Farmacia(
                            id = place.id ?: UUID.randomUUID().toString(),
                            nombre = place.displayName ?: "Farmacia",
                            direccion = place.formattedAddress ?: "Dirección no disponible",
                            ubicacion = loc
                        ))
                    }
                }
                
                if (listaActualFarmacias.isEmpty()) {
                    tvNoResults.text = "No se hallaron farmacias reales. Usando demo."
                    tvNoResults.visibility = View.VISIBLE
                    generarFarmaciasCercanasSimuladas(location)
                } else {
                    tvNoResults.visibility = View.GONE
                    actualizarCalculosEnTiempoReal(location)
                }
            }
            .addOnFailureListener { exception ->
                pbSearching.visibility = View.GONE
                Log.e("MapaActivity", "Error de API: ${exception.message}")
                tvNoResults.text = "Error de conexión. Usando modo demo."
                tvNoResults.visibility = View.VISIBLE
                generarFarmaciasCercanasSimuladas(location)
            }
    }

    private fun generarFarmaciasCercanasSimuladas(location: Location) {
        listaActualFarmacias.clear()
        val nombres = listOf("Farmacia Cruz Verde (Demo)", "Farmacia Ahumada (Demo)", "Salcobrand (Demo)", "Dr. Simi (Demo)", "Farmacia Local (Demo)")
        val random = Random()
        
        for (i in nombres.indices) {
            val lat = location.latitude + (random.nextDouble() - 0.5) / 150
            val lng = location.longitude + (random.nextDouble() - 0.5) / 150
            val f = Farmacia(i.toString(), nombres[i], "Calle de prueba $i", LatLng(lat, lng))
            listaActualFarmacias.add(f)
        }
        actualizarCalculosEnTiempoReal(location)
    }

    private fun actualizarCalculosEnTiempoReal(ubicacionActual: Location) {
        for (f in listaActualFarmacias) {
            val results = FloatArray(1)
            Location.distanceBetween(ubicacionActual.latitude, ubicacionActual.longitude, f.ubicacion.latitude, f.ubicacion.longitude, results)
            val dist = results[0]
            
            f.distanciaMetros = dist
            f.distancia = if (dist < 1000) "${dist.toInt()} m" else String.format(Locale.US, "%.1f km", dist / 1000)
            
            val min = (dist / 80).toInt()
            f.tiempo = if (min < 1) "1 min" else "$min min"
        }

        val filtradas = listaActualFarmacias.sortedBy { it.distanciaMetros }
        farmaciaAdapter.updateList(filtradas)
        
        selectedFarmacia?.let { sel ->
            val updated = filtradas.find { it.id == sel.id }
            updated?.let {
                tvDistancia.text = it.distancia
                tvTiempo.text = it.tiempo
            }
        }

        dibujarMarcadores(filtradas)
    }

    private fun dibujarMarcadores(farmacias: List<Farmacia>) {
        if (!::mMap.isInitialized) return
        mMap.clear()
        for (f in farmacias) {
            val marker = mMap.addMarker(MarkerOptions()
                .position(f.ubicacion)
                .title(f.nombre)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
            marker?.tag = f
        }
    }

    private fun mostrarDetalleFarmacia(farmacia: Farmacia) {
        selectedFarmacia = farmacia
        layoutList.visibility = View.GONE
        layoutDetail.visibility = View.VISIBLE
        
        tvFarmaciaNombre.text = farmacia.nombre
        tvDistancia.text = farmacia.distancia
        tvTiempo.text = farmacia.tiempo
        
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(farmacia.ubicacion, 16f))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMapToolbarEnabled = false

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
        
        mMap.isMyLocationEnabled = true
        startLocationUpdates()

        mMap.setOnMarkerClickListener { marker ->
            (marker.tag as? Farmacia)?.let { mostrarDetalleFarmacia(it) }
            true
        }
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
    }

    private fun configurarNavegacion() {
        findViewById<View>(R.id.btnHomeNav).setOnClickListener {
            irAActivity("HomeActivity")
        }
        
        findViewById<View>(R.id.btnRecetasNav).setOnClickListener {
            irAActivity("RecetasMedicasActivity")
        }
        
        findViewById<View>(R.id.btnMapasNav).setOnClickListener {
            // Ya estamos en Mapas
        }
        
        findViewById<View>(R.id.btnPerfilNav).setOnClickListener {
            irAActivity("perfilActivity")
        }
    }

    private fun irAActivity(className: String) {
        try {
            val intent = Intent(this, Class.forName("com.example.medly_proyecto.ui.$className"))
            val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(intent, options.toBundle())
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Pantalla en desarrollo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        if (::mMap.isInitialized) startLocationUpdates()
    }
}
