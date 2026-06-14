package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medly_proyecto.model.*
import com.example.medly_proyecto.repository.NotificacionesRepository
import com.example.medly_proyecto.repository.OpenAIService
import com.example.medly_proyecto.repository.RecetasRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecipeResult(
    val nombreMedicamento: String = "",
    val instrucciones: String = "",
    val dosis: String = "",
    val frecuencia: String = "",
    val duracion: String = "",
    val cantidadTotal: String = "",
    val cantidadEnvase: String = "",
    val vecesAlDia: String = "",
    val metodoUso: String = "",
    val esRecetaValida: Boolean = true
)

class RecetaDetalleViewModel : ViewModel() {
    private val openAIService = OpenAIService.create()
    private val repository = RecetasRepository()
    private val notifRepo = NotificacionesRepository()
    private val gson = Gson()

    private val _recipeData = MutableLiveData<RecipeResult?>()
    val recipeData: LiveData<RecipeResult?> = _recipeData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveStatus = MutableLiveData<Pair<Boolean, String?>>()
    val saveStatus: LiveData<Pair<Boolean, String?>> = _saveStatus

    private val _deleteStatus = MutableLiveData<Pair<Boolean, String?>>()
    val deleteStatus: LiveData<Pair<Boolean, String?>> = _deleteStatus

    fun analizarReceta(base64Image: String, apiKey: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = """
                    Analiza la imagen proporcionada. Determina si es una receta médica legible.
                    Extract medical recipe info into JSON. 
                    Keys: nombreMedicamento, instrucciones, dosis, frecuencia, duracion, cantidadTotal, cantidadEnvase, vecesAlDia, metodoUso, esRecetaValida (boolean).
                    Si no es una receta médica, no parece un documento de salud, o no es legible en absoluto, pon esRecetaValida a false.
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
                    val result = gson.fromJson(content, RecipeResult::class.java)
                    _recipeData.postValue(result)
                }
            } catch (e: Exception) {
                _error.postValue("Error en análisis: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun guardarReceta(userId: String, imageUri: String, manualData: RecipeResult, recetaId: String? = null, tratamientoIniciado: Boolean = false) {
        val receta = Receta(
            id = recetaId ?: "",
            userId = userId,
            nombreMedicamento = manualData.nombreMedicamento,
            dosis = manualData.dosis,
            frecuencia = manualData.frecuencia,
            duracion = manualData.duracion,
            cantidadTotal = manualData.cantidadTotal,
            cantidadEnvase = manualData.cantidadEnvase,
            vecesAlDia = manualData.vecesAlDia,
            instrucciones = manualData.instrucciones,
            metodoUso = manualData.metodoUso,
            fechaCaptura = System.currentTimeMillis(),
            imagenUri = imageUri,
            tratamientoIniciado = tratamientoIniciado
        )

        _isLoading.value = true
        repository.guardarRecetaEncriptada(receta) { success, message ->
            if (success) {
                notifRepo.guardarNotificacion(Notificacion(
                    userId = userId,
                    titulo = "Receta guardada",
                    descripcion = "Se ha registrado la receta de ${manualData.nombreMedicamento}",
                    tipo = "RECETA"
                ))
            }
            _isLoading.postValue(false)
            _saveStatus.postValue(Pair(success, message))
        }
    }

    fun iniciarTratamiento(receta: Receta) {
        _isLoading.value = true
        val recetaActiva = receta.copy(tratamientoIniciado = true)
        repository.guardarRecetaEncriptada(recetaActiva) { success, message ->
            _isLoading.postValue(false)
            _saveStatus.postValue(Pair(success, message))
        }
    }

    fun eliminarReceta(recetaId: String) {
        _isLoading.value = true
        repository.eliminarReceta(recetaId) { success, message ->
            _isLoading.postValue(false)
            _deleteStatus.postValue(Pair(success, message))
        }
    }
}
