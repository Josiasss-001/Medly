package com.example.medly_proyecto.ui

import android.Manifest
import android.app.ActivityOptions
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomsheet.BottomSheetDialog

class perfilActivity : AppCompatActivity() {

    private lateinit var headerBackground: ImageView
    private lateinit var profileImage: ImageView
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var userRegistrationDateText: TextView
    private lateinit var logoutButton: MaterialButton
    private lateinit var btnDatos: View 
    private lateinit var btnFotos: View
    private lateinit var btnCambiarPass: View
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

    private var imageUri: Uri? = null
    private var scanType: String = "" // "RECETA", "HORA" o "DOCUMENTO"

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) generarInforme() else Toast.makeText(this, "Se necesita permiso de notificaciones", Toast.LENGTH_SHORT).show()
    }

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

    private val pickDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
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
        setContentView(R.layout.activity_perfil)
        
        drawerLayout = findViewById(R.id.drawerLayout)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
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

        menuIcon.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        headerBackground = findViewById(R.id.headerBackground)
        profileImage = findViewById(R.id.profileImage)
        userNameText = findViewById(R.id.userNameText)
        userEmailText = findViewById(R.id.userEmailText)
        userRegistrationDateText = findViewById(R.id.userRegistrationDateText)
        logoutButton = findViewById(R.id.logoutButton)
        btnDatos = findViewById(R.id.editProfileButton)
        btnFotos = findViewById(R.id.editPhotosButton)
        btnCambiarPass = findViewById(R.id.changePasswordButton)
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
            val intent = Intent(this, EditProfileActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(intent, options.toBundle())
        }
        btnFotos.setOnClickListener { irAActivity("editPhotoActivity") }
        btnCambiarPass.setOnClickListener {
            val intent = Intent(this, CambiarContrasenaActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(intent, options.toBundle())
        }
        
        btnGenerarQR.setOnClickListener {
            val intent = Intent(this, CredencialQRActivity::class.java).apply {
                putExtra("GENERAR_NUEVA", true)
            }
            val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(intent, options.toBundle())
        }
        
        btnGenerarInforme.setOnClickListener { checkPermissionsAndGenerate() }
        logoutButton.setOnClickListener { viewModel.signOut() }
        deleteAccountText.setOnClickListener { mostrarDialogoEliminar() }

        findViewById<View>(R.id.contenedorEscaner).setOnClickListener { mostrarBottomSheetEscaneo() }

        try {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(onDownloadComplete, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(onDownloadComplete, filter)
            }
        } catch (e: Exception) { Log.e("perfilActivity", "Error al registrar receiver: ${e.message}") }
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

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(onDownloadComplete) } catch (e: Exception) { }
    }

    private fun checkPermissionsAndGenerate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else { generarInforme() }
        } else { generarInforme() }
    }

    private fun generarInforme() {
        val usuario = viewModel.usuario.value
        val datos = viewModel.datosMedicos.value
        if (usuario == null || datos == null) {
            Toast.makeText(this, "Cargando datos médicos...", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Generando informe...", Toast.LENGTH_LONG).show()
        try {
            val pdfFile = ReporteMedicoManager.generarReporteLocal(this, usuario, datos)
            lanzarIntentPdf(pdfFile)
        } catch (e: Exception) { Toast.makeText(this, "Error al generar: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun lanzarIntentPdf(file: File) {
        try {
            val contentUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(contentUri, "application/pdf")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(Intent.createChooser(intent, "Abrir Informe Médico con:"))
        } catch (e: Exception) { Toast.makeText(this, "No se encontró un lector de PDF instalado", Toast.LENGTH_LONG).show() }
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id && id != -1L) { abrirPdfDescargado() }
        }
    }

    private fun abrirPdfDescargado() {
        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsFolder, ultimoNombreArchivo ?: "")
            if (file.exists()) { lanzarIntentPdf(file) } else {
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
                    } else { cursor.close() }
                }
            }
        } catch (e: Exception) { Log.e("perfilActivity", "Error al abrir PDF: ${e.message}") }
    }

    private fun mostrarDialogoEliminar() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar cuenta")
            .setMessage("¿Estás seguro de que deseas eliminar tu cuenta? Esta acción borrará permanentemente todos tus datos médicos, recetas, citas y perfil. No se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                Toast.makeText(this, "Eliminando cuenta...", Toast.LENGTH_SHORT).show()
                viewModel.eliminarCuenta { success, error ->
                    if (success) {
                        Toast.makeText(this, "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show()
                        irAAuth()
                    } else {
                        if (error?.contains("recent", ignoreCase = true) == true) {
                            Toast.makeText(this, "Por seguridad, debes haber iniciado sesión recientemente para eliminar tu cuenta. Por favor, cierra sesión e inicia de nuevo.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Error al eliminar cuenta: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
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
                userRegistrationDateText.text = "Miembro desde: ${sdf.format(Date(it.fechaRegistro))}"
            }
        }
        viewModel.perfilImagenes.observe(this) { imagenes ->
            imagenes?.let {
                if (it.profileImageUrl.isNotEmpty()) {
                    val bitmap = base64ToBitmap(it.profileImageUrl)
                    profileImage.setImageBitmap(bitmap)
                    navHeaderProfileImg.setImageBitmap(bitmap)
                    // Cargar foto en la barra de navegación inferior
                    findViewById<ImageView>(R.id.botonPerfilNav)?.setImageBitmap(bitmap)
                }
                if (it.backgroundImageUrl.isNotEmpty()) {
                    val bitmap = base64ToBitmap(it.backgroundImageUrl)
                    headerBackground.setImageBitmap(bitmap)
                    navHeaderBackground.setImageBitmap(bitmap)
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
        val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
        startActivity(intent, options.toBundle())
        finish()
    }

    private fun configurarDrawer(navigationView: NavigationView) {
        navigationView.setCheckedItem(R.id.nav_perfil)
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> irAActivity("HomeActivity")
                R.id.nav_recetas -> irAActivityConFiltro("DocumentosActivity", "RECETA")
                R.id.nav_horas -> irAActivityConFiltro("DocumentosActivity", "CITA")
                R.id.nav_mapas -> irAActivity("MapaActivity")
                R.id.nav_perfil -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_logout -> viewModel.signOut()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun configurarNavegacion() {
        findViewById<View>(R.id.botonInicioNav).setOnClickListener { irAActivity("HomeActivity") }
        findViewById<View>(R.id.botonDocsNav).setOnClickListener { irAActivity("DocumentosActivity") }
        findViewById<View>(R.id.botonMapasNav).setOnClickListener { irAActivity("MapaActivity") }
        findViewById<View>(R.id.botonPerfilNav).setOnClickListener { /* Ya estamos en Perfil */ }
    }

    private fun irAActivity(className: String) {
        try {
            val intent = Intent(this, Class.forName("com.example.medly_proyecto.ui.$className"))
            val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(intent, options.toBundle())
            if (className == "HomeActivity") finish()
        } catch (e: Exception) { }
    }

    private fun irAActivityConFiltro(className: String, filtro: String) {
        try {
            val intent = Intent(this, Class.forName("com.example.medly_proyecto.ui.$className"))
            intent.putExtra("INITIAL_FILTER", filtro)
            val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(intent, options.toBundle())
        } catch (e: Exception) { }
    }
}
