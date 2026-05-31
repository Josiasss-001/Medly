package com.example.medly_proyecto.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
import com.example.medly_proyecto.viewmodel.RecetasViewModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import java.io.InputStream

class RecetaDetalleActivity : AppCompatActivity() {

    private val viewModel: RecetaDetalleViewModel by viewModels()
    private val recetasViewModel: RecetasViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var recipeLargeImage: ImageView
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

        // Inicializar vistas
        recipeLargeImage = findViewById(R.id.recipeLargeImage)
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

        backButtonDetail.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnAgregarManualmente.setOnClickListener {
            toggleEditMode()
        }

        btnEmpezarTratamiento.setOnClickListener {
            val intent = Intent(this, CalendarioTratamientoActivity::class.java)
            currentReceta?.let { receta ->
                intent.putExtra("RECETA_ID", receta.id)
                intent.putExtra("MEDICAMENTO_NOMBRE", receta.nombreMedicamento)
            }
            startActivity(intent)
        }

        btnEliminarReceta.setOnClickListener {
            confirmarEliminacion()
        }

        btnConfirmarIA.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
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
                // Usamos el ID de la receta actual si existe para actualizarla
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
                val uri = Uri.parse(uriString)
                recipeLargeImage.setImageURI(uri)
                
                val base64 = convertUriToBase64(uri)
                if (base64 != null) {
                    viewModel.analizarReceta(base64, BuildConfig.OPENAI_API_KEY)
                }
            }
        } else {
            btnConfirmarIA.visibility = View.VISIBLE
            btnAgregarManualmente.visibility = View.VISIBLE
            btnEmpezarTratamiento.visibility = View.VISIBLE
            btnEliminarReceta.visibility = View.VISIBLE
            
            currentReceta = intent.getSerializableExtra("RECETA_OBJ") as? Receta
            currentReceta?.let { 
                currentImageUri = it.imagenUri
                mostrarDatosReceta(it) 
            }
        }
    }

    private fun confirmarEliminacion() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Receta")
            .setMessage("¿Estás seguro de que deseas eliminar esta receta permanentemente?")
            .setPositiveButton("Eliminar") { _, _ ->
                currentReceta?.let { receta ->
                    auth.currentUser?.uid?.let { uid ->
                        recetasViewModel.eliminarReceta(receta.id, uid)
                    }
                }
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

        if (isEditMode) {
            recipeTitleDetail.requestFocus()
            Toast.makeText(this, "Modo edición activado", Toast.LENGTH_SHORT).show()
            btnAgregarManualmente.text = "BLOQUEAR EDICIÓN"
        } else {
            btnAgregarManualmente.text = "EDITAR INFORMACIÓN"
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
        
        if (r.imagenUri.isNotEmpty()) {
            try {
                recipeLargeImage.setImageURI(Uri.parse(r.imagenUri))
            } catch (e: Exception) {
                recipeLargeImage.setImageResource(R.mipmap.fondo)
            }
        }
    }

    private fun observarViewModel() {
        viewModel.recipeData.observe(this) { data ->
            data?.let {
                recipeTitleDetail.setText(it.nombreMedicamento)
                recipeDescriptionDetail.setText(it.instrucciones)
                doseText.setText(it.dosis)
                freqText.setText(it.frecuencia)
                durationText.setText(it.duracion)
                qtyText.setText(it.cantidadTotal)
                envaseText.setText(it.cantidadEnvase)
                timesText.setText(it.vecesAlDia)
                useMethodText.setText(it.metodoUso)
                
                btnConfirmarIA.isEnabled = true
                btnConfirmarIA.alpha = 1.0f
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            loadingIAOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.saveStatus.observe(this) { (success, result) ->
            if (success) {
                Toast.makeText(this, "Receta guardada con éxito", Toast.LENGTH_SHORT).show()
                // Actualizamos el objeto local con los datos actuales
                currentReceta = Receta(
                    id = result ?: currentReceta?.id ?: "",
                    userId = auth.currentUser?.uid ?: "",
                    nombreMedicamento = recipeTitleDetail.text.toString(),
                    dosis = doseText.text.toString(),
                    frecuencia = freqText.text.toString(),
                    duracion = durationText.text.toString(),
                    cantidadTotal = qtyText.text.toString(),
                    cantidadEnvase = envaseText.text.toString(),
                    vecesAlDia = timesText.text.toString(),
                    instrucciones = recipeDescriptionDetail.text.toString(),
                    metodoUso = useMethodText.text.toString(),
                    imagenUri = currentImageUri,
                    fechaCaptura = currentReceta?.fechaCaptura ?: System.currentTimeMillis()
                )
                btnEmpezarTratamiento.visibility = View.VISIBLE
                btnEmpezarTratamiento.alpha = 1.0f
                btnEliminarReceta.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Error al guardar: $result", Toast.LENGTH_LONG).show()
            }
        }

        recetasViewModel.deleteStatus.observe(this) { (success, message) ->
            if (success) {
                Toast.makeText(this, "Receta eliminada correctamente", Toast.LENGTH_SHORT).show()
                finish() // Cierra la pantalla y vuelve a la lista
            } else {
                Toast.makeText(this, "Error al eliminar: $message", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun convertUriToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }
}
