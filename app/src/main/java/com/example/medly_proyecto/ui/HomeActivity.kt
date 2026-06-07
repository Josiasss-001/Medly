package com.example.medly_proyecto.ui

import android.Manifest
import android.app.ActivityOptions
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.medly_proyecto.R
import com.example.medly_proyecto.ui.adapter.SugerenciaAdapter
import com.example.medly_proyecto.ui.adapter.HomeCarouselAdapter
import com.example.medly_proyecto.viewmodel.HomeViewModel
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.medly_proyecto.BuildConfig
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale
import kotlin.math.abs

class HomeActivity : AppCompatActivity() {

    // Vistas principales
    private lateinit var userNameTextView: TextView
    private lateinit var motivationTextView: TextView
    private lateinit var profileCircle: ShapeableImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var expandedOnlyContent: View
    
    // Header del Navigation Drawer
    private lateinit var navHeaderName: TextView
    private lateinit var navHeaderEmail: TextView
    private lateinit var navHeaderProfileImg: ShapeableImageView
    private lateinit var navHeaderBackground: ImageView

    // Componentes de búsqueda y contenido
    private lateinit var searchEditText: EditText
    private lateinit var tvQuick: TextView
    private lateinit var quoteCard: View
    private lateinit var statsCard: View
    private lateinit var tvStats: View
    private lateinit var tvNoResults: TextView
    private lateinit var suggestionsCard: View
    private lateinit var rvSuggestions: RecyclerView
    private lateinit var sugerenciaAdapter: SugerenciaAdapter

    // Botones de servicios
    private lateinit var btnCalendario: View
    private lateinit var btnCredencial: View
    private lateinit var btnTratamientos: View
    private lateinit var btnVacio: View

    // Salud e IMC
    private lateinit var tvPesoHome: TextView
    private lateinit var tvIMCHome: TextView
    private lateinit var tvIMCEstadoHome: TextView
    private lateinit var tvPresionHome: TextView
    private lateinit var tvNotifCounter: TextView
    
    // Carousel logic
    private lateinit var vpHomeCarousel: ViewPager2
    private lateinit var carouselAdapter: HomeCarouselAdapter
    private val carouselHandler = Handler(Looper.getMainLooper())
    private val carouselRunnable = Runnable {
        if (::vpHomeCarousel.isInitialized) {
            val currentItem = vpHomeCarousel.currentItem
            val nextItem = if (currentItem == 0) 1 else 0
            vpHomeCarousel.setCurrentItem(nextItem, true)
            startAutoScroll()
        }
    }

    private val viewModel: HomeViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private var imageUri: Uri? = null
    private var scanType: String = ""

    // Launchers de permisos y cámara
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera() else Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val activityClass = if (scanType == "RECETA") RecetaDetalleActivity::class.java else CitaDetalleActivity::class.java
            val key = if (scanType == "RECETA") "RECIPE_IMAGE_URI" else "APPOINTMENT_IMAGE_URI"
            val isNewKey = if (scanType == "RECETA") "IS_NEW_RECIPE" else "IS_NEW_APPOINTMENT"
            
            val intent = Intent(this, activityClass).apply {
                putExtra(key, imageUri.toString())
                putExtra(isNewKey, true)
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        
        // 1. Inicializar Drawer y Navigation
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

        findViewById<View>(R.id.cardHamburger)?.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        // 2. Inicializar Vistas del Header Dinámico
        userNameTextView = findViewById(R.id.userNameTextView)
        motivationTextView = findViewById(R.id.motivationTextView)
        profileCircle = findViewById(R.id.profileCircle)
        expandedOnlyContent = findViewById(R.id.expandedOnlyContent)
        
        setupAppBarScroll()

        // 3. Inicializar Contenido Principal
        searchEditText = findViewById(R.id.searchEditText)
        tvQuick = findViewById(R.id.tvQuick)
        quoteCard = findViewById(R.id.quoteCard)
        statsCard = findViewById(R.id.statsCard)
        tvStats = findViewById(R.id.tvStats)
        tvNoResults = findViewById(R.id.tvNoResults)
        suggestionsCard = findViewById(R.id.suggestionsCard)
        rvSuggestions = findViewById(R.id.rvSuggestions)

        btnCalendario = findViewById(R.id.btnCalendarioHome)
        btnCredencial = findViewById(R.id.btnCredencialHome)
        btnTratamientos = findViewById(R.id.btnTratamientosHome)
        btnVacio = findViewById(R.id.btnVacioHome)

        tvPesoHome = findViewById(R.id.tvPesoHome)
        tvIMCHome = findViewById(R.id.tvIMCHome)
        tvIMCEstadoHome = findViewById(R.id.tvIMCEstadoHome)
        tvPresionHome = findViewById(R.id.tvPresionHome)
        tvNotifCounter = findViewById(R.id.tvNotifCounter)

        // 4. Configurar Componentes
        configurarCarousel()
        configurarRecyclerViewSugerencias()
        observarViewModel()
        
        auth.currentUser?.uid?.let { viewModel.cargarDatos(it) }
        navHeaderEmail.text = auth.currentUser?.email ?: ""
        viewModel.iniciarFrases(BuildConfig.OPENAI_API_KEY)

        configurarNavegacion()
        configurarDrawer(navigationView)
        configurarBusqueda()
        configurarListenersCategorias()

        statsCard.setOnClickListener { irAActivity("EstadisticasActivity") }
        tvStats.setOnClickListener { irAActivity("EstadisticasActivity") }
        findViewById<View>(R.id.btnVerMasSalud).setOnClickListener { irAActivity("perfilActivity") }
        findViewById<View>(R.id.llNotifHeader).setOnClickListener { irAActivity("CalendarioTratamientoActivity") }
        findViewById<View>(R.id.scanContainer).setOnClickListener { mostrarBottomSheetEscaneo() }
    }

    private fun setupAppBarScroll() {
        val appBar = findViewById<AppBarLayout>(R.id.appBarLayout)
        val density = resources.displayMetrics.density
        val tvSubGreeting = findViewById<TextView>(R.id.tvSubGreeting)

        appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            if (totalScrollRange == 0) return@OnOffsetChangedListener
            val percentage = abs(verticalOffset).toFloat() / totalScrollRange.toFloat()

            expandedOnlyContent.alpha = (1f - percentage / 0.6f).coerceIn(0f, 1f)
            tvSubGreeting.alpha = (1f - percentage / 0.5f).coerceIn(0f, 1f)

            // Animar Foto: Tamaño inicial 70dp -> 30dp. Posición inicial (24, 85) -> final (16, 12)
            val initialSize = 70 * density
            val finalSize = 30 * density
            val scale = 1f - (percentage * (1f - (finalSize / initialSize)))
            profileCircle.scaleX = scale
            profileCircle.scaleY = scale
            
            profileCircle.translationX = (16 * density - 24 * density) * percentage
            profileCircle.translationY = (12 * density - 85 * density) * percentage

            // Animar Nombre: Pegado a la foto pequeña
            val nameScale = 1f - (percentage * 0.15f)
            userNameTextView.scaleX = nameScale
            userNameTextView.scaleY = nameScale
            
            val nameTargetX = 58 * density // 16 (foto x) + 30 (foto width) + 12 (gap)
            val nameInitialX = 110 * density // 24 + 70 + 16
            userNameTextView.translationX = (nameTargetX - nameInitialX) * percentage
            
            val photoCenterY = 12 * density + (finalSize / 2f)
            val nameTargetY = photoCenterY - (userNameTextView.height * nameScale / 2f)
            userNameTextView.translationY = (nameTargetY - 85 * density) * percentage
        })
    }

    private fun configurarCarousel() {
        vpHomeCarousel = findViewById(R.id.vpHomeCarousel)
        carouselAdapter = HomeCarouselAdapter(
            onCitaClick = { irAActivity("HorasActivity") },
            onMedClick = { irAActivity("CalendarioTratamientoActivity") }
        )
        vpHomeCarousel.adapter = carouselAdapter
        startAutoScroll()
    }

    private fun startAutoScroll() {
        carouselHandler.removeCallbacks(carouselRunnable)
        carouselHandler.postDelayed(carouselRunnable, 6000)
    }

    override fun onResume() {
        super.onResume()
        startAutoScroll()
        auth.currentUser?.uid?.let { viewModel.cargarDatos(it) } 
    }

    override fun onPause() {
        super.onPause()
        carouselHandler.removeCallbacks(carouselRunnable)
    }

    private fun mostrarBottomSheetEscaneo() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_scan, null)
        view.findViewById<View>(R.id.btnScanReceta).setOnClickListener { scanType = "RECETA"; checkCameraPermission(); dialog.dismiss() }
        view.findViewById<View>(R.id.btnScanHora).setOnClickListener { scanType = "HORA"; checkCameraPermission(); dialog.dismiss() }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera()
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        val values = ContentValues().apply { put(MediaStore.Images.Media.TITLE, if (scanType == "RECETA") "Nueva Receta" else "Cita Médica") }
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { putExtra(MediaStore.EXTRA_OUTPUT, imageUri) }
        takePictureLauncher.launch(intent)
    }

    private fun configurarListenersCategorias() {
        btnCalendario.setOnClickListener { irAActivity("HorasActivity") }
        btnCredencial.setOnClickListener { irAActivity("CredencialQRActivity") }
        btnTratamientos.setOnClickListener { irAActivity("RecetasMedicasActivity") }
        btnVacio.setOnClickListener { Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show() }
    }

    private fun configurarRecyclerViewSugerencias() {
        sugerenciaAdapter = SugerenciaAdapter(emptyList()) { sugerencia ->
            searchEditText.setText("") 
            when (sugerencia.idModulo) {
                1 -> irAActivity("RecetasMedicasActivity")
                2 -> irAActivity("HorasActivity")
                3 -> irAActivity("EstadisticasActivity")
            }
        }
        rvSuggestions.layoutManager = LinearLayoutManager(this)
        rvSuggestions.adapter = sugerenciaAdapter
    }

    private fun configurarBusqueda() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { viewModel.filtrarContenido(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observarViewModel() {
        viewModel.usuario.observe(this) { usuario ->
            usuario?.let {
                val nameToShow = it.nombres.ifEmpty { it.nombreCompleto.split(" ").firstOrNull() ?: "Usuario" }
                userNameTextView.text = "Hi $nameToShow,"
                navHeaderName.text = it.nombreCompleto
            }
        }

        viewModel.perfilImagenes.observe(this) { imagenes ->
            imagenes?.let {
                if (it.profileImageUrl.isNotEmpty()) {
                    val bitmap = base64ToBitmap(it.profileImageUrl)
                    profileCircle.setImageBitmap(bitmap)
                    navHeaderProfileImg.setImageBitmap(bitmap)
                }
                if (it.backgroundImageUrl.isNotEmpty()) {
                    navHeaderBackground.setImageBitmap(base64ToBitmap(it.backgroundImageUrl))
                }
            }
        }

        viewModel.fraseIA.observe(this) { motivationTextView.text = it }

        viewModel.estadoVisibilidad.observe(this) { estado ->
            val isVisible = if (estado.recetasVisible || estado.citasVisible) View.VISIBLE else View.GONE
            findViewById<View>(R.id.gridCategories).visibility = isVisible
            tvQuick.visibility = isVisible
            statsCard.visibility = if (estado.statsVisible) View.VISIBLE else View.GONE
            tvStats.visibility = if (estado.statsVisible) View.VISIBLE else View.GONE
            quoteCard.visibility = if (estado.quoteVisible) View.VISIBLE else View.GONE
            tvNoResults.visibility = if (estado.sinResultadosVisible) View.VISIBLE else View.GONE
            findViewById<View>(R.id.clHealthSummary).visibility = if (estado.statsVisible) View.VISIBLE else View.GONE
            findViewById<View>(R.id.llNotifHeader).visibility = isVisible
            vpHomeCarousel.visibility = isVisible
        }
        
        viewModel.sugerencias.observe(this) { lista ->
            sugerenciaAdapter.actualizarLista(lista)
            suggestionsCard.visibility = if (lista.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.datosMedicos.observe(this) { datos ->
            datos?.let {
                tvPesoHome.text = String.format(Locale.getDefault(), "%.1f kg", it.peso)
                val (imc, estado) = viewModel.calcularIMC(it.peso, it.estatura)
                tvIMCHome.text = String.format(Locale.getDefault(), "%.1f", imc)
                tvIMCEstadoHome.text = estado
            }
        }

        viewModel.conteoNotificaciones.observe(this) { conteo ->
            tvNotifCounter.text = conteo.toString()
            tvNotifCounter.visibility = if (conteo > 0) View.VISIBLE else View.GONE
        }

        val updateCarousel = { carouselAdapter.updateData(viewModel.proximaCita.value, viewModel.proximaToma.value, viewModel.progresoMedicamentos.value) }
        viewModel.proximaToma.observe(this) { updateCarousel() }
        viewModel.proximaCita.observe(this) { updateCarousel() }
        viewModel.progresoMedicamentos.observe(this) { updateCarousel() }
    }

    private fun base64ToBitmap(base64Str: String): android.graphics.Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) { null }
    }

    private fun configurarDrawer(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_recetas -> irAActivity("RecetasMedicasActivity")
                R.id.nav_horas -> irAActivity("HorasActivity")
                R.id.nav_mapas -> irAActivity("MapaActivity")
                R.id.nav_perfil -> irAActivity("perfilActivity")
                R.id.nav_logout -> cerrarSesion()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun configurarNavegacion() {
        findViewById<View>(R.id.btnRecetasNav).setOnClickListener { irAActivity("RecetasMedicasActivity") }
        findViewById<View>(R.id.btnMapasNav).setOnClickListener { irAActivity("MapaActivity") }
        findViewById<View>(R.id.btnPerfilNav).setOnClickListener { irAActivity("perfilActivity") }
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
}
