package com.example.medly_proyecto.ui

import android.Manifest
import android.app.ActivityOptions
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
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
import com.example.medly_proyecto.model.CitaMedica
import com.example.medly_proyecto.model.Documento
import com.example.medly_proyecto.model.Receta
import com.example.medly_proyecto.ui.adapter.CitasAdapter
import com.example.medly_proyecto.ui.adapter.DocumentoAdapter
import com.example.medly_proyecto.ui.adapter.RecetasAdapter
import com.example.medly_proyecto.ui.adapter.SugerenciaAdapter
import com.example.medly_proyecto.viewmodel.DocumentosViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentosActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvRecentDocs: RecyclerView
    private lateinit var documentoAdapter: DocumentoAdapter
    private lateinit var etSearchDocs: EditText
    private lateinit var chipGroupDocs: ChipGroup
    private lateinit var tvCountRecetas: TextView
    private lateinit var tvCountCitas: TextView
    private lateinit var tvCountExamenes: TextView
    private lateinit var tvCountCertificados: TextView
    private lateinit var tvCountOrdenes: TextView
    private lateinit var tvCountOtros: TextView
    private lateinit var layoutDashboard: View
    private lateinit var layoutRecetasContent: View
    private lateinit var layoutCitasContent: View
    private lateinit var rvRecetasUnificadas: RecyclerView
    private lateinit var rvCitasUnificadas: RecyclerView
    private lateinit var recetasAdapter: RecetasAdapter
    private lateinit var citasAdapter: CitasAdapter
    private lateinit var itemRecienteUnificado: View
    private lateinit var tvRecienAgregadoUnificado: TextView
    private lateinit var tvOtrasCitasUnificado: TextView
    private lateinit var fabAddDocumento: FloatingActionButton
    private lateinit var loadingOverlay: View
    
    private lateinit var navHeaderName: TextView
    private lateinit var navHeaderEmail: TextView
    private lateinit var navHeaderProfileImg: com.google.android.material.imageview.ShapeableImageView
    private lateinit var navHeaderBackground: ImageView

    private lateinit var suggestionsCard: View
    private lateinit var rvSuggestions: RecyclerView
    private lateinit var sugerenciaAdapter: SugerenciaAdapter

    private val viewModel: DocumentosViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private var imageUri: Uri? = null
    private var scanTypeHint: String = ""

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera() else Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriStr = result.data?.getStringExtra("SCANNED_IMAGE_URI")
            if (uriStr != null) {
                redireccionarSegunTipo(uriStr)
            }
        }
    }

    private val pickDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { 
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: Exception) {}
            redireccionarSegunTipo(it.toString()) 
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_documentos)
        inicializarVistas()
        configurarRecyclerViews()
        configurarRecyclerViewSugerencias()
        configurarFiltros()
        configurarBusqueda()
        configurarNavegacion()
        observarViewModel()

        val initialFilter = intent.getStringExtra("INITIAL_FILTER")
        when (initialFilter) {
            "RECETA" -> chipGroupDocs.check(R.id.chipRecetas)
            "CITA" -> chipGroupDocs.check(R.id.chipCitas)
        }
    }

    override fun onResume() {
        super.onResume()
        auth.currentUser?.uid?.let { viewModel.cargarDocumentos(it) }
    }

    private fun inicializarVistas() {
        drawerLayout = findViewById(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0)
        
        navHeaderName = headerView.findViewById(R.id.nav_header_name)
        navHeaderEmail = headerView.findViewById(R.id.nav_header_email)
        navHeaderProfileImg = headerView.findViewById(R.id.nav_header_profile_img)
        navHeaderBackground = headerView.findViewById(R.id.nav_header_background)

        findViewById<View>(R.id.menuIcon)?.setOnClickListener { 
            drawerLayout.openDrawer(GravityCompat.START) 
        }

        etSearchDocs = findViewById(R.id.etSearchDocs)
        chipGroupDocs = findViewById(R.id.chipGroupDocs)
        layoutDashboard = findViewById(R.id.layoutDashboard)
        rvRecentDocs = findViewById(R.id.rvRecentDocs)
        tvCountRecetas = findViewById(R.id.tvCountRecetas)
        tvCountCitas = findViewById(R.id.tvCountCitas)
        tvCountExamenes = findViewById(R.id.tvCountExamenes)
        tvCountCertificados = findViewById(R.id.tvCountCertificados)
        tvCountOrdenes = findViewById(R.id.tvCountOrdenes)
        tvCountOtros = findViewById(R.id.tvCountOtros)
        layoutRecetasContent = findViewById(R.id.layoutRecetasContent)
        rvRecetasUnificadas = findViewById(R.id.rvRecetasUnificadas)
        layoutCitasContent = findViewById(R.id.layoutCitasContent)
        rvCitasUnificadas = findViewById(R.id.rvCitasUnificadas)
        itemRecienteUnificado = findViewById(R.id.itemRecienteUnificado)
        tvRecienAgregadoUnificado = findViewById(R.id.tvRecienAgregadoUnificado)
        tvOtrasCitasUnificado = findViewById(R.id.tvOtrasCitasUnificado)
        fabAddDocumento = findViewById(R.id.fabAddDocumento)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        
        suggestionsCard = findViewById(R.id.suggestionsCard)
        rvSuggestions = findViewById(R.id.rvSuggestions)
        
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        configurarDrawer(navigationView)
        navHeaderEmail.text = auth.currentUser?.email ?: ""
    }

    private fun configurarDrawer(navigationView: NavigationView) {
        navigationView.setCheckedItem(R.id.nav_recetas)
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> irAActivity("HomeActivity")
                R.id.nav_recetas -> {
                    chipGroupDocs.check(R.id.chipRecetas)
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_horas -> {
                    chipGroupDocs.check(R.id.chipCitas)
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_mapas -> irAActivity("MapaActivity")
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

    private fun configurarRecyclerViews() {
        documentoAdapter = DocumentoAdapter(emptyList()) { doc ->
            abrirDetalleDocumento(doc)
        }
        rvRecentDocs.layoutManager = LinearLayoutManager(this)
        rvRecentDocs.adapter = documentoAdapter
        configurarSwipeParaEliminar(rvRecentDocs)

        recetasAdapter = RecetasAdapter(emptyList()) { r -> 
            val intent = Intent(this, RecetaDetalleActivity::class.java).apply { 
                putExtra("RECETA_OBJ", r)
                putExtra("IS_NEW_RECIPE", false) 
            }
            startActivity(intent, ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle())
        }
        rvRecetasUnificadas.layoutManager = LinearLayoutManager(this)
        rvRecetasUnificadas.adapter = recetasAdapter
        configurarSwipeParaEliminar(rvRecetasUnificadas)

        citasAdapter = CitasAdapter(emptyList()) { c -> 
            val intent = Intent(this, CitaDetalleActivity::class.java).apply { 
                putExtra("CITA_OBJ", c)
                putExtra("IS_NEW_APPOINTMENT", false) 
            }
            startActivity(intent, ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle())
        }
        rvCitasUnificadas.layoutManager = LinearLayoutManager(this)
        rvCitasUnificadas.adapter = citasAdapter
        configurarSwipeParaEliminar(rvCitasUnificadas)
    }

    private fun configurarSwipeParaEliminar(recyclerView: RecyclerView) {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private val background = ColorDrawable(Color.parseColor("#FF3B30"))
            private val deleteIcon = ContextCompat.getDrawable(this@DocumentosActivity, android.R.drawable.ic_menu_delete)

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val userId = auth.currentUser?.uid ?: return

                val (docId, tipo, titulo) = when (val adapter = recyclerView.adapter) {
                    is DocumentoAdapter -> {
                        val doc = adapter.getDocumentoAt(position)
                        Triple(doc.id, doc.tipo, doc.titulo)
                    }
                    is RecetasAdapter -> {
                        val receta = adapter.getRecetaAt(position)
                        Triple(receta.id, "RECETA", receta.nombreMedicamento)
                    }
                    is CitasAdapter -> {
                        val cita = adapter.getCitaAt(position)
                        Triple(cita.id, "CITA", cita.especialidad)
                    }
                    else -> Triple("", "", "")
                }

                if (docId.isEmpty()) return

                AlertDialog.Builder(this@DocumentosActivity)
                    .setTitle("Eliminar Documento")
                    .setMessage("¿Estás seguro de que deseas eliminar '$titulo'?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        when (tipo.uppercase()) {
                            "RECETA" -> viewModel.eliminarReceta(docId, userId)
                            "CITA" -> viewModel.eliminarCita(docId, userId)
                            else -> viewModel.eliminarDocumentoGeneral(docId, userId)
                        }
                    }
                    .setNegativeButton("Cancelar") { _, _ ->
                        recyclerView.adapter?.notifyItemChanged(position)
                    }
                    .setOnCancelListener {
                        recyclerView.adapter?.notifyItemChanged(position)
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
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)
    }

    private fun abrirDetalleDocumento(doc: Documento) {
        val dest = when (doc.tipo.uppercase()) {
            "RECETA" -> RecetaDetalleActivity::class.java
            "CITA" -> CitaDetalleActivity::class.java
            else -> DocumentoDetalleActivity::class.java
        }
        val intent = Intent(this, dest).apply {
            putExtra("IS_NEW", false)
            when (doc.tipo.uppercase()) {
                "RECETA" -> {
                    putExtra("IS_NEW_RECIPE", false)
                    val recetaObj = viewModel.recetas.value?.find { it.id == doc.id }
                    if (recetaObj != null) {
                        putExtra("RECETA_OBJ", recetaObj)
                    } else {
                        putExtra("RECETA_OBJ", Receta(
                            id = doc.id,
                            nombreMedicamento = doc.titulo,
                            instrucciones = doc.resumen,
                            imagenUri = doc.imagenUri,
                            fechaCaptura = doc.fecha
                        ))
                    }
                }
                "CITA" -> {
                    putExtra("IS_NEW_APPOINTMENT", false)
                    val citaObj = viewModel.citas.value?.find { it.id == doc.id }
                    if (citaObj != null) {
                        putExtra("CITA_OBJ", citaObj)
                    } else {
                        putExtra("CITA_OBJ", CitaMedica(
                            id = doc.id,
                            especialidad = doc.titulo.replace("Cita: ", ""),
                            centroMedico = doc.institucion,
                            motivoConsulta = doc.resumen,
                            imagenUri = doc.imagenUri,
                            fechaCaptura = doc.fecha
                        ))
                    }
                }
                else -> {
                    putExtra("IS_NEW_DOC", false)
                    putExtra("DOCUMENTO_OBJ", doc)
                    putExtra("DOC_IMAGE_URI", doc.imagenUri)
                }
            }
        }
        startActivity(intent, ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle())
    }

    private fun configurarRecyclerViewSugerencias() {
        sugerenciaAdapter = SugerenciaAdapter(emptyList()) { sugerencia ->
            etSearchDocs.setText("") 
            suggestionsCard.visibility = View.GONE
            
            when (sugerencia.idModulo) {
                101 -> chipGroupDocs.check(R.id.chipRecetas)
                102 -> chipGroupDocs.check(R.id.chipCitas)
                103 -> chipGroupDocs.check(R.id.chipExamenes)
                104 -> chipGroupDocs.check(R.id.chipCertificados)
                105 -> chipGroupDocs.check(R.id.chipOrdenes)
                200 -> {
                    val doc = viewModel.documentos.value?.find { it.titulo == sugerencia.texto }
                    doc?.let { abrirDetalleDocumento(it) }
                }
            }
        }
        rvSuggestions.layoutManager = LinearLayoutManager(this)
        rvSuggestions.adapter = sugerenciaAdapter
    }

    private fun configurarFiltros() {
        chipGroupDocs.setOnCheckedChangeListener { _, id ->
            val tipo = when (id) {
                R.id.chipRecetas -> "RECETA"
                R.id.chipCitas -> "CITA"
                R.id.chipExamenes -> "EXAMEN"
                R.id.chipCertificados -> "CERTIFICADO"
                R.id.chipOrdenes -> "ORDEN"
                else -> "Todos"
            }
            actualizarInterfazPorTipo(tipo)
        }
        findViewById<View>(R.id.btnCardRecetas).setOnClickListener { chipGroupDocs.check(R.id.chipRecetas) }
        findViewById<View>(R.id.btnCardCitas).setOnClickListener { chipGroupDocs.check(R.id.chipCitas) }
    }

    private fun actualizarInterfazPorTipo(tipo: String) {
        layoutDashboard.visibility = View.GONE
        layoutRecetasContent.visibility = View.GONE
        layoutCitasContent.visibility = View.GONE
        fabAddDocumento.visibility = View.GONE

        when (tipo) {
            "Todos" -> layoutDashboard.visibility = View.VISIBLE
            "RECETA" -> { 
                layoutRecetasContent.visibility = View.VISIBLE
                fabAddDocumento.visibility = View.VISIBLE
                fabAddDocumento.setOnClickListener { mostrarDialogoSeleccion("Receta Médica") } 
            }
            "CITA" -> { 
                layoutCitasContent.visibility = View.VISIBLE
                fabAddDocumento.visibility = View.VISIBLE
                fabAddDocumento.setOnClickListener { mostrarDialogoSeleccion("Cita Médica") } 
            }
            else -> layoutDashboard.visibility = View.VISIBLE
        }
        viewModel.filtrarPorTipo(tipo)
    }

    private fun observarViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                loadingOverlay.bringToFront()
            }
        }

        viewModel.listaRecientesUnificada.observe(this) { 
            documentoAdapter.actualizarLista(it)
            findViewById<View>(R.id.tvNoResults)?.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }
        
        viewModel.sugerencias.observe(this) { lista ->
            sugerenciaAdapter.actualizarLista(lista)
            suggestionsCard.visibility = if (lista.isNotEmpty()) View.VISIBLE else View.GONE
        }
        
        viewModel.documentos.observe(this) { list ->
            list?.let {
                tvCountExamenes.text = it.count { d -> d.tipo.equals("EXAMEN", true) }.toString()
                tvCountCertificados.text = it.count { d -> d.tipo.equals("CERTIFICADO", true) }.toString()
                tvCountOrdenes.text = it.count { d -> d.tipo.equals("ORDEN", true) }.toString()
                tvCountOtros.text = it.count { d -> d.tipo.equals("OTROS", true) }.toString()
            }
        }

        viewModel.recetasFiltradas.observe(this) { it?.let { 
            recetasAdapter.updateRecetas(it)
            tvCountRecetas.text = it.size.toString()
            findViewById<View>(R.id.tvEmptyRecetas).visibility = if (it.isEmpty()) View.VISIBLE else View.GONE 
        } }

        viewModel.citasFiltradas.observe(this) { it?.let {
            tvCountCitas.text = it.size.toString()
            if (it.isEmpty()) {
                tvRecienAgregadoUnificado.visibility = View.GONE
                itemRecienteUnificado.visibility = View.GONE
                tvOtrasCitasUnificado.visibility = View.GONE
                rvCitasUnificadas.visibility = View.GONE
            } else {
                val ord = it.sortedByDescending { c -> c.fechaCaptura }
                configurarItemReciente(ord[0])
                tvRecienAgregadoUnificado.visibility = View.VISIBLE
                itemRecienteUnificado.visibility = View.VISIBLE
                if (ord.size > 1) {
                    tvOtrasCitasUnificado.visibility = View.VISIBLE
                    rvCitasUnificadas.visibility = View.VISIBLE
                    citasAdapter.updateCitas(ord.drop(1))
                } else {
                    tvOtrasCitasUnificado.visibility = View.GONE
                    rvCitasUnificadas.visibility = View.GONE
                }
            }
        } }
        
        viewModel.usuario.observe(this) { usuario ->
            usuario?.let { navHeaderName.text = it.nombreCompleto }
        }

        viewModel.perfilImagenes.observe(this) { imagenes ->
            imagenes?.let {
                if (it.profileImageUrl.isNotEmpty()) {
                    val bitmap = base64ToBitmap(it.profileImageUrl)
                    navHeaderProfileImg.setImageBitmap(bitmap)
                    findViewById<ImageView>(R.id.botonPerfilNav)?.setImageBitmap(bitmap)
                }
                if (it.backgroundImageUrl.isNotEmpty()) navHeaderBackground.setImageBitmap(base64ToBitmap(it.backgroundImageUrl))
            }
        }

        viewModel.deleteStatus.observe(this) { (success, message) ->
            if (success) {
                Toast.makeText(this, "Documento eliminado con éxito", Toast.LENGTH_SHORT).show()
            } else if (message != null) {
                Toast.makeText(this, "Error al eliminar: $message", Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.error.observe(this) { it?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() } }
    }
    
    private fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) { null }
    }

    private fun redireccionarSegunTipo(uriString: String) {
        val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
        val type = when (scanTypeHint) {
            "Receta Médica", "RECETA" -> "RECETA"
            "Cita Médica", "HORA", "CITA" -> "CITA"
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
        try {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {}
        redireccionarSegunTipo(uri.toString())
    }

    private fun configurarItemReciente(cita: CitaMedica) {
        val tvEspecialidad: TextView = itemRecienteUnificado.findViewById(R.id.itemEspecialidad)
        val tvMedico: TextView = itemRecienteUnificado.findViewById(R.id.itemMedico)
        val tvFechaHora: TextView = itemRecienteUnificado.findViewById(R.id.itemFechaHora)
        val ivIcon: ImageView = itemRecienteUnificado.findViewById(R.id.itemCitaIcon)
        
        tvEspecialidad.text = cita.especialidad; tvMedico.text = if (cita.nombreMedico.isNotEmpty()) "Dr. ${cita.nombreMedico}" else "Médico por asignar"
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("es", "ES")); tvFechaHora.text = "${sdf.format(Date(cita.fechaCita))} - ${cita.horaCita}"
        
        if (cita.imagenUri.isNotEmpty()) {
            if (cita.imagenUri.lowercase().contains(".pdf")) {
                ivIcon.setImageResource(R.drawable.imagenpdf)
                ivIcon.scaleType = ImageView.ScaleType.FIT_CENTER
                ivIcon.setPadding(35, 35, 35, 35)
            } else {
                try {
                    ivIcon.setImageURI(Uri.parse(cita.imagenUri))
                    ivIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                    ivIcon.setPadding(0, 0, 0, 0)
                } catch (e: Exception) {
                    ivIcon.setImageResource(R.mipmap.watch)
                    ivIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                    ivIcon.setPadding(0, 0, 0, 0)
                }
            }
        } else {
            ivIcon.setImageResource(R.mipmap.watch)
            ivIcon.scaleType = ImageView.ScaleType.CENTER_CROP
            ivIcon.setPadding(0, 0, 0, 0)
        }

        itemRecienteUnificado.setOnClickListener { 
            val intent = Intent(this, CitaDetalleActivity::class.java).apply { 
                putExtra("CITA_OBJ", cita)
                putExtra("IS_NEW_APPOINTMENT", false) 
            }
            startActivity(intent, ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle())
        }
    }

    private fun configurarBusqueda() {
        etSearchDocs.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { 
                viewModel.buscarDocumentos(s.toString()) 
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun configurarNavegacion() {
        findViewById<View>(R.id.botonInicioNav).setOnClickListener { irAActivity("HomeActivity") }
        findViewById<View>(R.id.botonMapasNav).setOnClickListener { irAActivity("MapaActivity") }
        findViewById<View>(R.id.botonPerfilNav).setOnClickListener { irAActivity("perfilActivity") }
        findViewById<View>(R.id.contenedorEscaner).setOnClickListener { mostrarBottomSheetEscaneo() }
    }

    private fun irAActivity(className: String) {
        try {
            val intent = Intent(this, Class.forName("com.example.medly_proyecto.ui.$className"))
            startActivity(intent, ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle())
            if (className == "HomeActivity") finish()
        } catch (e: Exception) { 
            Toast.makeText(this, "Pantalla en desarrollo", Toast.LENGTH_SHORT).show() 
        }
    }

    private fun mostrarBottomSheetEscaneo() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_scan, null)
        
        view.findViewById<View>(R.id.btnScanReceta).setOnClickListener { 
            dialog.dismiss()
            mostrarDialogoSeleccion("Receta Médica") 
        }
        view.findViewById<View>(R.id.btnScanHora).setOnClickListener { 
            dialog.dismiss()
            mostrarDialogoSeleccion("Cita Médica") 
        }
        view.findViewById<View>(R.id.btnScanDocumento).setOnClickListener { 
            dialog.dismiss() 
            mostrarDialogoSeleccion("Documento")
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun mostrarDialogoSeleccion(tipo: String) {
        scanTypeHint = tipo
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = layoutInflater.inflate(R.layout.layout_dialog_source_choice, null)
        dialog.setContentView(view)
        
        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

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
}
