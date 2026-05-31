package com.example.medly_proyecto.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medly_proyecto.model.ChatRequest
import com.example.medly_proyecto.model.ContentPart
import com.example.medly_proyecto.model.ImageUrl
import com.example.medly_proyecto.model.Message
import com.example.medly_proyecto.model.CitaMedica
import com.example.medly_proyecto.model.ResponseFormat
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
    val fechaCita: String = "", // Formato sugerido DD/MM/YYYY
    val horaCita: String = "",
    val motivoConsulta: String = "",
    val instruccionesPrevias: String = ""
)

class CitaDetalleViewModel : ViewModel() {
    private val openAIService = OpenAIService.create()
    private val citasRepo = CitasRepository()
    private val gson = Gson()

    private val _appointmentData = MutableLiveData<AppointmentResult?>()
    val appointmentData: LiveData<AppointmentResult?> = _appointmentData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveStatus = MutableLiveData<Pair<Boolean, String?>>()
    val saveStatus: LiveData<Pair<Boolean, String?>> = _saveStatus

    fun analizarCita(base64Image: String, apiKey: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = """
                    Analiza esta imagen de una cita médica y extrae la información en formato JSON estricto.
                    Debes incluir los siguientes campos:
                    - especialidad (ej: Cardiología)
                    - nombreMedico
                    - centroMedico (hospital o clínica)
                    - fechaCita (formato DD/MM/YYYY)
                    - horaCita (ej: 10:30 AM)
                    - motivoConsulta
                    - instruccionesPrevias (ayuno, exámenes a llevar, etc.)
                    
                    Si no encuentras algún dato, deja el valor como "".
                """.trimIndent()

                val request = ChatRequest(
                    model = "gpt-4o",
                    messages = listOf(
                        Message(
                            role = "user",
                            content = listOf(
                                ContentPart(type = "text", text = prompt),
                                ContentPart(
                                    type = "image_url",
                                    image_url = ImageUrl(url = "data:image/jpeg;base64,$base64Image")
                                )
                            )
                        )
                    ),
                    response_format = ResponseFormat(type = "json_object")
                )

                val response = withContext(Dispatchers.IO) {
                    openAIService.getCompletion("Bearer $apiKey", request)
                }

                val jsonContent = response.choices.firstOrNull()?.message?.content
                if (jsonContent != null) {
                    val result = gson.fromJson(jsonContent, AppointmentResult::class.java)
                    _appointmentData.postValue(result)
                } else {
                    _error.postValue("No se pudo procesar la respuesta de la IA")
                }
            } catch (e: Exception) {
                Log.e("CitaDetalleVM", "Error analizando cita", e)
                _error.postValue("Error: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun guardarCita(userId: String, imageUri: String, manualData: AppointmentResult, citaId: String? = null) {
        val timestamp = try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.parse(manualData.fechaCita)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }

        val cita = CitaMedica(
            id = citaId ?: "",
            userId = userId,
            especialidad = manualData.especialidad,
            nombreMedico = manualData.nombreMedico,
            centroMedico = manualData.centroMedico,
            fechaCita = timestamp,
            horaCita = manualData.horaCita,
            motivoConsulta = manualData.motivoConsulta,
            instruccionesPrevias = manualData.instruccionesPrevias,
            imagenUri = imageUri
        )

        _isLoading.value = true
        citasRepo.guardarCitaEncriptada(cita) { success, message ->
            _isLoading.postValue(false)
            _saveStatus.postValue(Pair(success, message))
        }
    }
}
