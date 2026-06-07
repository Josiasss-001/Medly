package com.example.medly_proyecto.ui

import android.Manifest
import android.app.ActivityOptions
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.ui.adapter.RecetasAdapter
import com.example.medly_proyecto.viewmodel.RecetasViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomsheet.BottomSheetDialog

class RecetasMedicasActivity : AppCompatActivity() {

    private val viewModel: RecetasViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout
    private val auth = FirebaseAuth.getInstance()
    private var imageUri: Uri? = null
    private var scanType: String = "" // "RECETA" o "HORA"

    private lateinit var rvRecetas: RecyclerView
    private lateinit var adapter: RecetasAdapter
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var tvEmptyState: TextView

    // Vistas del Header del Navigation Drawer
    private lateinit var navHeaderName: TextView
    private lateinit var navHeaderEmail: TextView
    private lateinit var navHeaderProfileImg: ShapeableImageView
    private lateinit var navHeaderBackground: ImageView

    private val requestPermissionLauncher = registerForActivityResult(
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
            when (scanType) {
                "RECETA" -> {
                    val intent = Intent(this, RecetaDetalleActivity::class.java).apply {
                        putExtra("RECIPE_IMAGE_URI", imageUri.toString())
                        putExtra("IS_NEW_RECIPE", true)
                    }
                    startActivity(intent)
                }
                "HORA" -> {
                    val intent = Intent(this, CitaDetalleActivity::class.java).apply {
                        putExtra("APPOINTMENT_IMAGE_URI", imageUri.toString())
                        putExtra("IS_NEW_APPOINTMENT", true)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recetas_medicas)

        drawerLayout = findViewById(R.id.drawerLayout)
        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val addRecipeFab = findViewById<FloatingActionButton>(R.id.addRecipeFab)
        
        // Inicializar vistas del header
        val headerView = navigationView.getHeaderView(0)
        navHeaderName = headerView.findViewById(R.id.nav_header_name)
        navHeaderEmail = headerView.findViewById(R.id.nav_header_email)
        navHeaderProfileImg = headerView.findViewById(R.id.nav_header_profile_img)
        navHeaderBackground = headerView.findViewById(R.id.nav_header_background)

        rvRecetas = findViewById(R.id.rvRecetas)
        loadingProgressBar = findViewById(R.id.loadingRecetas)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        navigationView.itemIconTintList = null

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        menuIcon.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        addRecipeFab.setOnClickListener {
            scanType = "RECETA"
            checkCameraPermission()
        }

        setupRecyclerView()
        configurarNavegacion()
        configurarDrawer(navigationView)
        observarViewModel()
        
        navHeaderEmail.text = auth.currentUser?.email ?: "usuario@ejemplo.com"

        // Lottie Scan Button
        findViewById<LottieAnimationView>(R.id.lottieScan).setOnClickListener {
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

    override fun onStart() {
        super.onStart()
        auth.currentUser?.uid?.let { uid ->
            viewModel.cargarRecetas(uid)
        }
    }

    private fun setupRecyclerView() {
        adapter = RecetasAdapter(emptyList()) { receta ->
            val intent = Intent(this, RecetaDetalleActivity::class.java).apply {
                putExtra("RECETA_OBJ", receta)
                putExtra("IS_NEW_RECIPE", false)
            }
            startActivity(intent)
        }
        rvRecetas.layoutManager = LinearLayoutManager(this)
        rvRecetas.adapter = adapter

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private val background = ColorDrawable(Color.parseColor("#FF3B30"))
            private val deleteIcon = ContextCompat.getDrawable(this@RecetasMedicasActivity, android.R.drawable.ic_menu_delete)

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val receta = adapter.getRecetaAt(position)
                
                AlertDialog.Builder(this@RecetasMedicasActivity)
                    .setTitle("Eliminar Receta")
                    .setMessage("¿Estás seguro?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        auth.currentUser?.uid?.let { uid ->
                            viewModel.eliminarReceta(receta.id, uid)
                        }
                    }
                    .setNegativeButton("Cancelar") { _, _ ->
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top
                if (dX < 0) {
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    background.draw(c)
                    deleteIcon?.let {
                        val iconTop = itemView.top + (itemHeight - it.intrinsicHeight) / 2
                        val iconMargin = (itemHeight - it.intrinsicHeight) / 2
                        val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                        val iconRight = itemView.right - iconMargin
                        it.setBounds(iconLeft, iconTop, iconRight, iconTop + it.intrinsicHeight)
                        it.setTint(Color.WHITE)
                        it.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rvRecetas)
    }

    private fun observarViewModel() {
        viewModel.recetas.observe(this) { lista ->
            if (lista.isNullOrEmpty()) {
                tvEmptyState.visibility = View.VISIBLE
                rvRecetas.visibility = View.GONE
            } else {
                tvEmptyState.visibility = View.GONE
                rvRecetas.visibility = View.VISIBLE
                adapter.updateRecetas(lista)
            }
        }

        viewModel.usuario.observe(this) { usuario ->
            usuario?.let {
                val nameToShow = if (it.nombres.isNotEmpty()) it.nombres else it.nombreCompleto
                navHeaderName.text = if (nameToShow.isNotEmpty()) nameToShow else "Usuario"
            }
        }

        viewModel.perfilImagenes.observe(this) { imagenes ->
            imagenes?.let {
                if (it.profileImageUrl.isNotEmpty()) {
                    navHeaderProfileImg.setImageBitmap(base64ToBitmap(it.profileImageUrl))
                }
                if (it.backgroundImageUrl.isNotEmpty()) {
                    navHeaderBackground.setImageBitmap(base64ToBitmap(it.backgroundImageUrl))
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.deleteStatus.observe(this) { (success, message) ->
            if (success) {
                Toast.makeText(this, "Receta eliminada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
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

    private fun configurarDrawer(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> irAActivity("HomeActivity")
                R.id.nav_recetas -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_horas -> irAActivity("HorasActivity")
                R.id.nav_mapas -> irAActivity("MapaActivity")
                R.id.nav_perfil -> irAActivity("perfilActivity")
                R.id.nav_logout -> { auth.signOut(); startActivity(Intent(this, AuthActivity::class.java)); finish() }
            }
            true
        }
    }

    private fun configurarNavegacion() {
        findViewById<View>(R.id.btnHomeNav).setOnClickListener {
            irAActivity("HomeActivity")
        }
        
        findViewById<View>(R.id.btnRecetasNav).setOnClickListener {
            // Ya estamos en Recetas
        }
        
        findViewById<View>(R.id.btnMapasNav).setOnClickListener {
            irAActivity("MapaActivity")
        }
        
        findViewById<View>(R.id.btnPerfilNav).setOnClickListener {
            irAActivity("perfilActivity")
        }
    }
}
