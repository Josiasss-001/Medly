package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medly_proyecto.model.*
import com.example.medly_proyecto.repository.OpenAIService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScannerCameraViewModel : ViewModel() {
    private val openAIService = OpenAIService.create()
    private val gson = Gson()

    private val _isAnalyzing = MutableLiveData<Boolean>(false)
    val isAnalyzing: LiveData<Boolean> = _isAnalyzing

    private val _classificationResult = MutableLiveData<ClassificationResult?>()
    val classificationResult: LiveData<ClassificationResult?> = _classificationResult

    fun validarImagen(base64Image: String, apiKey: String) {
        _isAnalyzing.value = true
        viewModelScope.launch {
            try {
                val prompt = """
                    Eres un validador estricto de documentos médicos. Tu única función es determinar si la imagen es un documento clínico válido.
                    
                    RECHAZA (isValid: false) CATEGÓRICAMENTE si la imagen es:
                    1. Una persona, rostro, selfie o partes del cuerpo (ej. manos sosteniendo nada, brazos).
                    2. Un animal o mascota.
                    3. Un entorno (pared, piso, mesa, silla, ventana, exterior, vehículo).
                    4. Una captura de pantalla de redes sociales, chats (WhatsApp), memes o sitios web.
                    5. Objetos comunes (TV, control, celular, llaves, comida).
                    6. Fotografías desenfocadas, muy oscuras o donde no se aprecie texto médico.
                    
                    ACEPTA (isValid: true) SOLO SI ES:
                    - 'RECETA': Prescripción de fármacos con dosis y firma/sello (aunque sea parcial).
                    - 'CITA': Tarjeta, volante o documento de recordatorio de cita médica.
                    - 'DOCUMENTO': Resultados de laboratorio, exámenes (Rayos X, sangre), órdenes médicas o certificados.
                    
                    Responde únicamente este JSON:
                    {
                      "isValid": boolean,
                      "type": "RECETA" | "CITA" | "DOCUMENTO" | "INVALID",
                      "error": "El archivo seleccionado no corresponde a un documento médico válido o no contiene información suficiente para ser procesado."
                    }
                """.trimIndent()

                val request = ChatRequest(
                    model = "gpt-4o-mini",
                    messages = listOf(
                        Message(role = "system", content = "Eres un asistente de validación de documentos médicos. Responde siempre en JSON."),
                        Message(role = "user", content = listOf(
                            ContentPart(type = "text", text = prompt),
                            ContentPart(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,$base64Image"))
                        ))
                    ),
                    response_format = ResponseFormat(type = "json_object")
                )

                val response = withContext(Dispatchers.IO) { 
                    openAIService.getCompletion("Bearer $apiKey", request) 
                }
                
                val content = response.choices.firstOrNull()?.message?.content
                if (content != null) {
                    val result = gson.fromJson(content, ClassificationResult::class.java)
                    _classificationResult.postValue(result)
                } else {
                    _classificationResult.postValue(ClassificationResult(isValid = false))
                }
            } catch (e: Exception) {
                _classificationResult.postValue(ClassificationResult(isValid = false, error = "Error de conexión al validar"))
            } finally {
                _isAnalyzing.postValue(false)
            }
        }
    }

    fun resetResult() {
        _classificationResult.value = null
    }
}
