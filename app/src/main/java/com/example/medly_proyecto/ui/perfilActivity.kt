package com.example.medly_proyecto.ui

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.medly_proyecto.R
import com.example.medly_proyecto.util.ReporteMedicoManager
import com.example.medly_proyecto.viewmodel.PerfilViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.io.File
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
    private lateinit var btnAjustes: View
    private lateinit var btnSeguridad: View
    private lateinit var btnGenerarQR: View
    private lateinit var btnGenerarInforme: View
    private lateinit var deleteAccountText: TextView
    private lateinit var drawerLayout: DrawerLayout

    private lateinit var navHeaderName: TextView
    private lateinit var navHeaderEmail: TextView
    private lateinit var navHeaderProfileImg: com.google.android.material.imageview.ShapeableImageView
    private lateinit var navHeaderBackground: ImageView

    private val viewModel: PerfilViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    
    private var downloadId: Long = -1
    private var ultimoNombreArchivo: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            generarInforme()
        } else {
            Toast.makeText(this, "Se necesita permiso de notificaciones para la descarga", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil)
        
        drawerLayout = findViewById(R.id.drawerLayout)
        val includeTopBar = findViewById<View>(R.id.includeTopBar)
        val menuIcon = includeTopBar.findViewById<ImageView>(R.id.menuIcon)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)

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

        headerBackground = findViewById(R.id.headerBackground)
        profileImage = findViewById(R.id.profileImage)
        userNameText = findViewById(R.id.userNameText)
        userEmailText = findViewById(R.id.userEmailText)
        userRegistrationDateText = findViewById(R.id.userRegistrationDateText)
        logoutButton = findViewById(R.id.logoutButton)
        btnDatos = findViewById(R.id.editProfileButton)
        btnFotos = findViewById(R.id.editPhotosButton)
        btnAjustes = findViewById(R.id.settingsButton)
        btnSeguridad = findViewById(R.id.securityButton)
        btnGenerarQR = findViewById(R.id.btnGenerarQR)
        btnGenerarInforme = findViewById(R.id.btnGenerarInforme)
        deleteAccountText = findViewById(R.id.deleteAccountText)

        observarViewModel()
        
        auth.currentUser?.uid?.let { viewModel.loadProfile(it) }
        val email = auth.currentUser?.email ?: ""
        userEmailText.text = email
        navHeaderEmail.text = email

        configurarNavegacion()
        configurarDrawer(navigationView)

        btnDatos.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        btnFotos.setOnClickListener {
            irAActivity("editPhotoActivity")
        }

        btnGenerarQR.setOnClickListener {
            startActivity(Intent(this, CredencialQRActivity::class.java))
        }

        btnGenerarInforme.setOnClickListener {
            checkPermissionsAndGenerate()
        }

        logoutButton.setOnClickListener {
            viewModel.signOut()
        }

        deleteAccountText.setOnClickListener {
            mostrarDialogoEliminar()
        }

        // Registro seguro del receptor
        try {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(onDownloadComplete, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(onDownloadComplete, filter)
            }
        } catch (e: Exception) {
            Log.e("perfilActivity", "Error al registrar receiver: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(onDownloadComplete)
        } catch (e: Exception) { }
    }

    private fun checkPermissionsAndGenerate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                generarInforme()
            }
        } else {
            generarInforme()
        }
    }

    private fun generarInforme() {
        val usuario = viewModel.usuario.value
        val datos = viewModel.datosMedicos.value

        if (usuario == null || datos == null) {
            Toast.makeText(this, "Cargando datos médicos...", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Generando informe...", Toast.LENGTH_LONG).show()

        ReporteMedicoManager.generarYSubirReporte(this, usuario, datos, object : ReporteMedicoManager.ReporteCallback {
            override fun onSuccess(pdfUrl: String, qrBitmap: Bitmap?) {
                runOnUiThread {
                    iniciarDescarga(pdfUrl)
                }
            }

            override fun onError(e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@perfilActivity, "Error al generar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun iniciarDescarga(url: String) {
        try {
            val secureUrl = if (url.startsWith("http://")) url.replace("http://", "https://") else url
            // Usamos un nombre de archivo rastreable
            ultimoNombreArchivo = "Informe_Medly_${System.currentTimeMillis()}.pdf"
            
            val request = DownloadManager.Request(Uri.parse(secureUrl))
                .setTitle("Informe Médico Medly")
                .setDescription("Documento Clínico Oficial")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, ultimoNombreArchivo)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setMimeType("application/pdf")

            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = manager.enqueue(request)
            
            Toast.makeText(this, "Descarga iniciada...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al descargar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id && id != -1L) {
                abrirPdfDescargado()
            }
        }
    }

    private fun abrirPdfDescargado() {
        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsFolder, ultimoNombreArchivo ?: "")

            if (file.exists()) {
                lanzarIntentPdf(file)
            } else {
                // Si el nombre directo falla, intentamos por el cursor del DownloadManager como respaldo
                val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusIndex != -1 && DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(statusIndex)) {
                        val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        val uriString = if (localUriIndex != -1) cursor.getString(localUriIndex) else null
                        cursor.close()
                        
                        if (uriString != null) {
                            val uri = Uri.parse(uriString)
                            val backupFile = File(uri.path ?: "")
                            if (backupFile.exists()) lanzarIntentPdf(backupFile)
                            else Toast.makeText(this, "Archivo guardado en descargas", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        cursor.close()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("perfilActivity", "Error al abrir PDF: ${e.message}")
        }
    }

    private fun lanzarIntentPdf(file: File) {
        try {
            val contentUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(contentUri, "application/pdf")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            
            val chooser = Intent.createChooser(intent, "Abrir Informe Médico con:")
            startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(this, "No se encontró un lector de PDF instalado", Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarDialogoEliminar() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar cuenta")
            .setMessage("¿Estás seguro?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarCuenta { _, _ -> }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observarViewModel() {
        viewModel.usuario.observe(this) { usuario ->
            usuario?.let {
                userNameText.text = it.nombreCompleto
                navHeaderName.text = it.nombreCompleto
                
                val sdf = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "CL"))
                val dateStr = sdf.format(Date(it.fechaRegistro))
                userRegistrationDateText.text = "Miembro desde: $dateStr"
            }
        }

        viewModel.perfilImagenes.observe(this) { imagenes ->
            imagenes?.let {
                if (it.profileImageUrl.isNotEmpty()) {
                    profileImage.setImageBitmap(base64ToBitmap(it.profileImageUrl))
                }
                if (it.backgroundImageUrl.isNotEmpty()) {
                    headerBackground.setImageBitmap(base64ToBitmap(it.backgroundImageUrl))
                }
            }
        }

        viewModel.loggedOut.observe(this) { success -> if (success) irAAuth() }
    }

    private fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) { null }
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
                R.id.nav_home -> irAActivity("HomeActivity")
                R.id.nav_recetas -> irAActivity("RecetasMedicasActivity")
                R.id.nav_horas -> irAActivity("HorasActivity")
                R.id.nav_perfil -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_logout -> viewModel.signOut()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun configurarNavegacion() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.selectedItemId = R.id.nav_perfil
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { irAActivity("HomeActivity"); true }
                R.id.nav_recetas -> { irAActivity("RecetasMedicasActivity"); true }
                R.id.nav_horas -> { irAActivity("HorasActivity"); true }
                R.id.nav_perfil -> true
                else -> false
            }
        }
    }

    private fun irAActivity(className: String) {
        try {
            val intent = Intent(this, Class.forName("com.example.medly_proyecto.ui.$className"))
            startActivity(intent)
            finish()
        } catch (e: Exception) { }
    }
}
