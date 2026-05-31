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
import com.example.medly_proyecto.model.Receta
import com.example.medly_proyecto.model.ResponseFormat
import com.example.medly_proyecto.repository.OpenAIService
import com.example.medly_proyecto.repository.RecetasRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecipeResult(
    val nombreMedicamento: String = "",
    val dosis: String = "",
    val frecuencia: String = "",
    val duracion: String = "",
    val cantidadTotal: String = "",
    val cantidadEnvase: String = "",
    val vecesAlDia: String = "",
    val instrucciones: String = "",
    val metodoUso: String = ""
)

class RecetaDetalleViewModel : ViewModel() {
    private val openAIService = OpenAIService.create()
    private val recetasRepo = RecetasRepository()
    private val gson = Gson()

    private val _recipeData = MutableLiveData<RecipeResult?>()
    val recipeData: LiveData<RecipeResult?> = _recipeData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveStatus = MutableLiveData<Pair<Boolean, String?>>()
    val saveStatus: LiveData<Pair<Boolean, String?>> = _saveStatus

    fun analizarReceta(base64Image: String, apiKey: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = """
                    Analiza esta imagen de una receta médica y extrae la información en formato JSON estricto.
                    NO extraigas ni incluyas el nombre del médico.
                    Debes incluir los siguientes campos:
                    - nombreMedicamento
                    - dosis (ej: 500mg)
                    - frecuencia (ej: cada 8 horas)
                    - duracion (ej: 7 días)
                    - cantidadTotal (la cantidad total que el paciente debe consumir, ej: 21 cápsulas)
                    - cantidadEnvase (la cantidad que trae la caja o frasco, ej: 30 cápsulas)
                    - vecesAlDia (ej: 3 veces al día)
                    - instrucciones (indicaciones adicionales breves)
                    - metodoUso (Genera un texto detallado y profesional en español que explique para qué sirve este medicamento y las instrucciones paso a paso de cómo consumirlo correctamente).
                    
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
                    val result = gson.fromJson(jsonContent, RecipeResult::class.java)
                    _recipeData.postValue(result)
                } else {
                    _error.postValue("No se pudo procesar la respuesta de la IA")
                }
            } catch (e: Exception) {
                Log.e("RecetaDetalleVM", "Error analizando receta", e)
                _error.postValue("Error: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun guardarReceta(userId: String, imageUri: String, manualData: RecipeResult? = null, recetaId: String? = null) {
        val dataToSave = manualData ?: _recipeData.value ?: return
        
        val receta = Receta(
            id = recetaId ?: "",
            userId = userId,
            nombreMedicamento = dataToSave.nombreMedicamento,
            dosis = dataToSave.dosis,
            frecuencia = dataToSave.frecuencia,
            duracion = dataToSave.duracion,
            cantidadTotal = dataToSave.cantidadTotal,
            cantidadEnvase = dataToSave.cantidadEnvase,
            vecesAlDia = dataToSave.vecesAlDia,
            instrucciones = dataToSave.instrucciones,
            metodoUso = dataToSave.metodoUso,
            imagenUri = imageUri
        )

        _isLoading.value = true
        recetasRepo.guardarRecetaEncriptada(receta) { success, message ->
            _isLoading.postValue(false)
            _saveStatus.postValue(Pair(success, message))
        }
    }
}
