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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class HomeVisibilidadEstado(
    val recetasVisible: Boolean = true,
    val citasVisible: Boolean = true,
    val statsVisible: Boolean = true,
    val quoteVisible: Boolean = true,
    val sinResultadosVisible: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val usuarioRepo = UsuarioRepository()
    private val recetasRepo = RecetasRepository()
    private val citasRepo = CitasRepository()
    private val notifRepo = NotificacionesRepository()
    private val openAIService = OpenAIService.create()
    private val gson = Gson()

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val _perfilImagenes = MutableLiveData<PerfilImagenes?>()
    val perfilImagenes: LiveData<PerfilImagenes?> = _perfilImagenes

    private val _fraseIA = MutableLiveData<String>()
    val fraseIA: LiveData<String> = _fraseIA

    private val _estadoVisibilidad = MutableLiveData<HomeVisibilidadEstado>()
    val estadoVisibilidad: LiveData<HomeVisibilidadEstado> = _estadoVisibilidad

    private val _sugerencias = MutableLiveData<List<Sugerencia>>()
    val sugerencias: LiveData<List<Sugerencia>> = _sugerencias

    private val _datosMedicos = MutableLiveData<DatosMedicos?>()
    val datosMedicos: LiveData<DatosMedicos?> = _datosMedicos

    private val _tomasDelDia = MutableLiveData<List<TomaMedicamento>>()
    val tomasDelDia: LiveData<List<TomaMedicamento>> = _tomasDelDia

    private val _proximaToma = MutableLiveData<TomaMedicamento?>()
    val proximaToma: LiveData<TomaMedicamento?> = _proximaToma

    private val _conteoNotificaciones = MutableLiveData<Int>(0)
    val conteoNotificaciones: LiveData<Int> = _conteoNotificaciones

    private val _proximaCita = MutableLiveData<CitaMedica?>()
    val proximaCita: LiveData<CitaMedica?> = _proximaCita

    private val _progresoMedicamentos = MutableLiveData<Pair<Int, Int>>()
    val progresoMedicamentos: LiveData<Pair<Int, Int>> = _progresoMedicamentos

    private val _notificacionesHistorial = MutableLiveData<List<Notificacion>>()
    val notificacionesHistorial: LiveData<List<Notificacion>> = _notificacionesHistorial

    private val _tieneNotificacionesNuevas = MutableLiveData<Boolean>(false)
    val tieneNotificacionesNuevas: LiveData<Boolean> = _tieneNotificacionesNuevas

    private val _alertas = MutableLiveData<List<AlertaHogar>>()
    val alertas: LiveData<List<AlertaHogar>> = _alertas

    private val _isAnalyzing = MutableLiveData<Boolean>()
    val isAnalyzing: LiveData<Boolean> = _isAnalyzing

    private val _classificationResult = MutableLiveData<ClassificationResult?>()
    val classificationResult: LiveData<ClassificationResult?> = _classificationResult

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var todasLasCitas: List<CitaMedica> = emptyList()
    private var todasLasRecetas: List<Receta> = emptyList()

    init {
        _estadoVisibilidad.value = HomeVisibilidadEstado()
        _sugerencias.value = emptyList()
        _tomasDelDia.value = emptyList()
        _notificacionesHistorial.value = emptyList()
        _alertas.value = emptyList()
    }

    fun cargarDatos(uid: String) {
        usuarioRepo.getUsuario(uid) { user -> _usuario.value = user }
        usuarioRepo.getPerfilImagenes(uid) { imagenes -> _perfilImagenes.value = imagenes }
        usuarioRepo.getDatosMedicos(uid) { datos -> _datosMedicos.value = datos }
        asegurarTomasYActualizar(uid)
        cargarProximaCita(uid)
        cargarNotificaciones(uid)
        generarAlertasAutomáticas(uid)
    }

    private fun generarAlertasAutomáticas(uid: String) {
        viewModelScope.launch {
            val todasLasAlertas = mutableListOf<AlertaHogar>()
            val hoyCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            val hoyMillis = hoyCal.timeInMillis

            // 1. ALERTAS DE DOSIS
            val sdfHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            recetasRepo.getTomasPorFecha(uid, sdfHoy) { tomas ->
                val pendientes = tomas?.filter { it.estado == "PENDIENTE" } ?: emptyList()
                if (pendientes.isNotEmpty()) {
                    todasLasAlertas.add(AlertaHogar(
                        id = "alerta_dosis_pendientes",
                        tipo = TipoAlerta.DOSIS,
                        titulo = "Dosis pendientes",
                        mensaje = "Tienes ${pendientes.size} dosis por tomar hoy.",
                        iconoRes = R.drawable.pildora,
                        priority = 1,
                        dataId = ""
                    ))
                }
                actualizarAlertasConsolidado(todasLasAlertas)
            }

            // 2. ALERTAS DE RECETAS
            recetasRepo.getRecetasDesencriptadas(uid) { recetas ->
                todasLasRecetas = recetas ?: emptyList()
                todasLasRecetas.forEach { receta ->
                    val duracionDias = try { Regex("\\d+").find(receta.duracion)?.value?.toInt() ?: 7 } catch (e: Exception) { 7 }
                    val calVence = Calendar.getInstance().apply { 
                        timeInMillis = receta.fechaCaptura
                        add(Calendar.DAY_OF_YEAR, duracionDias)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    
                    val diffMillis = calVence.timeInMillis - hoyMillis
                    val diasRestantes = TimeUnit.MILLISECONDS.toDays(diffMillis)

                    if (diasRestantes in 0..5) {
                        val msg = when {
                            diasRestantes == 0L -> "Tu receta vence hoy"
                            diasRestantes == 1L -> "Tu receta vence mañana"
                            else -> "Tu receta vence en $diasRestantes días"
                        }
                        todasLasAlertas.add(AlertaHogar(
                            id = "alerta_receta_${receta.id}",
                            tipo = TipoAlerta.RECETA,
                            titulo = "Receta por vencer",
                            mensaje = msg,
                            iconoRes = android.R.drawable.stat_notify_error,
                            priority = 2,
                            dataId = receta.id
                        ))
                    }
                }
                actualizarAlertasConsolidado(todasLasAlertas)
            }

            // 3. ALERTAS DE CITAS
            citasRepo.getCitasDesencriptadas(uid) { citas ->
                todasLasCitas = citas ?: emptyList()
                todasLasCitas.forEach { cita ->
                    val diff = cita.fechaCita - System.currentTimeMillis()
                    val dias = TimeUnit.MILLISECONDS.toDays(diff)
                    
                    if (diff > -TimeUnit.HOURS.toMillis(1) && dias <= 2) {
                        val msg = when {
                            dias == 0L -> "Tienes una hora hoy a las ${cita.horaCita}"
                            dias == 1L -> "Tienes una hora mañana a las ${cita.horaCita}"
                            else -> "Tienes una hora en $dias días a las ${cita.horaCita}"
                        }
                        todasLasAlertas.add(AlertaHogar(
                            id = "alerta_cita_${cita.id}",
                            tipo = TipoAlerta.CITA,
                            titulo = "Cita Próxima",
                            mensaje = msg,
                            iconoRes = R.drawable.calendario,
                            priority = 3,
                            dataId = cita.id
                        ))
                    }
                }
                actualizarAlertasConsolidado(todasLasAlertas)
            }
        }
    }

    private fun actualizarAlertasConsolidado(lista: MutableList<AlertaHogar>) {
        val final = lista.distinctBy { it.id }
            .sortedWith(compareBy({ it.priority }, { it.id }))
            .take(3)
        _alertas.postValue(final)
    }

    fun getCitaById(id: String) = todasLasCitas.find { it.id == id }
    fun getRecetaById(id: String) = todasLasRecetas.find { it.id == id }

    private fun cargarNotificaciones(uid: String) {
        notifRepo.getNotificaciones(uid) { lista ->
            lista?.let {
                _notificacionesHistorial.postValue(it)
                _tieneNotificacionesNuevas.postValue(it.any { n -> !n.leida })
            }
        }
    }

    fun marcarNotificacionesLeidas(uid: String) {
        notifRepo.marcarComoLeidas(uid)
        _tieneNotificacionesNuevas.value = false
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
                      "error": "El archivo seleccionado no corresponde a un documento médico válido."
                    }
                    """.trimIndent()
                } else {
                    """
                    Analiza estrictamente esta imagen y determina si es un documento médico válido.
                    Responde ÚNICAMENTE en formato JSON:
                    { "isValid": boolean, "type": "RECETA" | "CITA" | "DOCUMENTO" | "INVALID", "error": "El archivo seleccionado no corresponde a un documento médico válido.", "data": { ... } }
                """.trimIndent()
                }

                val contentParts = mutableListOf<ContentPart>()
                contentParts.add(ContentPart(type = "text", text = prompt))
                if (base64Image != null) {
                    contentParts.add(ContentPart(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,$base64Image")))
                }

                val request = ChatRequest(
                    model = "gpt-4o-mini",
                    messages = listOf(
                        Message(role = "user", content = contentParts)
                    ),
                    response_format = ResponseFormat(type = "json_object")
                )
                val response = withContext(Dispatchers.IO) { openAIService.getCompletion("Bearer $apiKey", request) }
                val content = response.choices.firstOrNull()?.message?.content
                if (content != null) {
                    val result = gson.fromJson(content, ClassificationResult::class.java)
                    _classificationResult.postValue(result)
                }
            } catch (e: Exception) { 
                _error.postValue("Error de conexión al validar")
            } finally { 
                _isAnalyzing.postValue(false) 
            }
        }
    }

    private fun asegurarTomasYActualizar(uid: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hoyStr = sdf.format(Date())
        val hoyCal = Calendar.getInstance()

        recetasRepo.getRecetasDesencriptadas(uid) { recetas ->
            val listaRecetas = recetas?.distinctBy { it.id } ?: emptyList()
            
            recetasRepo.getTomasPorFecha(uid, hoyStr) { tomasExistentes ->
                val tomas = tomasExistentes ?: emptyList()
                val tomasPorReceta = tomas.groupBy { it.idReceta }
                
                val nuevasTomas = mutableListOf<TomaMedicamento>()
                
                listaRecetas.forEach { receta ->
                    if (aplicaParaFecha(receta, hoyCal)) {
                        val tomasHoy = tomasPorReceta[receta.id] ?: emptyList()
                        if (tomasHoy.isEmpty()) {
                            nuevasTomas.addAll(generarTomasDeUnDia(receta, hoyStr))
                        }
                    }
                }

                if (nuevasTomas.isNotEmpty()) {
                    recetasRepo.guardarTomasMasivas(nuevasTomas) {
                        actualizarProximaTomaYProgreso(uid, hoyStr)
                    }
                } else {
                    actualizarProximaTomaYProgreso(uid, hoyStr)
                }
            }
        }
    }

    private fun obtenerTotalTomasEsperadas(receta: Receta): Int {
        val vecesNum = Regex("\\d+").find(receta.vecesAlDia)?.value?.toInt() ?: 0
        if (vecesNum > 0) return vecesNum
        val frecuenciaNum = Regex("\\d+").find(receta.frecuencia)?.value?.toInt() ?: 0
        return when {
            frecuenciaNum >= 4 -> 24 / frecuenciaNum
            frecuenciaNum > 0 -> frecuenciaNum
            else -> 1
        }
    }

    private fun actualizarProximaTomaYProgreso(uid: String, hoyStr: String) {
        recetasRepo.getRecetasDesencriptadas(uid) { recetas ->
            val recetasIds = recetas?.map { it.id }?.toSet() ?: emptySet()
            val recetasMap = recetas?.associateBy { it.id } ?: emptyMap()

            recetasRepo.getTomasPorFecha(uid, hoyStr) { tomas ->
                if (tomas == null) return@getTomasPorFecha
                
                val tomasValidas = mutableListOf<TomaMedicamento>()
                tomas.groupBy { it.idReceta }.forEach { (idReceta, listaTomas) ->
                    if (idReceta in recetasIds) {
                        val receta = recetasMap[idReceta]!!
                        val maxDosis = obtenerTotalTomasEsperadas(receta)
                        tomasValidas.addAll(listaTomas.sortedBy { it.horaProgramada }.take(maxDosis))
                    }
                }

                _tomasDelDia.postValue(tomasValidas)

                val timeSdf = SimpleDateFormat("hh:mm a", Locale.US)
                val pendientes = tomasValidas.filter { it.estado == "PENDIENTE" }.sortedBy { 
                    try { timeSdf.parse(it.horaProgramada.uppercase())?.time } catch(e: Exception) { 0L }
                }

                _conteoNotificaciones.postValue(pendientes.size)
                _proximaToma.postValue(pendientes.firstOrNull())

                val tomados = tomasValidas.count { it.estado == "TOMADA" }
                val total = tomasValidas.size
                _progresoMedicamentos.postValue(Pair(tomados, total))
            }
        }
    }

    private fun aplicaParaFecha(receta: Receta, cal: Calendar): Boolean {
        val inicio = Calendar.getInstance().apply { 
            timeInMillis = receta.fechaCaptura
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val actual = cal.clone() as Calendar
        actual.set(Calendar.HOUR_OF_DAY, 0); actual.set(Calendar.MINUTE, 0); actual.set(Calendar.SECOND, 0); actual.set(Calendar.MILLISECOND, 0)
        if (actual.before(inicio)) return false
        
        val duracionDias = try { Regex("\\d+").find(receta.duracion)?.value?.toInt() ?: 7 } catch (e: Exception) { 7 }
        val fin = inicio.clone() as Calendar
        fin.add(Calendar.DAY_OF_YEAR, duracionDias)
        return actual.before(fin) || (actual.get(Calendar.YEAR) == fin.get(Calendar.YEAR) && actual.get(Calendar.DAY_OF_YEAR) == fin.get(Calendar.DAY_OF_YEAR))
    }

    private fun generarTomasDeUnDia(receta: Receta, fechaStr: String): List<TomaMedicamento> {
        val tomas = mutableListOf<TomaMedicamento>()
        val totalTomas = obtenerTotalTomasEsperadas(receta)
        val horasProgramadas = when (totalTomas) {
            1 -> listOf("08:00 AM")
            2 -> listOf("08:00 AM", "08:00 PM")
            3 -> listOf("08:00 AM", "02:00 PM", "08:00 PM")
            4 -> listOf("06:00 AM", "12:00 PM", "06:00 PM", "12:00 AM")
            else -> {
                val list = mutableListOf<String>()
                val intervalo = 24 / totalTomas
                for (i in 0 until totalTomas) {
                    val horaC = (8 + (i * intervalo)) % 24
                    val displayHour = if (horaC > 12) horaC - 12 else if (horaC == 0) 12 else horaC
                    val amPm = if (horaC >= 12) "PM" else "AM"
                    list.add(String.format(Locale.getDefault(), "%02d:00 %s", displayHour, amPm))
                }
                list
            }
        }
        horasProgramadas.forEachIndexed { index, horaStr ->
            val deterministicId = "${receta.id}_${fechaStr}_$index"
            tomas.add(TomaMedicamento(id = deterministicId, idUsuario = receta.userId, idReceta = receta.id, nombreMedicamento = receta.nombreMedicamento, fecha = fechaStr, horaProgramada = horaStr, estado = "PENDIENTE", dosis = receta.dosis))
        }
        return tomas
    }

    private fun cargarProximaCita(uid: String) {
        citasRepo.getCitasDesencriptadas(uid) { citas ->
            val ahora = System.currentTimeMillis()
            val proxima = citas?.filter { it.fechaCita >= ahora }?.minByOrNull { it.fechaCita }
            _proximaCita.postValue(proxima)
        }
    }

    fun calcularIMC(peso: Double, estaturaCm: Int): Pair<Double, String> {
        if (estaturaCm <= 0) return Pair(0.0, "N/A")
        val estaturaM = estaturaCm / 100.0
        val imc = peso / (estaturaM * estaturaM)
        val estado = when { imc < 18.5 -> "Bajo peso"; imc < 25.0 -> "Normal"; imc < 30.0 -> "Sobrepeso"; else -> "Obesidad" }
        return Pair(imc, estado)
    }

    fun filtrarContenido(consulta: String) {
        if (consulta.isEmpty()) { _estadoVisibilidad.value = HomeVisibilidadEstado(); _sugerencias.value = emptyList(); return }
        val q = consulta.lowercase().trim()
        val todosLosServicios = listOf(Sugerencia("Mis Recetas Médicas", R.drawable.archivo, 1), Sugerencia("Citas Médicas", R.mipmap.watch, 2), Sugerencia("Ver Progreso Semanal", android.R.drawable.ic_menu_sort_by_size, 3))
        val filtrados = todosLosServicios.filter { it.texto.lowercase().contains(q) }
        _sugerencias.value = filtrados
        _estadoVisibilidad.value = HomeVisibilidadEstado(recetasVisible = filtrados.any { it.idModulo == 1 } || q.isEmpty(), citasVisible = filtrados.any { it.idModulo == 2 } || q.isEmpty(), statsVisible = filtrados.any { it.idModulo == 3 } || q.isEmpty(), sinResultadosVisible = filtrados.isEmpty() && q.isNotEmpty())
    }

    fun iniciarFrases(apiKey: String) {
        viewModelScope.launch {
            val request = ChatRequest(messages = listOf(Message(role = "system", content = "Eres un asistente motivacional para una app de salud. Genera frases MUY CORTAS y directas."), Message(role = "user", content = "Dame una frase motivacional muy corta para hoy.")))
            while (isActive) {
                try {
                    val response = withContext(Dispatchers.IO) { openAIService.getCompletion("Bearer $apiKey", request) }
                    val fraseRecibida = response.choices.firstOrNull()?.message?.content?.trim()?.replace("\"", "")
                    if (!fraseRecibida.isNullOrEmpty()) _fraseIA.postValue(fraseRecibida)
                } catch (e: Exception) { _fraseIA.postValue("La salud es tu mayor riqueza.") }
                delay(10 * 60 * 1000)
            }
        }
    }

    fun resetClassification() { _classificationResult.value = null }
}
