package com.example.medly_proyecto.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.medly_proyecto.BuildConfig
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.Receta
import com.example.medly_proyecto.viewmodel.RecetaDetalleViewModel
import com.example.medly_proyecto.viewmodel.RecipeResult
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class RecetaDetalleActivity : AppCompatActivity() {

    private val viewModel: RecetaDetalleViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var recipeLargeImage: ImageView
    private lateinit var gradientOverlayReceta: View
    private lateinit var recipeTitleDetail: EditText
    private lateinit var recipeDescriptionDetail: EditText
    private lateinit var doseText: EditText
    private lateinit var freqText: EditText
    private lateinit var durationText: EditText
    private lateinit var qtyText: EditText
    private lateinit var envaseText: EditText
    private lateinit var timesText: EditText
    private lateinit var useMethodText: EditText
    private lateinit var loadingIAOverlay: View
    private lateinit var backButtonDetail: ImageButton
    private lateinit var btnConfirmarIA: View
    private lateinit var btnAgregarManualmente: MaterialButton
    private lateinit var btnEmpezarTratamiento: View
    private lateinit var btnEliminarReceta: MaterialButton

    private var currentImageUri: String = ""
    private var isEditMode = false
    private var currentReceta: Receta? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_receta_detalle)

        recipeLargeImage = findViewById(R.id.recipeLargeImage)
        gradientOverlayReceta = findViewById(R.id.gradientOverlayReceta)
        recipeTitleDetail = findViewById(R.id.recipeTitleDetail)
        recipeDescriptionDetail = findViewById(R.id.recipeDescriptionDetail)
        doseText = findViewById(R.id.doseText)
        freqText = findViewById(R.id.freqText)
        durationText = findViewById(R.id.durationText)
        qtyText = findViewById(R.id.qtyText)
        envaseText = findViewById(R.id.envaseText)
        timesText = findViewById(R.id.timesText)
        useMethodText = findViewById(R.id.useMethodText)
        loadingIAOverlay = findViewById(R.id.loadingIAOverlay)
        backButtonDetail = findViewById(R.id.backButtonDetail)
        btnConfirmarIA = findViewById(R.id.btnConfirmarIA)
        btnAgregarManualmente = findViewById(R.id.btnAgregarManualmente)
        btnEmpezarTratamiento = findViewById(R.id.btnEmpezarTratamiento)
        btnEliminarReceta = findViewById(R.id.btnEliminarReceta)

        backButtonDetail.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        btnAgregarManualmente.setOnClickListener { toggleEditMode() }
        btnEmpezarTratamiento.setOnClickListener {
            val intent = Intent(this, CalendarioTratamientoActivity::class.java)
            currentReceta?.let { r -> 
                intent.putExtra("RECETA_ID", r.id)
                intent.putExtra("MEDICAMENTO_NOMBRE", r.nombreMedicamento) 
            }
            startActivity(intent)
        }
        btnEliminarReceta.setOnClickListener { confirmarEliminacion() }

        btnConfirmarIA.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                btnConfirmarIA.isEnabled = false
                btnConfirmarIA.alpha = 0.5f

                val manualData = RecipeResult(
                    nombreMedicamento = recipeTitleDetail.text.toString(),
                    instrucciones = recipeDescriptionDetail.text.toString(),
                    dosis = doseText.text.toString(),
                    frecuencia = freqText.text.toString(),
                    duracion = durationText.text.toString(),
                    cantidadTotal = qtyText.text.toString(),
                    cantidadEnvase = envaseText.text.toString(),
                    vecesAlDia = timesText.text.toString(),
                    metodoUso = useMethodText.text.toString()
                )
                viewModel.guardarReceta(userId, currentImageUri, manualData, currentReceta?.id)
            }
        }

        observarViewModel()

        val isNewRecipe = intent.getBooleanExtra("IS_NEW_RECIPE", true)
        if (isNewRecipe) {
            btnConfirmarIA.isEnabled = false
            btnConfirmarIA.alpha = 0.5f
            btnEmpezarTratamiento.visibility = View.GONE
            btnEliminarReceta.visibility = View.GONE
            
            intent.getStringExtra("RECIPE_IMAGE_URI")?.let { uriString ->
                currentImageUri = uriString
                actualizarImagenCabecera(uriString)
                analizarConIA(Uri.parse(uriString))
            }
        } else {
            btnConfirmarIA.visibility = View.GONE
            btnAgregarManualmente.visibility = View.VISIBLE
            btnEmpezarTratamiento.visibility = View.VISIBLE
            btnEliminarReceta.visibility = View.VISIBLE
            currentReceta = intent.getSerializableExtra("RECETA_OBJ") as? Receta
            currentReceta?.let { 
                currentImageUri = it.imagenUri
                mostrarDatosReceta(it)
                actualizarImagenCabecera(it.imagenUri)
            }
        }
    }

    private fun isPdfUri(uriString: String): Boolean {
        if (uriString.lowercase().contains(".pdf")) return true
        return try {
            contentResolver.getType(Uri.parse(uriString)) == "application/pdf"
        } catch (e: Exception) {
            false
        }
    }

    private fun actualizarImagenCabecera(uriString: String) {
        if (uriString.isEmpty()) return
        
        if (isPdfUri(uriString)) {
            recipeLargeImage.setImageResource(R.drawable.imagenpdf)
            recipeLargeImage.scaleType = ImageView.ScaleType.FIT_CENTER
            recipeLargeImage.setBackgroundColor(Color.parseColor("#E53935"))
            gradientOverlayReceta.visibility = View.GONE
        } else {
            recipeLargeImage.setImageURI(Uri.parse(uriString))
            recipeLargeImage.scaleType = ImageView.ScaleType.CENTER_CROP
            recipeLargeImage.setBackgroundColor(Color.TRANSPARENT)
            gradientOverlayReceta.visibility = View.VISIBLE
        }
    }

    private fun mostrarDatosReceta(r: Receta) {
        recipeTitleDetail.setText(r.nombreMedicamento)
        recipeDescriptionDetail.setText(r.instrucciones)
        doseText.setText(r.dosis)
        freqText.setText(r.frecuencia)
        durationText.setText(r.duracion)
        qtyText.setText(r.cantidadTotal)
        envaseText.setText(r.cantidadEnvase)
        timesText.setText(r.vecesAlDia)
        useMethodText.setText(r.metodoUso)
    }

    private fun analizarConIA(uri: Uri) {
        val base64 = convertUriToBase64(uri)
        if (base64 != null) viewModel.analizarReceta(base64, BuildConfig.OPENAI_API_KEY)
    }

    private fun convertUriToBase64(uri: Uri): String? {
        return try {
            val type = contentResolver.getType(uri)
            val isPdf = type == "application/pdf" || uri.toString().lowercase().endsWith(".pdf")
            val bitmap = if (isPdf) {
                renderPdfToBitmap(uri)
            } else {
                BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
            }
            if (bitmap == null) return null
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) { null }
    }

    private fun renderPdfToBitmap(uri: Uri): Bitmap? {
        return try {
            val file = File(cacheDir, "temp_pdf_receta.pdf")
            contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(file).use { input.copyTo(it) } }
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            fd.close()
            bitmap
        } catch (e: Exception) { null }
    }

    private fun mostrarResultadosIA(data: RecipeResult) {
        recipeTitleDetail.setText(data.nombreMedicamento)
        recipeDescriptionDetail.setText(data.instrucciones)
        doseText.setText(data.dosis)
        freqText.setText(data.frecuencia)
        durationText.setText(data.duracion)
        qtyText.setText(data.cantidadTotal)
        envaseText.setText(data.cantidadEnvase)
        timesText.setText(data.vecesAlDia)
        useMethodText.setText(data.metodoUso)
        btnConfirmarIA.isEnabled = true
        btnConfirmarIA.alpha = 1.0f
    }

    private fun mostrarErrorValidacion() {
        val dialogView = layoutInflater.inflate(R.layout.layout_dialog_validation_error, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<View>(R.id.btnRescan).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialogView.findViewById<View>(R.id.btnCancelError).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun confirmarEliminacion() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Receta")
            .setMessage("¿Deseas eliminar esta receta?")
            .setPositiveButton("Eliminar") { _, _ -> 
                currentReceta?.let { r -> viewModel.eliminarReceta(r.id) } 
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        recipeTitleDetail.isEnabled = isEditMode
        recipeDescriptionDetail.isEnabled = isEditMode
        doseText.isEnabled = isEditMode
        freqText.isEnabled = isEditMode
        durationText.isEnabled = isEditMode
        qtyText.isEnabled = isEditMode
        envaseText.isEnabled = isEditMode
        timesText.isEnabled = isEditMode
        useMethodText.isEnabled = isEditMode
        
        if (!intent.getBooleanExtra("IS_NEW_RECIPE", true)) {
            btnConfirmarIA.visibility = if (isEditMode) View.VISIBLE else View.GONE
        }
        
        btnAgregarManualmente.text = if (isEditMode) "BLOQUEAR EDICIÓN" else "EDITAR INFORMACIÓN"
    }

    private fun observarViewModel() {
        viewModel.recipeData.observe(this) { data -> 
            data?.let { 
                if (it.esRecetaValida) {
                    mostrarResultadosIA(it)
                } else {
                    mostrarErrorValidacion()
                }
            } 
        }
        viewModel.isLoading.observe(this) { loading -> 
            loadingIAOverlay.visibility = if (loading) View.VISIBLE else View.GONE 
        }
        viewModel.saveStatus.observe(this) { status ->
            val (success, _) = status
            if (success) { 
                btnConfirmarIA.isEnabled = false
                btnConfirmarIA.visibility = View.GONE
                Toast.makeText(this, "Receta guardada con éxito", Toast.LENGTH_SHORT).show()
                finish() 
            } else {
                btnConfirmarIA.isEnabled = true
                btnConfirmarIA.alpha = 1.0f
                Toast.makeText(this, "Error al guardar los cambios", Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.deleteStatus.observe(this) { status ->
            val (success, _) = status
            if (success) { finish() } 
        }
    }
}
