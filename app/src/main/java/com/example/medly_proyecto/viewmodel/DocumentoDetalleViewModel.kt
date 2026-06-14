package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medly_proyecto.model.*
import com.example.medly_proyecto.repository.DocumentoRepository
import com.example.medly_proyecto.repository.NotificacionesRepository
import com.example.medly_proyecto.repository.OpenAIService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DocumentResult(
    val titulo: String = "",
    val tipo: String = "OTRO",
    val fecha: String = "",
    val institucion: String = "",
    val resumen: String = ""
)

class DocumentoDetalleViewModel : ViewModel() {
    private val openAIService = OpenAIService.create()
    private val documentoRepo = DocumentoRepository()
    private val notifRepo = NotificacionesRepository()
    private val gson = Gson()

    private val _docData = MutableLiveData<DocumentResult?>()
    val docData: LiveData<DocumentResult?> = _docData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveStatus = MutableLiveData<Boolean>()
    val saveStatus: LiveData<Boolean> = _saveStatus

    fun analizarDocumento(base64Image: String, apiKey: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = """
                    Extract medical document info into JSON. 
                    Identify type: RECETA, CITA, EXAMEN, CERTIFICADO, ORDEN, OTRO.
                    JSON keys: titulo, tipo, fecha (DD/MM/YYYY), institucion, resumen.
                    Return ONLY the JSON.
                """.trimIndent()

                val request = ChatRequest(
                    model = "gpt-4o-mini",
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

                var jsonContent = response.choices.firstOrNull()?.message?.content
                if (jsonContent != null) {
                    jsonContent = jsonContent.replace("```json", "").replace("```", "").trim()
                    val result = gson.fromJson(jsonContent, DocumentResult::class.java)
                    _docData.postValue(result)
                }
            } catch (e: Exception) {
                _error.postValue("Error en análisis rápido: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun guardarDocumento(userId: String, imageUri: String, manualData: DocumentResult) {
        val documento = Documento(
            userId = userId,
            titulo = manualData.titulo,
            tipo = manualData.tipo,
            fecha = System.currentTimeMillis(),
            fechaTexto = manualData.fecha,
            institucion = manualData.institucion,
            resumen = manualData.resumen,
            imagenUri = imageUri
        )

        _isLoading.value = true
        documentoRepo.guardarDocumento(documento) { success ->
            if (success) {
                // Registrar en historial de notificaciones
                notifRepo.guardarNotificacion(Notificacion(
                    userId = userId,
                    titulo = "Documento agregado",
                    descripcion = "Se ha guardado el documento: ${manualData.titulo}",
                    tipo = "DOCUMENTO"
                ))
            }
            _isLoading.postValue(false)
            _saveStatus.postValue(success)
        }
    }
}
