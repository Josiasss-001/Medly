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
import java.util.*

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
                    Debes incluir los siguientes campos:
                    - nombreMedicamento
                    - dosis
                    - frecuencia
                    - duracion
                    - cantidadTotal
                    - cantidadEnvase
                    - vecesAlDia
                    - instrucciones
                    - metodoUso
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
                }
            } catch (e: Exception) {
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
            imagenUri = imageUri,
            fechaCaptura = System.currentTimeMillis()
        )

        _isLoading.value = true
        recetasRepo.guardarRecetaEncriptada(receta) { success, docId ->
            _isLoading.postValue(false)
            _saveStatus.postValue(Pair(success, docId))
        }
    }
}
