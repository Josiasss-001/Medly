package com.example.medly_proyecto.ui

import android.Manifest
import android.app.ActivityOptions
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.Farmacia
import com.example.medly_proyecto.ui.adapter.FarmaciaAdapter
import com.example.medly_proyecto.viewmodel.PerfilViewModel
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
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class MapaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var farmaciaAdapter: FarmaciaAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var placesClient: PlacesClient
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navHeaderName: TextView
    private lateinit var navHeaderEmail: TextView
    private lateinit var navHeaderProfileImg: com.google.android.material.imageview.ShapeableImageView
    private lateinit var navHeaderBackground: ImageView

    private lateinit var layoutList: LinearLayout
    private lateinit var layoutDetail: LinearLayout
    private lateinit var tvFarmaciaNombre: TextView
    private lateinit var tvDistancia: TextView
    private lateinit var tvTiempo: TextView
    private lateinit var btnComenzar: MaterialButton
    private lateinit var btnBackDetail: ImageButton
    private lateinit var tvTituloLista: TextView
    
    private lateinit var pbSearching: ProgressBar
    private lateinit var tvNoResults: TextView
    private lateinit var toggleGroup: MaterialButtonToggleGroup

    private val viewModel: PerfilViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    private var myLocation: Location? = null
    private var selectedFarmacia: Farmacia? = null
    private val listaActualLugares = mutableListOf<Farmacia>()
    private var currentTipoBusqueda = "FARMACIA" // "FARMACIA" o "CESFAM"

    private var imageUri: Uri? = null
    private var scanType: String = "" // "RECETA", "HORA" o "DOCUMENTO"

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) openCamera() else Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            imageUri?.let { redireccionarSegunTipo(it.toString()) }
        }
    }

    private val pickDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { redireccionarSegunTipo(it.toString()) }
    }

    private fun redireccionarSegunTipo(uriString: String) {
        val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
        when (scanType) {
            "RECETA" -> {
                val intent = Intent(this, RecetaDetalleActivity::class.java).apply {
                    putExtra("RECIPE_IMAGE_URI", uriString)
                    putExtra("IS_NEW_RECIPE", true)
                }
                startActivity(intent, options.toBundle())
            }
            "HORA" -> {
                val intent = Intent(this, CitaDetalleActivity::class.java).apply {
                    putExtra("APPOINTMENT_IMAGE_URI", uriString)
                    putExtra("IS_NEW_APPOINTMENT", true)
                }
                startActivity(intent, options.toBundle())
            }
            "DOCUMENTO" -> {
                val intent = Intent(this, DocumentoDetalleActivity::class.java).apply {
                    putExtra("DOC_IMAGE_URI", uriString)
                    putExtra("IS_NEW_DOC", true)
                }
                startActivity(intent, options.toBundle())
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

        drawerLayout = findViewById(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0)
        
        navHeaderName = headerView.findViewById(R.id.nav_header_name)
        navHeaderEmail = headerView.findViewById(R.id.nav_header_email)
        navHeaderProfileImg = headerView.findViewById(R.id.nav_header_profile_img)
        navHeaderBackground = headerView.findViewById(R.id.nav_header_background)

        findViewById<ImageView>(R.id.menuIcon).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        setupUI()
        configurarNavegacion()
        configurarDrawer(navigationView)
        observarViewModel()
        setupOnBackPressed()
        
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (myLocation == null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
                        buscarLugaresCercanos(location)
                    }
                    myLocation = location
                    actualizarCalculosEnTiempoReal(location)
                }
            }
        }

        findViewById<View>(R.id.contenedorEscaner).setOnClickListener {
            mostrarBottomSheetEscaneo()
        }
        
        auth.currentUser?.uid?.let { viewModel.loadProfile(it) }
        navHeaderEmail.text = auth.currentUser?.email ?: ""
    }

    private fun observarViewModel() {
        viewModel.usuario.observe(this) { usuario ->
            usuario?.let { navHeaderName.text = it.nombreCompleto }
        }
        viewModel.perfilImagenes.observe(this) { imagenes ->
            imagenes?.let {
                if (it.profileImageUrl.isNotEmpty()) {
                    val bitmap = base64ToBitmap(it.profileImageUrl)
                    navHeaderProfileImg.setImageBitmap(bitmap)
                    // Cargar foto en la barra de navegación inferior
                    findViewById<ImageView>(R.id.botonPerfilNav)?.setImageBitmap(bitmap)
                }
                if (it.backgroundImageUrl.isNotEmpty()) navHeaderBackground.setImageBitmap(base64ToBitmap(it.backgroundImageUrl))
            }
        }
    }

    private fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) { null }
    }

    private fun configurarDrawer(navigationView: NavigationView) {
        navigationView.setCheckedItem(R.id.nav_mapas)
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> irAActivity("HomeActivity")
                R.id.nav_recetas -> irAActivityConFiltro("DocumentosActivity", "RECETA")
                R.id.nav_horas -> irAActivityConFiltro("DocumentosActivity", "CITA")
                R.id.nav_mapas -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_perfil -> irAActivity("perfilActivity")
                R.id.nav_logout -> cerrarSesion()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun cerrarSesion() {
        auth.signOut()
        startActivity(Intent(this, AuthActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
        finish()
    }

    private fun mostrarBottomSheetEscaneo() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_scan, null)
        
        view.findViewById<View>(R.id.btnScanReceta).setOnClickListener {
            dialog.dismiss()
            mostrarDialogoSeleccion("RECETA")
        }
        
        view.findViewById<View>(R.id.btnScanHora).setOnClickListener {
            dialog.dismiss()
            mostrarDialogoSeleccion("HORA")
        }

        view.findViewById<View>(R.id.btnScanDocumento).setOnClickListener {
            dialog.dismiss()
            scanType = "DOCUMENTO"
            pickDocumentLauncher.launch(arrayOf("image/*", "application/pdf"))
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun mostrarDialogoSeleccion(tipo: String) {
        scanType = tipo
        val view = layoutInflater.inflate(R.layout.layout_dialog_source_choice, null)
        val dialog = AlertDialog.Builder(this).setView(view).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.findViewById<TextView>(R.id.tvDialogTitle).text = "Subir $tipo"
        view.findViewById<View>(R.id.btnChoiceCamera).setOnClickListener {
            dialog.dismiss()
            checkCameraPermission()
        }
        view.findViewById<View>(R.id.btnChoiceGallery).setOnClickListener {
            dialog.dismiss()
            pickDocumentLauncher.launch(arrayOf("image/*", "application/pdf"))
        }
        view.findViewById<View>(R.id.btnCancelChoice).setOnClickListener { dialog.dismiss() }
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
        val intent = Intent(this, ScannerCameraActivity::class.java)
        takePictureLauncher.launch(intent)
    }

    private fun setupUI() {
        layoutList = findViewById(R.id.layoutList)
        layoutDetail = findViewById(R.id.layoutDetail)
        tvFarmaciaNombre = findViewById(R.id.tvFarmaciaNombre)
        tvDistancia = findViewById(R.id.tvDistancia)
        tvTiempo = findViewById(R.id.tvTiempo)
        btnComenzar = findViewById(R.id.btnComenzar)
        btnBackDetail = findViewById(R.id.btnBackDetail)
        tvTituloLista = findViewById(R.id.tvTituloLista)
        
        pbSearching = findViewById(R.id.pbSearching)
        tvNoResults = findViewById(R.id.tvNoResultsMapa)
        toggleGroup = findViewById(R.id.toggleGroup)

        val bottomSheet = findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = false

        val rvFarmacias = findViewById<RecyclerView>(R.id.rvFarmacias)
        rvFarmacias.layoutManager = LinearLayoutManager(this)
        farmaciaAdapter = FarmaciaAdapter(emptyList()) { lugar ->
            mostrarDetalleLugar(lugar)
        }
        rvFarmacias.adapter = farmaciaAdapter

        btnBackDetail.setOnClickListener {
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

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnSearchFarmacias -> {
                        currentTipoBusqueda = "FARMACIA"
                        tvTituloLista.text = "Farmacias cercanas"
                    }
                    R.id.btnSearchCesfam -> {
                        currentTipoBusqueda = "CESFAM"
                        tvTituloLista.text = "CESFAMs cercanos"
                    }
                }
                myLocation?.let { buscarLugaresCercanos(it) }
                if (layoutDetail.visibility == View.VISIBLE) {
                    btnBackDetail.performClick()
                }
            }
        }
    }

    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else if (layoutDetail.visibility == View.VISIBLE) {
                    btnBackDetail.performClick()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun buscarLugaresCercanos(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        pbSearching.visibility = View.VISIBLE
        tvNoResults.visibility = View.GONE

        val placeFields = listOf(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS, Place.Field.LOCATION)
        val restriction = CircularBounds.newInstance(latLng, 5000.0)
        val typeToSearch = if (currentTipoBusqueda == "FARMACIA") "pharmacy" else "hospital"
        val searchNearbyRequest = SearchNearbyRequest.builder(restriction, placeFields)
            .setIncludedTypes(listOf(typeToSearch))
            .setMaxResultCount(15)
            .build()

        placesClient.searchNearby(searchNearbyRequest)
            .addOnSuccessListener { response ->
                pbSearching.visibility = View.GONE
                listaActualLugares.clear()
                for (place in response.places) {
                    place.location?.let { loc ->
                        listaActualLugares.add(Farmacia(
                            id = place.id ?: UUID.randomUUID().toString(),
                            nombre = place.displayName ?: if(currentTipoBusqueda == "FARMACIA") "Farmacia" else "CESFAM",
                            direccion = place.formattedAddress ?: "Dirección no disponible",
                            ubicacion = loc,
                            tipo = currentTipoBusqueda
                        ))
                    }
                }
                if (listaActualLugares.isEmpty()) {
                    val label = if (currentTipoBusqueda == "FARMACIA") "farmacias" else "CESFAMs"
                    tvNoResults.text = "No se hallaron $label reales. Usando demo."
                    tvNoResults.visibility = View.VISIBLE
                    generarLugaresSimulados(location)
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
                generarLugaresSimulados(location)
            }
    }

    private fun generarLugaresSimulados(location: Location) {
        listaActualLugares.clear()
        val nombres = if (currentTipoBusqueda == "FARMACIA") {
            listOf("Farmacia Cruz Verde (Demo)", "Farmacia Ahumada (Demo)", "Salcobrand (Demo)", "Dr. Simi (Demo)", "Farmacia Local (Demo)")
        } else {
            listOf("CESFAM Salvador Bustos (Demo)", "CESFAM Segismundo Iturra (Demo)", "CESFAM Dr. Jorge Sabat (Demo)", "CESFAM Remigio Sapunar (Demo)")
        }
        val random = Random()
        for (i in nombres.indices) {
            val lat = location.latitude + (random.nextDouble() - 0.5) / 150
            val lng = location.longitude + (random.nextDouble() - 0.5) / 150
            listaActualLugares.add(Farmacia(id = "demo_$i", nombre = nombres[i], direccion = "Dirección Simulada $i", ubicacion = LatLng(lat, lng), tipo = currentTipoBusqueda))
        }
        actualizarCalculosEnTiempoReal(location)
    }

    private fun actualizarCalculosEnTiempoReal(ubicacionActual: Location) {
        for (f in listaActualLugares) {
            val results = FloatArray(1)
            Location.distanceBetween(ubicacionActual.latitude, ubicacionActual.longitude, f.ubicacion.latitude, f.ubicacion.longitude, results)
            val dist = results[0]
            f.distanciaMetros = dist
            f.distancia = if (dist < 1000) "${dist.toInt()} m" else String.format(Locale.US, "%.1f km", dist / 1000)
            val min = (dist / 80).toInt()
            f.tiempo = if (min < 1) "1 min" else "$min min"
        }
        val filtradas = listaActualLugares.sortedBy { it.distanciaMetros }
        farmaciaAdapter.updateList(filtradas)
        selectedFarmacia?.let { sel ->
            filtradas.find { it.id == sel.id }?.let {
                tvDistancia.text = it.distancia
                tvTiempo.text = it.tiempo
            }
        }
        dibujarMarcadores(filtradas)
    }

    private fun dibujarMarcadores(lugares: List<Farmacia>) {
        if (!::mMap.isInitialized) return
        mMap.clear()
        for (f in lugares) {
            val color = if (f.tipo == "CESFAM") BitmapDescriptorFactory.HUE_RED else BitmapDescriptorFactory.HUE_AZURE
            val marker = mMap.addMarker(MarkerOptions().position(f.ubicacion).title(f.nombre).icon(BitmapDescriptorFactory.defaultMarker(color)))
            marker?.tag = f
        }
    }

    private fun mostrarDetalleLugar(lugar: Farmacia) {
        selectedFarmacia = lugar
        layoutList.visibility = View.GONE
        layoutDetail.visibility = View.VISIBLE
        tvFarmaciaNombre.text = lugar.nombre
        tvDistancia.text = lugar.distancia
        tvTiempo.text = lugar.tiempo
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lugar.ubicacion, 16f))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.setPadding(0, 300, 0, 0)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
        mMap.isMyLocationEnabled = true
        startLocationUpdates()
        mMap.setOnMarkerClickListener { marker ->
            (marker.tag as? Farmacia)?.let { mostrarDetalleLugar(it) }
            true
        }
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).setMinUpdateIntervalMillis(2000).build()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        }
    }

    private fun configurarNavegacion() {
        findViewById<View>(R.id.botonInicioNav).setOnClickListener { irAActivity("HomeActivity") }
        findViewById<View>(R.id.botonDocsNav).setOnClickListener { irAActivity("DocumentosActivity") }
        findViewById<View>(R.id.botonPerfilNav).setOnClickListener { irAActivity("perfilActivity") }
    }

    private fun irAActivity(className: String) {
        try {
            val intent = Intent(this, Class.forName("com.example.medly_proyecto.ui.$className"))
            val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(intent, options.toBundle())
            if (className == "HomeActivity") finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Pantalla en desarrollo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun irAActivityConFiltro(className: String, filtro: String) {
        try {
            val intent = Intent(this, Class.forName("com.example.medly_proyecto.ui.$className"))
            intent.putExtra("INITIAL_FILTER", filtro)
            startActivity(intent, ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle())
        } catch (e: Exception) { Toast.makeText(this, "Pantalla en desarrollo", Toast.LENGTH_SHORT).show() }
    }

    override fun onPause() { super.onPause(); fusedLocationClient.removeLocationUpdates(locationCallback) }
    override fun onResume() { super.onResume(); if (::mMap.isInitialized) startLocationUpdates() }
}
