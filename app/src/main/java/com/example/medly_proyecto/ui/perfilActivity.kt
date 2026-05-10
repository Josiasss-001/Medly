package com.example.medly_proyecto.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.medly_proyecto.R
import com.example.medly_proyecto.viewmodel.PerfilViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class perfilActivity : AppCompatActivity() {

    private lateinit var headerBackground: ImageView
    private lateinit var profileImage: ImageView
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var userRegistrationDateText: TextView
    private lateinit var logoutButton: MaterialButton
    private lateinit var btnDatos: View 
    private lateinit var btnFotos: View
    private lateinit var deleteAccountText: TextView
    private lateinit var drawerLayout: DrawerLayout

    private val viewModel: PerfilViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil)
        
        drawerLayout = findViewById(R.id.drawerLayout)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)

        navigationView.itemIconTintList = null

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        menuIcon.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        headerBackground = findViewById(R.id.headerBackground)
        profileImage = findViewById(R.id.profileImage)
        userNameText = findViewById(R.id.userNameText)
        userEmailText = findViewById(R.id.userEmailText)
        userRegistrationDateText = findViewById(R.id.userRegistrationDateText)
        logoutButton = findViewById(R.id.logoutButton)
        btnDatos = findViewById(R.id.editProfileButton)
        btnFotos = findViewById(R.id.editPhotosButton)
        deleteAccountText = findViewById(R.id.deleteAccountText)

        observarViewModel()
        
        auth.currentUser?.uid?.let { viewModel.loadProfile(it) }
        userEmailText.text = auth.currentUser?.email ?: ""

        configurarNavegacion()
        configurarDrawer(navigationView)

        btnDatos.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))

        }

        btnFotos.setOnClickListener {
            irAActivity("editPhotoActivity")
        }

        logoutButton.setOnClickListener {
            viewModel.signOut()
        }

        deleteAccountText.setOnClickListener {
            mostrarDialogoEliminar()
        }
    }

    private fun mostrarDialogoEliminar() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar cuenta")
            .setMessage("¿Estás seguro de que deseas eliminar tu cuenta? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarCuenta()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observarViewModel() {
        viewModel.usuario.observe(this) { usuario ->
            usuario?.let {
                userNameText.text = it.nombreCompleto
                
                // Formatear y mostrar la fecha de registro
                val sdf = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "CL"))
                val dateStr = sdf.format(Date(it.fechaRegistro))
                userRegistrationDateText.text = "Miembro desde: $dateStr"
            }
        }

        viewModel.loggedOut.observe(this) { success ->
            if (success) {
                irAAuth()
            }
        }

        viewModel.deleteStatus.observe(this) { (success, error) ->
            if (success) {
                Toast.makeText(this, "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show()
                irAAuth()
            } else {
                Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun irAAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun configurarDrawer(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { irAActivity("HomeActivity") }
                R.id.nav_recetas -> { irAActivity("RecetasActivity") }
                R.id.nav_horas -> { irAActivity("HorasActivity") }
                R.id.nav_mapas -> { irAActivity("mapaActivity") }
                R.id.nav_perfil -> drawerLayout.closeDrawer(GravityCompat.START)
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun configurarNavegacion() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.itemIconTintList = null
        bottomNavigationView.selectedItemId = R.id.nav_perfil
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { irAActivity("HomeActivity"); true }
                R.id.nav_recetas -> { irAActivity("RecetasActivity"); true }
                R.id.nav_horas -> { irAActivity("HorasActivity"); true }
                R.id.nav_perfil -> true
                R.id.nav_mapas -> { irAActivity("mapaActivity"); true }
                else -> false
            }
        }
    }

    private fun irAActivity(className: String) {
        try {
            val intent = Intent(this, Class.forName("com.example.medly_proyecto.ui.$className"))
            startActivity(intent)
            if (className != "EditProfileActivity" && className != "editPhotoActivity") {
                overridePendingTransition(0, 0)
                finish()
            }
        } catch (e: ClassNotFoundException) {
            Toast.makeText(this, "Pantalla en desarrollo", Toast.LENGTH_SHORT).show()
        }
    }
}
