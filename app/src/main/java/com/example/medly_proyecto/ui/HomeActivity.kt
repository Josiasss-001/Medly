package com.example.medly_proyecto.ui

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.medly_proyecto.R
import com.example.medly_proyecto.viewmodel.HomeViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.medly_proyecto.BuildConfig

class HomeActivity : AppCompatActivity() {

    private lateinit var userNameTextView: TextView
    private lateinit var motivationTextView: TextView
    private lateinit var profileCircle: ShapeableImageView
    private lateinit var welcomeBackground: ImageView
    private lateinit var drawerLayout: DrawerLayout
    
    // Vistas del Drawer Header
    private lateinit var navHeaderName: TextView
    private lateinit var navHeaderEmail: TextView
    private lateinit var navHeaderProfileImg: ShapeableImageView
    private lateinit var navHeaderBackground: ImageView

    private lateinit var searchEditText: EditText
    private lateinit var btnRecetasHome: View
    private lateinit var btnCitasHome: View
    private lateinit var tvQuick: TextView

    private val viewModel: HomeViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        
        drawerLayout = findViewById(R.id.drawerLayout)
        val includeTopBar = findViewById<View>(R.id.includeTopBar)
        val menuIcon = includeTopBar.findViewById<ImageView>(R.id.menuIcon)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)

        // Inicializar vistas del header
        val headerView = navigationView.getHeaderView(0)
        navHeaderName = headerView.findViewById(R.id.nav_header_name)
        navHeaderEmail = headerView.findViewById(R.id.nav_header_email)
        navHeaderProfileImg = headerView.findViewById(R.id.nav_header_profile_img)
        navHeaderBackground = headerView.findViewById(R.id.nav_header_background)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        menuIcon.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        userNameTextView = findViewById(R.id.userNameTextView)
        motivationTextView = findViewById(R.id.motivationTextView)
        profileCircle = findViewById(R.id.profileCircle)
        welcomeBackground = findViewById(R.id.welcomeBackground)
        
        searchEditText = findViewById(R.id.searchEditText)
        btnRecetasHome = findViewById(R.id.btnRecetasHome)
        btnCitasHome = findViewById(R.id.btnCitasHome)
        tvQuick = findViewById(R.id.tvQuick)

        observarViewModel()
        
        auth.currentUser?.uid?.let { uid ->
            viewModel.cargarDatos(uid)
        }

        navHeaderEmail.text = auth.currentUser?.email ?: ""
        viewModel.iniciarFrases(BuildConfig.OPENAI_API_KEY)

        configurarNavegacion()
        configurarDrawer(navigationView)
        configurarBusqueda()

        btnRecetasHome.setOnClickListener {
            irAActivity("RecetasMedicasActivity")
        }

        btnCitasHome.setOnClickListener {
            irAActivity("HorasActivity")
        }
    }

    private fun configurarBusqueda() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase().trim()
                filtrarModulos(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filtrarModulos(query: String) {
        if (query.isEmpty()) {
            btnRecetasHome.visibility = View.VISIBLE
            btnCitasHome.visibility = View.VISIBLE
            tvQuick.visibility = View.VISIBLE
            return
        }

        val coincideRecetas = "recetas médicas escanear organizar".lowercase().contains(query)
        val coincideCitas = "control de horas gestionar próximas citas médicas".lowercase().contains(query)

        btnRecetasHome.visibility = if (coincideRecetas) View.VISIBLE else View.GONE
        btnCitasHome.visibility = if (coincideCitas) View.VISIBLE else View.GONE
        tvQuick.visibility = if (coincideRecetas || coincideCitas) View.VISIBLE else View.GONE
    }

    private fun observarViewModel() {
        viewModel.usuario.observe(this) { usuario ->
            usuario?.let {
                val nameToShow = it.nombres.ifEmpty { it.nombreCompleto.split(" ").firstOrNull() ?: "Usuario" }
                userNameTextView.text = nameToShow
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
                    val bitmap = base64ToBitmap(it.backgroundImageUrl)
                    welcomeBackground.setImageBitmap(bitmap)
                    navHeaderBackground.setImageBitmap(bitmap)
                }
            }
        }

        viewModel.fraseIA.observe(this) { frase ->
            motivationTextView.text = frase
        }
    }

    private fun base64ToBitmap(base64Str: String): android.graphics.Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun configurarDrawer(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_recetas -> irAActivity("RecetasMedicasActivity")
                R.id.nav_horas -> irAActivity("HorasActivity")
                R.id.nav_perfil -> irAActivity("perfilActivity")
                R.id.nav_logout -> cerrarSesion()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun configurarNavegacion() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.selectedItemId = R.id.nav_home
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_recetas -> { irAActivity("RecetasMedicasActivity"); true }
                R.id.nav_horas -> { irAActivity("HorasActivity"); true }
                R.id.nav_perfil -> { irAActivity("perfilActivity"); true }
                else -> false
            }
        }
    }

    private fun cerrarSesion() {
        auth.signOut()
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun irAActivity(className: String) {
        try {
            val intent = Intent(this, Class.forName("com.example.medly_proyecto.ui.$className"))
            val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(intent, options.toBundle())
            if (className != "HomeActivity") finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Pantalla en desarrollo", Toast.LENGTH_SHORT).show()
        }
    }
}
