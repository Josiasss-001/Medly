package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.*
import com.example.medly_proyecto.repository.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentosViewModel : ViewModel() {
    private val repository = DocumentoRepository()
    private val recetasRepository = RecetasRepository()
    private val citasRepository = CitasRepository()
    private val usuarioRepo = UsuarioRepository()
    private val openAIService = OpenAIService.create()
    private val gson = Gson()
    
    private val _documentos = MutableLiveData<List<Documento>?>()
    val documentos: LiveData<List<Documento>?> = _documentos

    private val _recetas = MutableLiveData<List<Receta>?>()
    val recetas: LiveData<List<Receta>?> = _recetas

    private val _citas = MutableLiveData<List<CitaMedica>?>()
    val citas: LiveData<List<CitaMedica>?> = _citas

    private val _recetasFiltradas = MutableLiveData<List<Receta>?>()
    val recetasFiltradas: LiveData<List<Receta>?> = _recetasFiltradas

    private val _citasFiltradas = MutableLiveData<List<CitaMedica>?>()
    val citasFiltradas: LiveData<List<CitaMedica>?> = _citasFiltradas

    private val _recetasCount = MutableLiveData<Int>(0)
    val recetasCount: LiveData<Int> = _recetasCount

    private val _citasCount = MutableLiveData<Int>(0)
    val citasCount: LiveData<Int> = _citasCount

    private val _listaRecientesUnificada = MutableLiveData<List<Documento>>()
    val listaRecientesUnificada: LiveData<List<Documento>> = _listaRecientesUnificada

    private val _isAnalyzing = MutableLiveData<Boolean>()
    val isAnalyzing: LiveData<Boolean> = _isAnalyzing

    private val _isLoading = MutableLiveData<Boolean>(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _classificationResult = MutableLiveData<ClassificationResult?>()
    val classificationResult: LiveData<ClassificationResult?> = _classificationResult

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _deleteStatus = MutableLiveData<Pair<Boolean, String?>>()
    val deleteStatus: LiveData<Pair<Boolean, String?>> = _deleteStatus

    private val _sugerencias = MutableLiveData<List<Sugerencia>>()
    val sugerencias: LiveData<List<Sugerencia>> = _sugerencias
    
    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val _perfilImagenes = MutableLiveData<PerfilImagenes?>()
    val perfilImagenes: LiveData<PerfilImagenes?> = _perfilImagenes

    private var tipoActual = "Todos"
    private var currentQuery = ""
    private var allDocsUnified = mutableListOf<Documento>()
    
    private var docsLoaded = false
    private var recetasLoaded = false
    private var citasLoaded = false
    private var minTimePassed = false

    private var currentDocs: List<Documento> = emptyList()
    private var currentRecetas: List<Receta> = emptyList()
    private var currentCitas: List<CitaMedica> = emptyList()

    fun cargarDocumentos(userId: String) {
        _isLoading.value = true
        docsLoaded = false
        recetasLoaded = false
        citasLoaded = false
        minTimePassed = false

        viewModelScope.launch {
            delay(2000)
            minTimePassed = true
            verificarCargaCompleta()
        }

        repository.getDocumentos(userId) { docs ->
            currentDocs = docs ?: emptyList()
            _documentos.postValue(currentDocs)
            docsLoaded = true
            actualizarDatosYFiltrar()
            verificarCargaCompleta()
        }

        recetasRepository.getRecetasDesencriptadas(userId) { lista ->
            currentRecetas = lista ?: emptyList()
            _recetas.postValue(currentRecetas)
            _recetasCount.postValue(currentRecetas.size)
            recetasLoaded = true
            actualizarDatosYFiltrar()
            verificarCargaCompleta()
        }

        citasRepository.getCitasDesencriptadas(userId) { lista ->
            currentCitas = lista ?: emptyList()
            _citas.postValue(currentCitas)
            _citasCount.postValue(currentCitas.size)
            citasLoaded = true
            actualizarDatosYFiltrar()
            verificarCargaCompleta()
        }
        
        usuarioRepo.getUsuario(userId) { user ->
            _usuario.postValue(user)
        }
        usuarioRepo.getPerfilImagenes(userId) { imagenes ->
            _perfilImagenes.postValue(imagenes)
        }
    }

    private fun verificarCargaCompleta() {
        if (docsLoaded && recetasLoaded && citasLoaded && minTimePassed) {
            _isLoading.postValue(false)
        }
    }

    private fun actualizarDatosYFiltrar() {
        allDocsUnified.clear()
        allDocsUnified.addAll(currentDocs)
        
        currentRecetas.forEach { receta ->
            allDocsUnified.add(Documento(
                id = receta.id,
                userId = receta.userId,
                titulo = receta.nombreMedicamento,
                tipo = "RECETA",
                fecha = receta.fechaCaptura,
                resumen = receta.instrucciones,
                imagenUri = receta.imagenUri
            ))
        }
        
        currentCitas.forEach { cita ->
            allDocsUnified.add(Documento(
                id = cita.id,
                userId = cita.userId,
                titulo = "Cita: ${cita.especialidad}",
                tipo = "CITA",
                fecha = cita.fechaCaptura,
                institucion = cita.centroMedico,
                resumen = "Dr. ${cita.nombreMedico}",
                imagenUri = cita.imagenUri
            ))
        }
        aplicarFiltros()
    }

    fun buscarDocumentos(query: String) {
        currentQuery = query
        generarSugerencias(query)
        aplicarFiltros()
    }

    private fun generarSugerencias(query: String) {
        if (query.isEmpty()) {
            _sugerencias.postValue(emptyList())
            return
        }
        val q = query.lowercase().trim()
        val listaSugerencias = mutableListOf<Sugerencia>()

        if ("recetas".contains(q)) listaSugerencias.add(Sugerencia("Ver Recetas Médicas", R.drawable.archivo, 101))
        if ("citas".contains(q)) listaSugerencias.add(Sugerencia("Ver Citas Médicas", R.drawable.horas2, 102))
        if ("examenes".contains(q) || "exámenes".contains(q)) listaSugerencias.add(Sugerencia("Ver Exámenes", R.drawable.examen, 103))
        if ("certificados".contains(q)) listaSugerencias.add(Sugerencia("Ver Certificados", R.drawable.archivo, 104))
        if ("ordenes".contains(q) || "órdenes".contains(q)) listaSugerencias.add(Sugerencia("Ver Órdenes Médicas", R.drawable.archivo, 105))

        allDocsUnified.filter { it.titulo.lowercase().contains(q) }
            .take(3)
            .forEach { doc ->
                val icono = when(doc.tipo.uppercase()) {
                    "RECETA" -> R.drawable.archivo
                    "CITA" -> R.drawable.horas2
                    "EXAMEN" -> R.drawable.examen
                    else -> R.drawable.archivo
                }
                listaSugerencias.add(Sugerencia(doc.titulo, icono, 200))
            }

        _sugerencias.postValue(listaSugerencias)
    }

    fun filtrarPorTipo(tipo: String) {
        tipoActual = tipo
        aplicarFiltros()
    }

    private fun aplicarFiltros() {
        val q = currentQuery.lowercase()
        val filteredUnified = allDocsUnified.filter { doc ->
            doc.titulo.lowercase().contains(q) && (tipoActual == "Todos" || doc.tipo.equals(tipoActual, true))
        }.sortedByDescending { it.fecha }
        
        _listaRecientesUnificada.postValue(if (currentQuery.isEmpty() && tipoActual == "Todos") filteredUnified.take(3) else filteredUnified)
        _recetasFiltradas.postValue(currentRecetas.filter { it.nombreMedicamento.lowercase().contains(q) })
        _citasFiltradas.postValue(currentCitas.filter { it.especialidad.lowercase().contains(q) || it.nombreMedico.lowercase().contains(q) })
    }

    fun clasificarDocumento(base64Image: String? = null, texto: String? = null, apiKey: String) {
        _isAnalyzing.value = true
        viewModelScope.launch {
            try {
                val prompt = if (texto != null) {
                    """
                    Eres un experto validador de documentos médicos. Analiza el siguiente texto extraído:
                    
                    Texto:
                    $texto
                    
                    Determina si es una 'RECETA', 'CITA' o 'DOCUMENTO' médico válido.
                    Criterios:
                    - 'RECETA': Prescripción de fármacos, dosis, indicaciones médicas.
                    - 'CITA': Fecha de atención, especialidad, nombre de médico o institución.
                    - 'DOCUMENTO': Exámenes clínicos, certificados de salud, órdenes médicas.
                    
                    Responde ÚNICAMENTE en JSON:
                    {
                      "isValid": boolean,
                      "type": "RECETA" | "CITA" | "DOCUMENTO" | "INVALID",
                      "error": "Mensaje si no es médico"
                    }
                    """.trimIndent()
                } else {
                    """
                    Analiza estrictamente esta imagen y determina si es un documento médico válido.
                    Responde ÚNICAMENTE en formato JSON:
                    {
                      "isValid": boolean,
                      "type": "RECETA" | "CITA" | "DOCUMENTO" | "INVALID",
                      "error": "El archivo seleccionado no corresponde a un documento médico válido."
                    }
                    """.trimIndent()
                }
                
                val contentParts = mutableListOf<ContentPart>()
                contentParts.add(ContentPart(type = "text", text = prompt))
                if (base64Image != null) {
                    contentParts.add(ContentPart(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,$base64Image")))
                }

                val request = ChatRequest(
                    model = "gpt-4o-mini",
                    messages = listOf(Message(role = "user", content = contentParts)),
                    response_format = ResponseFormat(type = "json_object")
                )
                val response = withContext(Dispatchers.IO) { openAIService.getCompletion("Bearer $apiKey", request) }
                val content = response.choices.firstOrNull()?.message?.content
                if (content != null) {
                    val result = gson.fromJson(content, ClassificationResult::class.java)
                    _classificationResult.postValue(result)
                }
            } catch (e: Exception) {
                _error.postValue("Error en validación: ${e.message}")
            } finally {
                _isAnalyzing.postValue(false)
            }
        }
    }

    fun eliminarReceta(recetaId: String, userId: String) {
        _isLoading.value = true
        recetasRepository.eliminarReceta(recetaId) { success, message ->
            _deleteStatus.postValue(Pair(success, message))
            if (success) cargarDocumentos(userId)
            else _isLoading.postValue(false)
        }
    }

    fun eliminarCita(citaId: String, userId: String) {
        _isLoading.value = true
        citasRepository.eliminarCita(citaId) { success, message ->
            _deleteStatus.postValue(Pair(success, message))
            if (success) cargarDocumentos(userId)
            else _isLoading.postValue(false)
        }
    }

    fun eliminarDocumentoGeneral(docId: String, userId: String) {
        _isLoading.value = true
        repository.eliminarDocumento(docId) { success, message ->
            _deleteStatus.postValue(Pair(success, message))
            if (success) cargarDocumentos(userId)
            else _isLoading.postValue(false)
        }
    }

    fun resetClassification() { _classificationResult.value = null }
}
