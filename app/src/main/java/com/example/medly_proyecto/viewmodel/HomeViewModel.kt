package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.ChatRequest
import com.example.medly_proyecto.model.CitaMedica
import com.example.medly_proyecto.model.DatosMedicos
import com.example.medly_proyecto.model.Message
import com.example.medly_proyecto.model.PerfilImagenes
import com.example.medly_proyecto.model.Receta
import com.example.medly_proyecto.model.Sugerencia
import com.example.medly_proyecto.model.TomaMedicamento
import com.example.medly_proyecto.model.Usuario
import com.example.medly_proyecto.repository.CitasRepository
import com.example.medly_proyecto.repository.OpenAIService
import com.example.medly_proyecto.repository.RecetasRepository
import com.example.medly_proyecto.repository.UsuarioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

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
    private val openAIService = OpenAIService.create()

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

    private val _proximaToma = MutableLiveData<TomaMedicamento?>()
    val proximaToma: LiveData<TomaMedicamento?> = _proximaToma

    private val _conteoNotificaciones = MutableLiveData<Int>(0)
    val conteoNotificaciones: LiveData<Int> = _conteoNotificaciones

    private val _proximaCita = MutableLiveData<CitaMedica?>()
    val proximaCita: LiveData<CitaMedica?> = _proximaCita

    private val _progresoMedicamentos = MutableLiveData<Pair<Int, Int>>() // Tomados, Total
    val progresoMedicamentos: LiveData<Pair<Int, Int>> = _progresoMedicamentos

    init {
        _estadoVisibilidad.value = HomeVisibilidadEstado()
        _sugerencias.value = emptyList()
    }

    fun cargarDatos(uid: String) {
        usuarioRepo.getUsuario(uid) { user ->
            _usuario.value = user
        }
        usuarioRepo.getPerfilImagenes(uid) { imagenes ->
            _perfilImagenes.value = imagenes
        }
        usuarioRepo.getDatosMedicos(uid) { datos ->
            _datosMedicos.value = datos
        }
        asegurarTomasYActualizar(uid)
        cargarProximaCita(uid)
    }

    private fun asegurarTomasYActualizar(uid: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hoyStr = sdf.format(Date())
        
        recetasRepo.getRecetasDesencriptadas(uid) { recetas ->
            val listaRecetas = recetas ?: emptyList()
            recetasRepo.getTomasPorFecha(uid, hoyStr) { tomasExistentes ->
                val tomas = tomasExistentes ?: emptyList()
                
                val idsRecetasConTomas = tomas.map { it.idReceta }.toSet()
                val hoyCal = Calendar.getInstance()
                
                val recetasFaltantes = listaRecetas.filter { receta ->
                    aplicaParaFecha(receta, hoyCal) && receta.id !in idsRecetasConTomas
                }

                if (recetasFaltantes.isNotEmpty()) {
                    val nuevasTomas = mutableListOf<TomaMedicamento>()
                    recetasFaltantes.forEach { receta ->
                        nuevasTomas.addAll(generarTomasDeUnDia(receta, hoyStr))
                    }
                    recetasRepo.guardarTomasMasivas(nuevasTomas) { exito ->
                        actualizarProximaTomaYProgreso(uid, hoyStr)
                    }
                } else {
                    actualizarProximaTomaYProgreso(uid, hoyStr)
                }
            }
        }
    }

    private fun actualizarProximaTomaYProgreso(uid: String, hoyStr: String) {
        recetasRepo.getTomasPorFecha(uid, hoyStr) { tomas ->
            val timeSdf = SimpleDateFormat("hh:mm a", Locale.US)
            val pendientes = tomas?.filter { it.estado == "PENDIENTE" }?.sortedBy { 
                try { timeSdf.parse(it.horaProgramada.uppercase())?.time } catch(e: Exception) { 0L }
            }
            _conteoNotificaciones.postValue(pendientes?.size ?: 0)
            _proximaToma.postValue(pendientes?.firstOrNull())

            val tomados = tomas?.count { it.estado == "TOMADA" } ?: 0
            val total = tomas?.size ?: 0
            _progresoMedicamentos.postValue(Pair(tomados, total))
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

        val duracionDias = try { 
            receta.duracion.filter { it.isDigit() }.toInt().takeIf { it > 0 } ?: 7 
        } catch (e: Exception) { 7 }
        
        val fin = inicio.clone() as Calendar
        fin.add(Calendar.DAY_OF_YEAR, duracionDias)

        return actual.before(fin) || (actual.get(Calendar.YEAR) == fin.get(Calendar.YEAR) && actual.get(Calendar.DAY_OF_YEAR) == fin.get(Calendar.DAY_OF_YEAR))
    }

    private fun generarTomasDeUnDia(receta: Receta, fechaStr: String): List<TomaMedicamento> {
        val tomas = mutableListOf<TomaMedicamento>()
        val vecesNum = try { receta.vecesAlDia.filter { it.isDigit() }.toInt() } catch (e: Exception) { 0 }
        val frecuenciaNum = try { receta.frecuencia.filter { it.isDigit() }.toInt() } catch (e: Exception) { 0 }
        
        var totalTomas = 1
        if (vecesNum > 0) totalTomas = vecesNum
        else if (frecuenciaNum >= 4) totalTomas = 24 / frecuenciaNum
        else if (frecuenciaNum > 0) totalTomas = frecuenciaNum

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
            tomas.add(TomaMedicamento(
                id = deterministicId,
                idUsuario = receta.userId,
                idReceta = receta.id,
                nombreMedicamento = receta.nombreMedicamento,
                fecha = fechaStr,
                horaProgramada = horaStr,
                estado = "PENDIENTE",
                dosis = receta.dosis
            ))
        }
        return tomas
    }

    private fun cargarProximaCita(uid: String) {
        citasRepo.getCitasDesencriptadas(uid) { citas ->
            val ahora = System.currentTimeMillis()
            val proxima = citas?.filter { it.fechaCita >= ahora }
                ?.minByOrNull { it.fechaCita }
            _proximaCita.postValue(proxima)
        }
    }

    fun calcularIMC(peso: Double, estaturaCm: Int): Pair<Double, String> {
        if (estaturaCm <= 0) return Pair(0.0, "N/A")
        val estaturaM = estaturaCm / 100.0
        val imc = peso / (estaturaM * estaturaM)
        val estado = when {
            imc < 18.5 -> "Bajo peso"
            imc < 25.0 -> "Normal"
            imc < 30.0 -> "Sobrepeso"
            else -> "Obesidad"
        }
        return Pair(imc, estado)
    }

    fun filtrarContenido(consulta: String) {
        if (consulta.isEmpty()) {
            _estadoVisibilidad.value = HomeVisibilidadEstado()
            _sugerencias.value = emptyList()
            return
        }

        val q = consulta.lowercase().trim()

        val todosLosServicios = listOf(
            Sugerencia("Mis Recetas Médicas", R.drawable.archivo, 1),
            Sugerencia("Citas Médicas", R.mipmap.watch, 2),
            Sugerencia("Ver Progreso Semanal", android.R.drawable.ic_menu_sort_by_size, 3),
            Sugerencia("Inspiración del día", android.R.drawable.ic_menu_info_details, 4)
        )

        val filtrados = todosLosServicios.filter { it.texto.lowercase().contains(q) }
        _sugerencias.value = filtrados

        val coincideRecetas = filtrados.any { it.idModulo == 1 }
        val coincideCitas = filtrados.any { it.idModulo == 2 }
        val coincideStats = filtrados.any { it.idModulo == 3 }
        val coincideQuote = filtrados.any { it.idModulo == 4 }

        val algunResultado = filtrados.isNotEmpty()

        _estadoVisibilidad.value = HomeVisibilidadEstado(
            recetasVisible = coincideRecetas || q.isEmpty(),
            citasVisible = coincideCitas || q.isEmpty(),
            statsVisible = coincideStats || q.isEmpty(),
            quoteVisible = coincideQuote || q.isEmpty(),
            sinResultadosVisible = !algunResultado && q.isNotEmpty()
        )
    }

    fun iniciarFrases(apiKey: String) {
        viewModelScope.launch {
            val request = ChatRequest(
                messages = listOf(
                    Message(role = "system", content = "Eres un asistente motivacional para una app de salud. Genera frases MUY CORTAS y directas, de máximo 5 o 6 palabras."),
                    Message(role = "user", content = "Dame una frase motivacional muy corta para hoy.")
                )
            )
            while (isActive) {
                try {
                    val response = withContext(Dispatchers.IO) {
                        openAIService.getCompletion("Bearer $apiKey", request)
                    }
                    val fraseRecibida = response.choices.firstOrNull()?.message?.content?.trim()?.replace("\"", "")
                    if (!fraseRecibida.isNullOrEmpty()) {
                        _fraseIA.postValue(fraseRecibida)
                    }
                } catch (e: Exception) {
                    _fraseIA.postValue("La salud es tu mayor riqueza.")
                }
                delay(10 * 60 * 1000)
            }
        }
    }
}
