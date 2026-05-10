package com.example.medly_proyecto.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
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
    
    private lateinit var navHeaderName: TextView
    private lateinit var navHeaderProfileImg: ShapeableImageView
    private lateinit var navHeaderBackground: ImageView

    private val viewModel: HomeViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        
        drawerLayout = findViewById(R.id.drawerLayout)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)

        val headerView = navigationView.getHeaderView(0)
        navHeaderName = headerView.findViewById(R.id.nav_header_name)
        navHeaderProfileImg = headerView.findViewById(R.id.nav_header_profile_img)
        navHeaderBackground = headerView.findViewById(R.id.nav_header_background)

        // IMPORTANTE: Permitir que los iconos se vean con sus colores originales de los mipmaps
        navigationView.itemIconTintList = null

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

        observarViewModel()
        
        auth.currentUser?.uid?.let { uid ->
            viewModel.cargarDatos(uid)
        }

        viewModel.iniciarFrases(BuildConfig.OPENAI_API_KEY)

        configurarNavegacion()
        configurarDrawer(navigationView)
    }

    private fun observarViewModel() {
        viewModel.usuario.observe(this) { usuario ->
            usuario?.let {
                userNameTextView.text = it.nombreCompleto
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
                R.id.nav_recetas -> irAActivity("RecetasActivity")
                R.id.nav_horas -> irAActivity("HorasActivity")
                R.id.nav_mapas -> irAActivity("mapaActivity")
                R.id.nav_perfil -> irAActivity("perfilActivity")
                R.id.nav_logout -> cerrarSesion()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun configurarNavegacion() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.itemIconTintList = null // Permitir colores originales
        bottomNavigationView.selectedItemId = R.id.nav_home
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_recetas -> { irAActivity("RecetasActivity"); true }
                R.id.nav_horas -> { irAActivity("HorasActivity"); true }
                R.id.nav_perfil -> { irAActivity("perfilActivity"); true }
                R.id.nav_mapas -> { irAActivity("mapaActivity"); true }
                else -> false
            }
        }
    }

    private fun cerrarSesion() {
        auth.signOut()
        val intent = Intent(this, AuthActivity::class.java)
        // Limpiar el stack de actividades para que no pueda volver atrás
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun irAActivity(className: String) {
        try {
            val intent = Intent(this, Class.forName("com.example.medly_proyecto.ui.$className"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Pantalla en desarrollo", Toast.LENGTH_SHORT).show()
        }
    }
}
