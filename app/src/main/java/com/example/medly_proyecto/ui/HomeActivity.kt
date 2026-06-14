package com.example.medly_proyecto.ui

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.example.medly_proyecto.BuildConfig
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.TipoAlerta
import com.example.medly_proyecto.notification.NotificationSyncWorker
import com.example.medly_proyecto.ui.adapter.AlertaAdapter
import com.example.medly_proyecto.ui.adapter.BannerAdapter
import com.example.medly_proyecto.ui.adapter.HomeCarouselAdapter
import com.example.medly_proyecto.ui.adapter.NotificacionesAdapter
import com.example.medly_proyecto.ui.adapter.SugerenciaAdapter
import com.example.medly_proyecto.viewmodel.HomeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class HomeActivity : AppCompatActivity() {

    private lateinit var userNameTextView: TextView
    private lateinit var profileCircle: ShapeableImageView
    private lateinit var drawerLayout: DrawerLayout
    
    private lateinit var navHeaderName: TextView
    private lateinit var navHeaderEmail: TextView
    private lateinit var navHeaderProfileImg: ShapeableImageView
    private lateinit var navHeaderBackground: ImageView

    private lateinit var searchEditText: EditText
    private lateinit var tvQuick: TextView
    private lateinit var statsCard: View
    private lateinit var tvStats: View
    private lateinit var tvNoResults: TextView
    private lateinit var suggestionsCard: View
    private lateinit var rvSuggestions: RecyclerView
    private lateinit var sugerenciaAdapter: SugerenciaAdapter

    private lateinit var btnRecetas: View
    private lateinit var btnCitas: View
    private lateinit var btnTratamiento: View
    private lateinit var btnCredencial: View

    private lateinit var tvPesoHome: TextView
    private lateinit var tvAlturaHome: TextView
    private lateinit var tvIMCHome: TextView
    private lateinit var tvIMCEstadoHome: TextView
    private lateinit var tvEnfermedadHome: TextView
    
    private lateinit var tvNotifCounter: TextView
    private lateinit var btnNotificationBell: View
    private lateinit var notifBadgeIndicator: View
    private lateinit var lottieBell: LottieAnimationView
    
    private lateinit var rvAlertas: RecyclerView
    private lateinit var alertaAdapter: AlertaAdapter
    
    private lateinit var vpBannerCarousel: ViewPager2
    private lateinit var layoutDots: LinearLayout
    private lateinit var vpHomeCarousel: ViewPager2
    private lateinit var carouselAdapter: HomeCarouselAdapter
    
    private val carouselHandler = Handler(Looper.getMainLooper())
    private val autoScrollRunnable = Runnable {
        if (::vpBannerCarousel.isInitialized && vpBannerCarousel.adapter != null) {
            val itemCount = vpBannerCarousel.adapter!!.itemCount
            if (itemCount > 1) {
                val nextItem = (vpBannerCarousel.currentItem + 1) % itemCount
                vpBannerCarousel.setCurrentItem(nextItem, true)
            }
            startAutoScroll()
        }
    }

    private val viewModel: HomeViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private var imageUri: Uri? = null
    private var scanType: String = ""

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera() else Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) Toast.makeText(this, "No recibirás recordatorios", Toast.LENGTH_LONG).show()
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriStr = result.data?.getStringExtra("SCANNED_IMAGE_URI")
            if (uriStr != null) {
                procesarDocumentoSeleccionado(Uri.parse(uriStr))
            }
        }
    }

    private val pickDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { 
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: Exception) {}
            procesarDocumentoSeleccionado(it) 
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        
        drawerLayout = findViewById(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0)
        
        navHeaderName = headerView.findViewById(R.id.nav_header_name)
        navHeaderEmail = headerView.findViewById(R.id.nav_header_email)
        navHeaderProfileImg = headerView.findViewById(R.id.nav_header_profile_img)
        navHeaderBackground = headerView.findViewById(R.id.nav_header_background)

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        findViewById<View>(R.id.navizquieda)?.setOnClickListener { 
            drawerLayout.openDrawer(GravityCompat.START) 
        }

        userNameTextView = findViewById(R.id.userNameTextView)
        profileCircle = findViewById(R.id.profileCircle)

        searchEditText = findViewById(R.id.searchEditText)
        tvQuick = findViewById(R.id.tvQuick)
        statsCard = findViewById(R.id.statsCard)
        tvStats = findViewById(R.id.tvStats)
        tvNoResults = findViewById(R.id.tvNoResults)
        suggestionsCard = findViewById(R.id.suggestionsCard)
        rvSuggestions = findViewById(R.id.rvSuggestions)

        btnRecetas = findViewById(R.id.btnRecetasHome)
        btnCitas = findViewById(R.id.btnCitasHome)
        btnTratamiento = findViewById(R.id.btnTratamientoHome)
        btnCredencial = findViewById(R.id.btnCredencialHome)

        tvPesoHome = findViewById(R.id.tvPesoHome)
        tvAlturaHome = findViewById(R.id.tvAlturaHome)
        tvIMCHome = findViewById(R.id.tvIMCHome)
        tvIMCEstadoHome = findViewById(R.id.tvIMCEstadoHome)
        tvEnfermedadHome = findViewById(R.id.tvEnfermedadHome)
        
        tvNotifCounter = findViewById(R.id.tvNotifCounter)
        btnNotificationBell = findViewById(R.id.btnNotificationBell)
        notifBadgeIndicator = findViewById(R.id.notifBadgeIndicator)
        lottieBell = findViewById(R.id.lottieBell)

        configurarBannerCarousel()
        configurarCarousel()
        configurarRecyclerViewSugerencias()
        configurarRecyclerViewAlertas()
        observarViewModel()
        
        auth.currentUser?.uid?.let { 
            viewModel.cargarDatos(it)
            iniciarSincronizacionNotificaciones()
        }
        
        navHeaderEmail.text = auth.currentUser?.email ?: ""
        viewModel.iniciarFrases(BuildConfig.OPENAI_API_KEY)

        configurarNavegacion()
        configurarDrawer(navigationView)
        configurarBusqueda()
        configurarListenersCategorias()
        verificarPermisoNotificaciones()

        btnNotificationBell.setOnClickListener { mostrarBottomSheetNotificaciones() }

        statsCard.setOnClickListener { irAActivity("EstadisticasActivity") }
        tvStats.setOnClickListener { irAActivity("EstadisticasActivity") }
        findViewById<View>(R.id.btnVerMasSalud).setOnClickListener { irAActivity("perfilActivity") }
        findViewById<View>(R.id.llNotifHeader).setOnClickListener { irAActivity("CalendarioTratamientoActivity") }
        
        findViewById<View>(R.id.contenedorEscaner).setOnClickListener { 
            mostrarBottomSheetEscaneo()
        }
    }

    private fun configurarRecyclerViewAlertas() {
        rvAlertas = findViewById(R.id.rvAlertas)
        alertaAdapter = AlertaAdapter { alerta ->
            when (alerta.tipo) {
                TipoAlerta.CITA -> {
                    val cita = viewModel.getCitaById(alerta.dataId)
                    if (cita != null) {
                        val intent = Intent(this, CitaDetalleActivity::class.java)
                        intent.putExtra("CITA_OBJ", cita)
                        intent.putExtra("IS_NEW_APPOINTMENT", false)
                        startActivity(intent)
                    } else {
                        irAActivityConFiltro("DocumentosActivity", "CITA")
                    }
                }
                TipoAlerta.RECETA -> {
                    val receta = viewModel.getRecetaById(alerta.dataId)
                    if (receta != null) {
                        val intent = Intent(this, RecetaDetalleActivity::class.java)
                        intent.putExtra("RECETA_OBJ", receta)
                        intent.putExtra("IS_NEW_RECIPE", false)
                        startActivity(intent)
                    } else {
                        irAActivityConFiltro("DocumentosActivity", "RECETA")
                    }
                }
                TipoAlerta.DOSIS -> irAActivity("CalendarioTratamientoActivity")
            }
        }
        rvAlertas.layoutManager = LinearLayoutManager(this)
        rvAlertas.adapter = alertaAdapter
    }

    private fun iniciarSincronizacionNotificaciones() {
        val syncRequest = PeriodicWorkRequestBuilder<NotificationSyncWorker>(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NotificationSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun verificarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent().apply {
                    action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                }
                startActivity(intent)
            }
        }
    }

    private fun observarViewModel() {
        viewModel.usuario.observe(this) { usuario ->
            usuario?.let {
                val nameToShow = it.nombres.ifEmpty { it.nombreCompleto.split(" ").firstOrNull() ?: "Usuario" }
                userNameTextView.text = "Hola $nameToShow,"
                navHeaderName.text = it.nombreCompleto
            }
        }

        viewModel.perfilImagenes.observe(this) { imagenes ->
            imagenes?.let {
                if (it.profileImageUrl.isNotEmpty()) {
                    val bitmap = base64ToBitmap(it.profileImageUrl)
                    profileCircle.setImageBitmap(bitmap)
                    navHeaderProfileImg.setImageBitmap(bitmap)
                    findViewById<ImageView>(R.id.botonPerfilNav)?.setImageBitmap(bitmap)
                }
                if (it.backgroundImageUrl.isNotEmpty()) {
                    navHeaderBackground.setImageBitmap(base64ToBitmap(it.backgroundImageUrl))
                }
            }
        }

        viewModel.alertas.observe(this) { alertas ->
            alertaAdapter.submitList(alertas)
            findViewById<View>(R.id.llAlertasHeader).visibility = if (alertas.isNotEmpty()) View.VISIBLE else View.GONE
            rvAlertas.visibility = if (alertas.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.estadoVisibilidad.observe(this) { estado ->
            val isVisible = if (estado.recetasVisible || estado.citasVisible) View.VISIBLE else View.GONE
            findViewById<View>(R.id.gridCategories).visibility = isVisible
            tvQuick.visibility = isVisible
            statsCard.visibility = if (estado.statsVisible) View.VISIBLE else View.GONE
            tvStats.visibility = if (estado.statsVisible) View.VISIBLE else View.GONE
            tvNoResults.visibility = if (estado.sinResultadosVisible) View.GONE else View.VISIBLE
            findViewById<View>(R.id.clHealthSummary).visibility = if (estado.statsVisible) View.VISIBLE else View.GONE
            findViewById<View>(R.id.llNotifHeader).visibility = isVisible
            vpHomeCarousel.visibility = isVisible
            vpBannerCarousel.visibility = if (estado.sinResultadosVisible) View.GONE else View.VISIBLE
            layoutDots.visibility = if (estado.sinResultadosVisible) View.GONE else View.VISIBLE
        }
        
        viewModel.sugerencias.observe(this) { lista ->
            sugerenciaAdapter.actualizarLista(lista)
            suggestionsCard.visibility = if (lista.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.datosMedicos.observe(this) { datos ->
            datos?.let {
                tvPesoHome.text = String.format(Locale.getDefault(), "%.1f kg", it.peso)
                tvAlturaHome.text = String.format(Locale.getDefault(), "%.2f m", it.estatura / 100.0)
                
                val (imc, estado) = viewModel.calcularIMC(it.peso, it.estatura)
                tvIMCHome.text = String.format(Locale.getDefault(), "%.1f", imc)
                tvIMCEstadoHome.text = estado
                
                tvEnfermedadHome.text = if (it.enfermedadCronica && it.detalleEnfermedad.isNotEmpty()) {
                    it.detalleEnfermedad
                } else {
                    "Ninguna"
                }
            }
        }

        viewModel.conteoNotificaciones.observe(this) { conteo ->
            tvNotifCounter.text = conteo.toString()
            tvNotifCounter.visibility = if (conteo > 0) View.VISIBLE else View.GONE
        }

        viewModel.tieneNotificacionesNuevas.observe(this) { nuevas ->
            notifBadgeIndicator.visibility = if (nuevas) View.VISIBLE else View.GONE
            if (nuevas) {
                lottieBell.playAnimation()
                lottieBell.repeatCount = LottieDrawable.INFINITE
            } else {
                lottieBell.pauseAnimation()
                lottieBell.progress = 0f
            }
        }

        val updateCarousel = { carouselAdapter.updateData(viewModel.proximaCita.value, viewModel.proximaToma.value, viewModel.progresoMedicamentos.value) }
        viewModel.proximaToma.observe(this) { updateCarousel() }
        viewModel.proximaCita.observe(this) { updateCarousel() }
        viewModel.progresoMedicamentos.observe(this) { updateCarousel() }
    }

    private fun mostrarBottomSheetNotificaciones() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_notifications, null)
        
        val rvNotifs = view.findViewById<RecyclerView>(R.id.rvNotificationsHistory)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyNotifs)
        
        val adapter = NotificacionesAdapter(emptyList())
        rvNotifs.layoutManager = LinearLayoutManager(this)
        rvNotifs.adapter = adapter
        
        viewModel.notificacionesHistorial.observe(this) { lista ->
            adapter.updateLista(lista)
            tvEmpty.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        }
        
        auth.currentUser?.uid?.let { viewModel.marcarNotificacionesLeidas(it) }
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun redireccionarSegunTipo(uriString: String) {
        val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
        val type = when (scanType) {
            "RECETA" -> "RECETA"
            "HORA" -> "CITA"
            else -> "DOCUMENTO"
        }
        
        val dest = when (type) {
            "RECETA" -> RecetaDetalleActivity::class.java
            "CITA" -> CitaDetalleActivity::class.java
            else -> DocumentoDetalleActivity::class.java
        }
        
        val intent = Intent(this, dest).apply {
            putExtra("IS_NEW", true)
            when (type) {
                "RECETA" -> { putExtra("RECIPE_IMAGE_URI", uriString); putExtra("IS_NEW_RECIPE", true) }
                "CITA" -> { putExtra("APPOINTMENT_IMAGE_URI", uriString); putExtra("IS_NEW_APPOINTMENT", true) }
                else -> { putExtra("DOC_IMAGE_URI", uriString); putExtra("IS_NEW_DOC", true) }
            }
        }
        startActivity(intent, options.toBundle())
    }

    private fun procesarDocumentoSeleccionado(uri: Uri) {
        imageUri = uri
        redireccionarSegunTipo(uri.toString())
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
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()
        
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

        view.findViewById<View>(R.id.btnCancelChoice).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera()
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        val intent = Intent(this, ScannerCameraActivity::class.java)
        takePictureLauncher.launch(intent)
    }

    private fun configurarBannerCarousel() {
        vpBannerCarousel = findViewById(R.id.vpBannerCarousel)
        layoutDots = findViewById(R.id.layoutDots)
        
        val images = listOf(
            R.drawable.caminar,
            R.drawable.saludmental,
            R.drawable.moverte,
            R.drawable.tucuerpo
        )
        
        vpBannerCarousel.adapter = BannerAdapter(images)
        vpBannerCarousel.clipToPadding = false
        vpBannerCarousel.clipChildren = false
        vpBannerCarousel.offscreenPageLimit = 3
        vpBannerCarousel.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(20))
        compositePageTransformer.addTransformer { page, position ->
            val r = 1 - abs(position)
            page.scaleX = 0.90f + r * 0.10f
            page.scaleY = 0.90f + r * 0.10f
            page.alpha = 0.5f + r * 0.5f
        }
        vpBannerCarousel.setPageTransformer(compositePageTransformer)
        
        setupDots(images.size)
        
        vpBannerCarousel.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(position)
                startAutoScroll()
            }
        })
    }

    private fun setupDots(size: Int) {
        layoutDots.removeAllViews()
        val dots = arrayOfNulls<ImageView>(size)
        for (i in 0 until size) {
            dots[i] = ImageView(this)
            dots[i]?.setImageResource(R.drawable.dot_inactive)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 0, 8, 0)
            layoutDots.addView(dots[i], params)
        }
        if (size > 0) updateDots(0)
    }

    private fun updateDots(position: Int) {
        for (i in 0 until layoutDots.childCount) {
            val dot = layoutDots.getChildAt(i) as ImageView
            dot.setImageResource(if (i == position) R.drawable.dot_active else R.drawable.dot_inactive)
        }
    }

    private fun configurarCarousel() {
        vpHomeCarousel = findViewById(R.id.vpHomeCarousel)
        carouselAdapter = HomeCarouselAdapter(
            onCitaClick = { irAActivityConFiltro("DocumentosActivity", "CITA") },
            onMedClick = { irAActivity("CalendarioTratamientoActivity") }
        )
        vpHomeCarousel.adapter = carouselAdapter
    }

    private fun startAutoScroll() {
        carouselHandler.removeCallbacks(autoScrollRunnable)
        carouselHandler.postDelayed(autoScrollRunnable, 5000)
    }

    override fun onResume() {
        super.onResume()
        startAutoScroll()
        auth.currentUser?.uid?.let { viewModel.cargarDatos(it) } 
    }

    override fun onPause() {
        super.onPause()
        carouselHandler.removeCallbacks(autoScrollRunnable)
    }

    private fun base64ToBitmap(base64Str: String): android.graphics.Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) { null }
    }

    private fun configurarDrawer(navigationView: NavigationView) {
        navigationView.setCheckedItem(R.id.nav_home)
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_recetas -> irAActivityConFiltro("DocumentosActivity", "RECETA")
                R.id.nav_horas -> irAActivityConFiltro("DocumentosActivity", "CITA")
                R.id.nav_mapas -> irAActivity("MapaActivity")
                R.id.nav_perfil -> irAActivity("perfilActivity")
                R.id.nav_logout -> cerrarSesion()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun configurarNavegacion() {
        findViewById<View>(R.id.botonInicioNav).apply {
            setOnClickListener { /* Página actual */ }
        }
        findViewById<View>(R.id.botonDocsNav).setOnClickListener { irAActivity("DocumentosActivity") }
        findViewById<View>(R.id.botonMapasNav).setOnClickListener { irAActivity("MapaActivity") }
        findViewById<View>(R.id.botonPerfilNav).setOnClickListener { irAActivity("perfilActivity") }
    }

    private fun cerrarSesion() {
        auth.signOut()
        startActivity(Intent(this, AuthActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
        finish()
    }

    private fun irAActivity(className: String) {
        try {
            val intent = Intent(this, Class.forName("com.example.medly_proyecto.ui.$className"))
            startActivity(intent, ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle())
        } catch (e: Exception) { Toast.makeText(this, "Pantalla en desarrollo", Toast.LENGTH_SHORT).show() }
    }

    private fun irAActivityConFiltro(className: String, filtro: String) {
        try {
            val intent = Intent(this, Class.forName("com.example.medly_proyecto.ui.$className"))
            intent.putExtra("INITIAL_FILTER", filtro)
            startActivity(intent, ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle())
        } catch (e: Exception) { Toast.makeText(this, "Pantalla en desarrollo", Toast.LENGTH_SHORT).show() }
    }

    private fun configurarListenersCategorias() {
        btnRecetas.setOnClickListener { irAActivityConFiltro("DocumentosActivity", "RECETA") }
        btnCitas.setOnClickListener { irAActivityConFiltro("DocumentosActivity", "CITA") }
        btnTratamiento.setOnClickListener { irAActivity("CalendarioTratamientoActivity") }
        btnCredencial.setOnClickListener { irAActivity("CredencialQRActivity") }
    }

    private fun configurarRecyclerViewSugerencias() {
        sugerenciaAdapter = SugerenciaAdapter(emptyList()) { sugerencia ->
            searchEditText.setText("") 
            when (sugerencia.idModulo) {
                1 -> irAActivityConFiltro("DocumentosActivity", "RECETA")
                2 -> irAActivityConFiltro("DocumentosActivity", "CITA")
                3 -> irAActivity("EstadisticasActivity")
            }
        }
        rvSuggestions.layoutManager = LinearLayoutManager(this)
        rvSuggestions.adapter = sugerenciaAdapter
    }

    private fun configurarBusqueda() {
        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { viewModel.filtrarContenido(s.toString()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }
}
