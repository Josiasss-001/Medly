package com.example.medly_proyecto.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.medly_proyecto.BuildConfig
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.CitaMedica
import com.example.medly_proyecto.viewmodel.AppointmentResult
import com.example.medly_proyecto.viewmodel.CitaDetalleViewModel
import com.example.medly_proyecto.viewmodel.CitasViewModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class CitaDetalleActivity : AppCompatActivity() {

    private val viewModel: CitaDetalleViewModel by viewModels()
    private val citasViewModel: CitasViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var appointmentLargeImage: ImageView
    private lateinit var appointmentSpecialtyDetail: EditText
    private lateinit var appointmentDoctorDetail: EditText
    private lateinit var centerText: EditText
    private lateinit var dateText: EditText
    private lateinit var hourText: EditText
    private lateinit var reasonText: EditText
    private lateinit var instructionsText: EditText
    private lateinit var loadingCitaOverlay: View
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

        // Inicializar vistas
        appointmentLargeImage = findViewById(R.id.appointmentLargeImage)
        appointmentSpecialtyDetail = findViewById(R.id.appointmentSpecialtyDetail)
        appointmentDoctorDetail = findViewById(R.id.appointmentDoctorDetail)
        centerText = findViewById(R.id.centerText)
        dateText = findViewById(R.id.dateText)
        hourText = findViewById(R.id.hourText)
        reasonText = findViewById(R.id.reasonText)
        instructionsText = findViewById(R.id.instructionsText)
        loadingCitaOverlay = findViewById(R.id.loadingCitaOverlay)
        backButtonCitaDetail = findViewById(R.id.backButtonCitaDetail)
        btnGuardarCita = findViewById(R.id.btnGuardarCita)
        btnEditarCita = findViewById(R.id.btnEditarCita)
        btnEliminarCita = findViewById(R.id.btnEliminarCita)
        btnReanalizarCita = findViewById(R.id.btnReanalizarCita)

        backButtonCitaDetail.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnEditarCita.setOnClickListener {
            toggleEditMode()
        }

        btnEliminarCita.setOnClickListener {
            confirmarEliminacion()
        }

        btnReanalizarCita.setOnClickListener {
            reanalizarFotografia()
        }

        btnGuardarCita.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val manualData = AppointmentResult(
                    especialidad = appointmentSpecialtyDetail.text.toString(),
                    nombreMedico = appointmentDoctorDetail.text.toString(),
                    centroMedico = centerText.text.toString(),
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
                val uri = Uri.parse(uriString)
                appointmentLargeImage.setImageURI(uri)
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
            }
        }
    }

    private fun reanalizarFotografia() {
        if (currentImageUri.isNotEmpty()) {
            val uri = Uri.parse(currentImageUri)
            val base64 = convertUriToBase64(uri)
            if (base64 != null) {
                viewModel.analizarCita(base64, BuildConfig.OPENAI_API_KEY)
            } else {
                Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun configurarSelectores() {
        dateText.setOnClickListener {
            if (isEditMode) {
                val c = Calendar.getInstance()
                DatePickerDialog(this, { _, y, m, d ->
                    dateText.setText(String.format("%02d/%02d/%d", d, m + 1, y))
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }
        }

        hourText.setOnClickListener {
            if (isEditMode) {
                val c = Calendar.getInstance()
                TimePickerDialog(this, { _, h, min ->
                    val amPm = if (h >= 12) "PM" else "AM"
                    val displayHour = if (h > 12) h - 12 else if (h == 0) 12 else h
                    hourText.setText(String.format("%02d:%02d %s", displayHour, min, amPm))
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
            }
        }
    }

    private fun confirmarEliminacion() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Cita")
            .setMessage("¿Estás seguro de que deseas eliminar esta cita?")
            .setPositiveButton("Eliminar") { _, _ ->
                currentCita?.let { cita ->
                    auth.currentUser?.uid?.let { uid ->
                        citasViewModel.eliminarCita(cita.id, uid)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        
        appointmentSpecialtyDetail.isEnabled = isEditMode
        appointmentDoctorDetail.isEnabled = isEditMode
        centerText.isEnabled = isEditMode
        dateText.isEnabled = isEditMode
        hourText.isEnabled = isEditMode
        reasonText.isEnabled = isEditMode
        instructionsText.isEnabled = isEditMode

        if (isEditMode) {
            appointmentSpecialtyDetail.requestFocus()
            Toast.makeText(this, "Modo edición activado", Toast.LENGTH_SHORT).show()
            btnEditarCita.text = "BLOQUEAR EDICIÓN"
        } else {
            btnEditarCita.text = "EDITAR INFORMACIÓN"
        }
    }

    private fun mostrarDatosCita(c: CitaMedica) {
        appointmentSpecialtyDetail.setText(c.especialidad)
        appointmentDoctorDetail.setText(c.nombreMedico)
        centerText.setText(c.centroMedico)
        
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateText.setText(sdf.format(Date(c.fechaCita)))
        
        hourText.setText(c.horaCita)
        reasonText.setText(c.motivoConsulta)
        instructionsText.setText(c.instruccionesPrevias)
        
        if (c.imagenUri.isNotEmpty()) {
            try {
                appointmentLargeImage.setImageURI(Uri.parse(c.imagenUri))
            } catch (e: Exception) {
                appointmentLargeImage.setImageResource(R.mipmap.fondo)
            }
        }
    }

    private fun observarViewModel() {
        viewModel.appointmentData.observe(this) { data ->
            data?.let {
                appointmentSpecialtyDetail.setText(it.especialidad)
                appointmentDoctorDetail.setText(it.nombreMedico)
                centerText.setText(it.centroMedico)
                dateText.setText(it.fechaCita)
                hourText.setText(it.horaCita)
                reasonText.setText(it.motivoConsulta)
                instructionsText.setText(it.instruccionesPrevias)
                
                btnGuardarCita.isEnabled = true
                btnGuardarCita.alpha = 1.0f
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            loadingCitaOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.saveStatus.observe(this) { (success, result) ->
            if (success) {
                Toast.makeText(this, "Cita guardada con éxito", Toast.LENGTH_SHORT).show()
                currentCita = CitaMedica(
                    id = result ?: currentCita?.id ?: "",
                    userId = auth.currentUser?.uid ?: "",
                    especialidad = appointmentSpecialtyDetail.text.toString(),
                    nombreMedico = appointmentDoctorDetail.text.toString(),
                    centroMedico = centerText.text.toString(),
                    fechaCita = try {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateText.text.toString())?.time ?: 0L
                    } catch (e: Exception) { 0L },
                    horaCita = hourText.text.toString(),
                    motivoConsulta = reasonText.text.toString(),
                    instruccionesPrevias = instructionsText.text.toString(),
                    imagenUri = currentImageUri,
                    fechaCaptura = currentCita?.fechaCaptura ?: System.currentTimeMillis()
                )
                btnEliminarCita.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Error al guardar: $result", Toast.LENGTH_LONG).show()
            }
        }

        citasViewModel.deleteStatus.observe(this) { (success, message) ->
            if (success) {
                Toast.makeText(this, "Cita eliminada correctamente", Toast.LENGTH_SHORT).show()
                finish()
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
