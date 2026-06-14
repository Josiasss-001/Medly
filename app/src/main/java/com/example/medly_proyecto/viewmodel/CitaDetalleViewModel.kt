package com.example.medly_proyecto.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.medly_proyecto.model.*
import com.example.medly_proyecto.notification.ReminderScheduler
import com.example.medly_proyecto.repository.NotificacionesRepository
import com.example.medly_proyecto.repository.OpenAIService
import com.example.medly_proyecto.repository.CitasRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

data class AppointmentResult(
    val especialidad: String = "",
    val nombreMedico: String = "",
    val centroMedico: String = "",
    val direccion: String = "",
    val fechaCita: String = "",
    val horaCita: String = "",
    val motivoConsulta: String = "",
    val instruccionesPrevias: String = "",
    val esCitaValida: Boolean = true
)

class CitaDetalleViewModel(application: Application) : AndroidViewModel(application) {
    private val openAIService = OpenAIService.create()
    private val citasRepo = CitasRepository()
    private val notifRepo = NotificacionesRepository()
    private val scheduler = ReminderScheduler(application)
    private val gson = Gson()

    private val _appointmentData = MutableLiveData<AppointmentResult?>()
    val appointmentData: LiveData<AppointmentResult?> = _appointmentData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isAnalyzing = MutableLiveData<Boolean>()
    val isAnalyzing: LiveData<Boolean> = _isAnalyzing

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveStatus = MutableLiveData<Pair<Boolean, String?>>()
    val saveStatus: LiveData<Pair<Boolean, String?>> = _saveStatus

    fun setAnalyzing(analyzing: Boolean) {
        _isAnalyzing.value = analyzing
    }

    fun analizarCita(base64Image: String, apiKey: String) {
        _isAnalyzing.value = true
        viewModelScope.launch {
            try {
                val prompt = """
                    Analiza la imagen proporcionada. Determina si es un documento de cita médica o reserva de hora médica legible.
                    Extract appointment info from image into JSON. 
                    Keys: especialidad, nombreMedico, centroMedico, direccion, fechaCita (DD/MM/YYYY), horaCita, motivoConsulta, instruccionesPrevias, esCitaValida (boolean).
                    Si no es un documento de cita médica, no parece un documento de salud, o no es legible en absoluto, pon esCitaValida a false.
                    Return ONLY the JSON.
                """.trimIndent()

                val request = ChatRequest(
                    model = "gpt-4o-mini",
                    messages = listOf(Message(role = "user", content = listOf(
                        ContentPart(type = "text", text = prompt),
                        ContentPart(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,$base64Image"))
                    ))),
                    response_format = ResponseFormat(type = "json_object")
                )

                val response = withContext(Dispatchers.IO) { openAIService.getCompletion("Bearer $apiKey", request) }
                val content = response.choices.firstOrNull()?.message?.content
                if (content != null) {
                    val result = gson.fromJson(content, AppointmentResult::class.java)
                    _appointmentData.postValue(result)
                }
            } catch (e: Exception) {
                _error.postValue("Error en análisis: ${e.message}")
            } finally {
                _isAnalyzing.postValue(false)
            }
        }
    }

    fun guardarCita(userId: String, imageUri: String, manualData: AppointmentResult, citaId: String? = null) {
        val timestamp = try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(manualData.fechaCita)?.time ?: 0L
        } catch (e: Exception) { 0L }

        val cita = CitaMedica(
            id = citaId ?: "",
            userId = userId,
            especialidad = manualData.especialidad,
            nombreMedico = manualData.nombreMedico,
            centroMedico = manualData.centroMedico,
            direccion = manualData.direccion,
            fechaCita = timestamp,
            horaCita = manualData.horaCita,
            motivoConsulta = manualData.motivoConsulta,
            instruccionesPrevias = manualData.instruccionesPrevias,
            imagenUri = imageUri,
            fechaCaptura = System.currentTimeMillis()
        )

        _isLoading.value = true
        citasRepo.guardarCitaEncriptada(cita) { success, id ->
            if (success && id != null) {
                val citaConId = cita.copy(id = id)
                scheduler.programarRecordatoriosCita(citaConId)
                
                // Registrar en historial de notificaciones
                notifRepo.guardarNotificacion(Notificacion(
                    userId = userId,
                    titulo = "Cita agendada",
                    descripcion = "Nueva cita: ${manualData.especialidad} en ${manualData.centroMedico}",
                    tipo = "CITA"
                ))
            }
            _isLoading.postValue(false)
            _saveStatus.postValue(Pair(success, id))
        }
    }
}
