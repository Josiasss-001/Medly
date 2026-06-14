package com.example.medly_proyecto.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.appcompat.app.AppCompatActivity
import com.example.medly_proyecto.BuildConfig
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.Documento
import com.example.medly_proyecto.viewmodel.DocumentResult
import com.example.medly_proyecto.viewmodel.DocumentoDetalleViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class DocumentoDetalleActivity : AppCompatActivity() {

    private val viewModel: DocumentoDetalleViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var docLargeImage: ImageView
    private lateinit var docTitleDetail: EditText
    private lateinit var tvDocType: TextView
    private lateinit var docDateText: EditText
    private lateinit var docInstitutionText: EditText
    private lateinit var docSummaryDetail: EditText
    private lateinit var loadingDocOverlay: View
    private lateinit var backButtonDoc: ImageButton
    private lateinit var btnConfirmarDoc: View
    private lateinit var btnEditarDoc: View

    private var currentImageUri: String = ""
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_documento_detalle)

        docLargeImage = findViewById(R.id.docLargeImage)
        docTitleDetail = findViewById(R.id.docTitleDetail)
        tvDocType = findViewById(R.id.tvDocType)
        docDateText = findViewById(R.id.docDateText)
        docInstitutionText = findViewById(R.id.docInstitutionText)
        docSummaryDetail = findViewById(R.id.docSummaryDetail)
        loadingDocOverlay = findViewById(R.id.loadingDocOverlay)
        backButtonDoc = findViewById(R.id.backButtonDoc)
        btnConfirmarDoc = findViewById(R.id.btnConfirmarIA) ?: findViewById(R.id.btnConfirmarDoc)
        btnEditarDoc = findViewById(R.id.btnEditarDoc)

        backButtonDoc.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        btnEditarDoc.setOnClickListener { toggleEditMode() }

        btnConfirmarDoc.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val manualData = DocumentResult(
                    titulo = docTitleDetail.text.toString(),
                    tipo = tvDocType.text.toString(),
                    fecha = docDateText.text.toString(),
                    institucion = docInstitutionText.text.toString(),
                    resumen = docSummaryDetail.text.toString()
                )
                viewModel.guardarDocumento(userId, currentImageUri, manualData)
            }
        }

        observarViewModel()

        val isNewDoc = intent.getBooleanExtra("IS_NEW_DOC", true)
        if (isNewDoc) {
            intent.getStringExtra("DOC_IMAGE_URI")?.let { uriString ->
                currentImageUri = uriString
                val uri = Uri.parse(uriString)
                if (uriString.lowercase().endsWith(".pdf") || contentResolver.getType(uri) == "application/pdf") {
                    docLargeImage.setImageResource(R.drawable.imagenpdf)
                } else {
                    docLargeImage.setImageURI(uri)
                }
                docLargeImage.scaleType = ImageView.ScaleType.CENTER_CROP
                docLargeImage.setPadding(0, 0, 0, 0)
                dispararAnalisisCero(uri)
            }
        } else {
            // Documento ya existente (viene de la lista de Recientes)
            val doc = intent.getSerializableExtra("DOCUMENTO_OBJ") as? Documento
            doc?.let {
                currentImageUri = it.imagenUri
                docTitleDetail.setText(it.titulo)
                tvDocType.text = it.tipo
                docDateText.setText(it.fechaTexto)
                docInstitutionText.setText(it.institucion)
                docSummaryDetail.setText(it.resumen)
                
                if (it.imagenUri.isNotEmpty()) {
                    try {
                        val uri = Uri.parse(it.imagenUri)
                        if (it.imagenUri.lowercase().contains(".pdf")) {
                            docLargeImage.setImageResource(R.drawable.imagenpdf)
                        } else {
                            docLargeImage.setImageURI(uri)
                        }
                        docLargeImage.scaleType = ImageView.ScaleType.CENTER_CROP
                        docLargeImage.setPadding(0, 0, 0, 0)
                    } catch (e: Exception) {
                        docLargeImage.setImageResource(R.drawable.imagenpdf)
                        docLargeImage.scaleType = ImageView.ScaleType.CENTER_CROP
                        docLargeImage.setPadding(0, 0, 0, 0)
                    }
                }
                btnConfirmarDoc.visibility = View.GONE
            }
        }
    }

    private fun dispararAnalisisCero(uri: Uri) {
        val base64 = convertUriToBase64(uri)
        if (base64 != null) {
            viewModel.analizarDocumento(base64, BuildConfig.OPENAI_API_KEY)
        } else {
            Toast.makeText(this, "Error al preparar el documento", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observarViewModel() {
        viewModel.docData.observe(this) { data ->
            data?.let {
                docTitleDetail.setText(it.titulo)
                tvDocType.text = it.tipo
                docDateText.setText(it.fecha)
                docInstitutionText.setText(it.institucion)
                docSummaryDetail.setText(it.resumen)
            }
        }
        viewModel.isLoading.observe(this) { loadingDocOverlay.visibility = if (it) View.VISIBLE else View.GONE }
        viewModel.saveStatus.observe(this) { if (it) { Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show(); finish() } }
        viewModel.error.observe(this) { it?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() } }
    }

    private fun convertUriToBase64(uri: Uri): String? {
        return try {
            val type = contentResolver.getType(uri)
            val bitmap = if (type == "application/pdf" || uri.toString().endsWith(".pdf")) renderPdfToBitmap(uri)
            else BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
            
            if (bitmap == null) return null
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) { null }
    }

    private fun renderPdfToBitmap(uri: Uri): Bitmap? {
        return try {
            val file = File(cacheDir, "temp_pdf_doc_det.pdf")
            contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(file).use { input.copyTo(it) } }
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close(); renderer.close(); fd.close()
            bitmap
        } catch (e: Exception) { null }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        docTitleDetail.isEnabled = isEditMode; docDateText.isEnabled = isEditMode
        docInstitutionText.isEnabled = isEditMode; docSummaryDetail.isEnabled = isEditMode
        if (isEditMode) { docTitleDetail.requestFocus(); (btnEditarDoc as? TextView)?.text = "BLOQUEAR EDICIÓN" }
        else { (btnEditarDoc as? TextView)?.text = "EDITAR INFORMACIÓN" }
    }
}
