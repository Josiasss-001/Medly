package com.example.medly_proyecto.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medly_proyecto.BuildConfig
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.CitaMedica
import com.example.medly_proyecto.viewmodel.AppointmentResult
import com.example.medly_proyecto.viewmodel.CitaDetalleViewModel
import com.example.medly_proyecto.viewmodel.CitasViewModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class CitaDetalleActivity : AppCompatActivity() {

    private val viewModel: CitaDetalleViewModel by viewModels()
    private val citasViewModel: CitasViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var appointmentLargeImage: ImageView
    private lateinit var gradientOverlay: View
    private lateinit var appointmentSpecialtyDetail: EditText
    private lateinit var appointmentDoctorDetail: EditText
    private lateinit var centerText: EditText
    private lateinit var addressText: EditText
    private lateinit var dateText: EditText
    private lateinit var hourText: EditText
    private lateinit var reasonText: EditText
    private lateinit var instructionsText: EditText
    private lateinit var loadingCitaOverlay: View
    private lateinit var analyzingCitaOverlay: View
    private lateinit var backButtonCitaDetail: ImageButton
    private lateinit var btnGuardarCita: View
    private lateinit var btnEditarCita: MaterialButton
    private lateinit var btnEliminarCita: MaterialButton
    private lateinit var btnReanalizarCita: MaterialButton

    private var currentImageUri: String = ""
    private var isEditMode = false
    private var currentCita: CitaMedica? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cita_detalle)

        appointmentLargeImage = findViewById(R.id.appointmentLargeImage)
        gradientOverlay = findViewById(R.id.gradientOverlay)
        appointmentSpecialtyDetail = findViewById(R.id.appointmentSpecialtyDetail)
        appointmentDoctorDetail = findViewById(R.id.appointmentDoctorDetail)
        centerText = findViewById(R.id.centerText)
        addressText = findViewById(R.id.addressText)
        dateText = findViewById(R.id.dateText)
        hourText = findViewById(R.id.hourText)
        reasonText = findViewById(R.id.reasonText)
        instructionsText = findViewById(R.id.instructionsText)
        loadingCitaOverlay = findViewById(R.id.loadingCitaOverlay)
        analyzingCitaOverlay = findViewById(R.id.analyzingCitaOverlay)
        backButtonCitaDetail = findViewById(R.id.backButtonCitaDetail)
        btnGuardarCita = findViewById(R.id.btnGuardarCita)
        btnEditarCita = findViewById(R.id.btnEditarCita)
        btnEliminarCita = findViewById(R.id.btnEliminarCita)
        btnReanalizarCita = findViewById(R.id.btnReanalizarCita)

        backButtonCitaDetail.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        btnEditarCita.setOnClickListener { toggleEditMode() }
        btnEliminarCita.setOnClickListener { confirmarEliminacion() }
        btnReanalizarCita.setOnClickListener { reanalizarFotografia() }

        btnGuardarCita.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val manualData = AppointmentResult(
                    especialidad = appointmentSpecialtyDetail.text.toString(),
                    nombreMedico = appointmentDoctorDetail.text.toString(),
                    centroMedico = centerText.text.toString(),
                    direccion = addressText.text.toString(),
                    fechaCita = dateText.text.toString(),
                    horaCita = hourText.text.toString(),
                    motivoConsulta = reasonText.text.toString(),
                    instruccionesPrevias = instructionsText.text.toString()
                )
                viewModel.guardarCita(userId, currentImageUri, manualData, currentCita?.id)
            }
        }

        configurarSelectores()
        observarViewModel()

        val isNewAppointment = intent.getBooleanExtra("IS_NEW_APPOINTMENT", true)
        if (isNewAppointment) {
            btnGuardarCita.isEnabled = false
            btnGuardarCita.alpha = 0.5f
            btnEliminarCita.visibility = View.GONE
            intent.getStringExtra("APPOINTMENT_IMAGE_URI")?.let { uriString ->
                currentImageUri = uriString
                actualizarImagenCabecera(uriString)
                reanalizarFotografia()
            }
        } else {
            btnGuardarCita.visibility = View.VISIBLE
            btnEditarCita.visibility = View.VISIBLE
            btnEliminarCita.visibility = View.VISIBLE
            btnReanalizarCita.visibility = View.VISIBLE
            currentCita = intent.getSerializableExtra("CITA_OBJ") as? CitaMedica
            currentCita?.let { 
                currentImageUri = it.imagenUri
                mostrarDatosCita(it) 
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
            appointmentLargeImage.setImageResource(R.drawable.imagenpdf)
            appointmentLargeImage.scaleType = ImageView.ScaleType.FIT_CENTER
            appointmentLargeImage.setPadding(0, 0, 0, 0)
            appointmentLargeImage.setBackgroundColor(Color.parseColor("#E53935"))
            gradientOverlay.visibility = View.GONE 
        } else {
            appointmentLargeImage.setImageURI(Uri.parse(uriString))
            appointmentLargeImage.scaleType = ImageView.ScaleType.CENTER_CROP
            appointmentLargeImage.setPadding(0, 0, 0, 0)
            appointmentLargeImage.setBackgroundColor(Color.TRANSPARENT)
            gradientOverlay.visibility = View.VISIBLE
        }
    }

    private fun mostrarDatosCita(c: CitaMedica) {
        appointmentSpecialtyDetail.setText(c.especialidad)
        appointmentDoctorDetail.setText(c.nombreMedico)
        centerText.setText(c.centroMedico)
        addressText.setText(c.direccion)
        dateText.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(c.fechaCita)))
        hourText.setText(c.horaCita)
        reasonText.setText(c.motivoConsulta)
        instructionsText.setText(c.instruccionesPrevias)
    }

    private fun reanalizarFotografia() {
        if (currentImageUri.isNotEmpty()) {
            viewModel.setAnalyzing(true)
            lifecycleScope.launch(Dispatchers.IO) {
                val base64 = convertUriToBase64(Uri.parse(currentImageUri))
                withContext(Dispatchers.Main) {
                    if (base64 != null) {
                        viewModel.analizarCita(base64, BuildConfig.OPENAI_API_KEY)
                    } else {
                        viewModel.setAnalyzing(false)
                        Toast.makeText(this@CitaDetalleActivity, "Error al procesar documento", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
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
            val file = File(cacheDir, "temp_pdf_cita_det.pdf")
            contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(file).use { input.copyTo(it) } }
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close(); renderer.close(); fd.close(); bitmap
        } catch (e: Exception) { null }
    }

    private fun mostrarResultadosIA(data: AppointmentResult) {
        appointmentSpecialtyDetail.setText(data.especialidad)
        appointmentDoctorDetail.setText(data.nombreMedico)
        centerText.setText(data.centroMedico); addressText.setText(data.direccion)
        dateText.setText(data.fechaCita); hourText.setText(data.horaCita)
        reasonText.setText(data.motivoConsulta); instructionsText.setText(data.instruccionesPrevias)
        btnGuardarCita.isEnabled = true; btnGuardarCita.alpha = 1.0f
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

    private fun configurarSelectores() {
        dateText.setOnClickListener { if (isEditMode) {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d -> dateText.setText(String.format("%02d/%02d/%d", d, m + 1, y)) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        } }
        hourText.setOnClickListener { if (isEditMode) {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, h, min ->
                val amPm = if (h >= 12) "PM" else "AM"; val displayHour = if (h > 12) h - 12 else if (h == 0) 12 else h
                hourText.setText(String.format("%02d:%02d %s", displayHour, min, amPm))
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
        } }
    }

    private fun confirmarEliminacion() {
        AlertDialog.Builder(this).setTitle("Eliminar Cita").setMessage("¿Deseas eliminar esta cita?")
            .setPositiveButton("Eliminar") { _, _ -> currentCita?.let { cita -> auth.currentUser?.uid?.let { uid -> citasViewModel.eliminarCita(cita.id, uid) } } }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        appointmentSpecialtyDetail.isEnabled = isEditMode; appointmentDoctorDetail.isEnabled = isEditMode
        centerText.isEnabled = isEditMode; addressText.isEnabled = isEditMode
        dateText.isEnabled = isEditMode; hourText.isEnabled = isEditMode
        reasonText.isEnabled = isEditMode; instructionsText.isEnabled = isEditMode
        btnEditarCita.text = if (isEditMode) "BLOQUEAR EDICIÓN" else "EDITAR INFORMACIÓN"
    }

    private fun observarViewModel() {
        viewModel.appointmentData.observe(this) { data -> 
            data?.let { 
                if (it.esCitaValida) {
                    mostrarResultadosIA(it)
                } else {
                    mostrarErrorValidacion()
                }
            } 
        }
        viewModel.isLoading.observe(this) { loading -> loadingCitaOverlay.visibility = if (loading) View.VISIBLE else View.GONE }
        viewModel.isAnalyzing.observe(this) { analyzing -> analyzingCitaOverlay.visibility = if (analyzing) View.VISIBLE else View.GONE }
        viewModel.saveStatus.observe(this) { pair -> if (pair.first) finish() }
        citasViewModel.deleteStatus.observe(this) { pair -> if (pair.first) finish() }
    }
}
