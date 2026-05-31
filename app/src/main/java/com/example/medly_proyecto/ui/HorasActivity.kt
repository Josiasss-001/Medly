package com.example.medly_proyecto.ui

import android.Manifest
import android.app.ActivityOptions
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
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
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.CitaMedica
import com.example.medly_proyecto.ui.adapter.CitasAdapter
import com.example.medly_proyecto.viewmodel.CitasViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class HorasActivity : AppCompatActivity() {

    private val viewModel: CitasViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout
    private val auth = FirebaseAuth.getInstance()
    private var imageUri: Uri? = null

    private lateinit var rvCitas: RecyclerView
    private lateinit var adapter: CitasAdapter
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var tvEmptyState: TextView

    // Vistas para el item reciente
    private lateinit var tvRecienAgregado: TextView
    private lateinit var itemReciente: View
    private lateinit var tvOtrasCitas: TextView

    private lateinit var navHeaderName: TextView
    private lateinit var navHeaderEmail: TextView
    private lateinit var navHeaderProfileImg: com.google.android.material.imageview.ShapeableImageView
    private lateinit var navHeaderBackground: ImageView

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openCamera() else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val intent = Intent(this, CitaDetalleActivity::class.java).apply {
                putExtra("APPOINTMENT_IMAGE_URI", imageUri.toString())
                putExtra("IS_NEW_APPOINTMENT", true)
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_horas)

        drawerLayout = findViewById(R.id.drawerLayout)
        val includeTopBar = findViewById<View>(R.id.includeTopBar)
        val menuIcon = includeTopBar.findViewById<ImageView>(R.id.menuIcon)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val addCitaFab = findViewById<FloatingActionButton>(R.id.addCitaFab)

        val headerView = navigationView.getHeaderView(0)
        navHeaderName = headerView.findViewById(R.id.nav_header_name)
        navHeaderEmail = headerView.findViewById(R.id.nav_header_email)
        navHeaderProfileImg = headerView.findViewById(R.id.nav_header_profile_img)
        navHeaderBackground = headerView.findViewById(R.id.nav_header_background)

        rvCitas = findViewById(R.id.rvCitas)
        loadingProgressBar = findViewById(R.id.loadingCitas)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        // Inicializar nuevas vistas
        tvRecienAgregado = findViewById(R.id.tvRecienAgregado)
        itemReciente = findViewById(R.id.itemReciente)
        tvOtrasCitas = findViewById(R.id.tvOtrasCitas)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        menuIcon.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        addCitaFab.setOnClickListener { checkCameraPermission() }

        setupRecyclerView()
        configurarNavegacion()
        configurarDrawer(navigationView)
        observarViewModel()

        navHeaderEmail.text = auth.currentUser?.email ?: ""
    }

    override fun onStart() {
        super.onStart()
        auth.currentUser?.uid?.let { viewModel.cargarCitas(it) }
    }

    private fun setupRecyclerView() {
        adapter = CitasAdapter(emptyList()) { cita ->
            abrirDetalleCita(cita)
        }
        rvCitas.layoutManager = LinearLayoutManager(this)
        rvCitas.adapter = adapter
    }

    private fun abrirDetalleCita(cita: CitaMedica) {
        val intent = Intent(this, CitaDetalleActivity::class.java).apply {
            putExtra("CITA_OBJ", cita)
            putExtra("IS_NEW_APPOINTMENT", false)
        }
        startActivity(intent)
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val values = ContentValues().apply { put(MediaStore.Images.Media.TITLE, "Cita Médica") }
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { putExtra(MediaStore.EXTRA_OUTPUT, imageUri) }
        takePictureLauncher.launch(intent)
    }

    private fun observarViewModel() {
        viewModel.citas.observe(this) { lista ->
            if (lista.isNullOrEmpty()) {
                tvEmptyState.visibility = View.VISIBLE
                rvCitas.visibility = View.GONE
                tvRecienAgregado.visibility = View.GONE
                itemReciente.visibility = View.GONE
                tvOtrasCitas.visibility = View.GONE
            } else {
                tvEmptyState.visibility = View.GONE
                
                // Ordenar por fecha de captura o ID para tener la más reciente primero si no viene ordenada
                val listaOrdenada = lista.sortedByDescending { it.fechaCaptura }
                
                // Mostrar la primera cita en el item grande
                val reciente = listaOrdenada[0]
                configurarItemReciente(reciente)
                
                tvRecienAgregado.visibility = View.VISIBLE
                itemReciente.visibility = View.VISIBLE

                // Si hay más de una, mostrar el resto en el RecyclerView
                if (listaOrdenada.size > 1) {
                    tvOtrasCitas.visibility = View.VISIBLE
                    rvCitas.visibility = View.VISIBLE
                    adapter.updateCitas(listaOrdenada.drop(1))
                } else {
                    tvOtrasCitas.visibility = View.GONE
                    rvCitas.visibility = View.GONE
                }
            }
        }

        viewModel.usuario.observe(this) { usuario ->
            usuario?.let { navHeaderName.text = it.nombreCompleto }
        }

        viewModel.perfilImagenes.observe(this) { imagenes ->
            imagenes?.let {
                if (it.profileImageUrl.isNotEmpty()) navHeaderProfileImg.setImageBitmap(base64ToBitmap(it.profileImageUrl))
                if (it.backgroundImageUrl.isNotEmpty()) navHeaderBackground.setImageBitmap(base64ToBitmap(it.backgroundImageUrl))
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun configurarItemReciente(cita: CitaMedica) {
        val ivFoto: ImageView = itemReciente.findViewById(R.id.itemCitaIcon)
        val tvEspecialidad: TextView = itemReciente.findViewById(R.id.itemEspecialidad)
        val tvMedico: TextView = itemReciente.findViewById(R.id.itemMedico)
        val tvFechaHora: TextView = itemReciente.findViewById(R.id.itemFechaHora)
        val btnDetalles: View = itemReciente.findViewById(R.id.btnVerDetallesCita)

        tvEspecialidad.text = cita.especialidad
        tvMedico.text = if (cita.nombreMedico.isNotEmpty()) "Dr. ${cita.nombreMedico}" else "Médico por asignar"
        
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("es", "ES"))
        val fechaStr = sdf.format(Date(cita.fechaCita))
        tvFechaHora.text = "$fechaStr - ${cita.horaCita}"

        if (cita.imagenUri.isNotEmpty()) {
            try {
                ivFoto.setImageURI(Uri.parse(cita.imagenUri))
            } catch (e: Exception) {
                ivFoto.setImageResource(R.mipmap.watch)
            }
        }

        val listener = View.OnClickListener { abrirDetalleCita(cita) }
        btnDetalles.setOnClickListener(listener)
        itemReciente.setOnClickListener(listener)
    }

    private fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) { null }
    }

    private fun configurarDrawer(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> irAActivity("HomeActivity")
                R.id.nav_recetas -> irAActivity("RecetasMedicasActivity")
                R.id.nav_horas -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_perfil -> irAActivity("perfilActivity")
                R.id.nav_logout -> { auth.signOut(); startActivity(Intent(this, AuthActivity::class.java)); finish() }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun configurarNavegacion() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.selectedItemId = R.id.nav_horas
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { irAActivity("HomeActivity"); true }
                R.id.nav_recetas -> { irAActivity("RecetasMedicasActivity"); true }
                R.id.nav_horas -> true
                R.id.nav_perfil -> { irAActivity("perfilActivity"); true }
                else -> false
            }
        }
    }

    private fun irAActivity(className: String) {
        try {
            val intent = Intent(this, Class.forName("com.example.medly_proyecto.ui.$className"))
            val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(intent, options.toBundle())
            finish()
        } catch (e: Exception) { Toast.makeText(this, "Pantalla en desarrollo", Toast.LENGTH_SHORT).show() }
    }
}
