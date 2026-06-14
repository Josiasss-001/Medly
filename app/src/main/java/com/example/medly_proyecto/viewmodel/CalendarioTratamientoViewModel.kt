package com.example.medly_proyecto.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.medly_proyecto.model.Receta
import com.example.medly_proyecto.model.TomaMedicamento
import com.example.medly_proyecto.notification.ReminderScheduler
import com.example.medly_proyecto.repository.RecetasRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

data class EventoTratamiento(
    val id: String = "", 
    val nombre: String = "",
    val hora: String = "",
    val completado: Boolean = false,
    val idReceta: String = "",
    val dosis: String = ""
)

data class MedicamentoGrupo(
    val idReceta: String,
    val nombreMedicamento: String,
    val dosis: String,
    val frecuencia: String,
    val eventos: List<EventoTratamiento>,
    var estaExpandido: Boolean = true
)

data class ResumenTratamiento(
    val tomadas: Int = 0,
    val pendientes: Int = 0,
    val proximaDosis: String = "--",
    val adherencia: Int = 0
)

class CalendarioTratamientoViewModel(application: Application) : AndroidViewModel(application) {
    private val repositorio = RecetasRepository()
    private val auth = FirebaseAuth.getInstance()
    private val scheduler = ReminderScheduler(application)
    
    private val _grupos = MutableLiveData<List<MedicamentoGrupo>>()
    val grupos: LiveData<List<MedicamentoGrupo>> = _grupos

    private val _resumen = MutableLiveData<ResumenTratamiento>()
    val resumen: LiveData<ResumenTratamiento> = _resumen

    private val _eventos = MutableLiveData<List<EventoTratamiento>>()
    val eventos: LiveData<List<EventoTratamiento>> = _eventos

    private var todasLasRecetas: List<Receta> = emptyList()
    private var fechaActual: Calendar = Calendar.getInstance()
    private var estaCargando = false

    init {
        cargarRecetasBase()
    }

    private fun cargarRecetasBase() {
        val userId = auth.currentUser?.uid ?: return
        repositorio.getRecetasDesencriptadas(userId) { recetas ->
            if (recetas != null) {
                todasLasRecetas = recetas
                cargarTomasDelDia()
            }
        }
    }

    fun seleccionarFecha(anio: Int, mes: Int, dia: Int) {
        val nuevaFecha = Calendar.getInstance().apply {
            set(anio, mes, dia, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        fechaActual = nuevaFecha
        cargarTomasDelDia()
    }

    private fun cargarTomasDelDia() {
        val userId = auth.currentUser?.uid ?: return
        if (estaCargando) return
        estaCargando = true

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fechaString = sdf.format(fechaActual.time)

        repositorio.getTomasPorFecha(userId, fechaString) { tomasExistentes ->
            val tomas = tomasExistentes ?: emptyList()
            
            // Usamos el id de la receta para identificar tomas únicas
            val tomasExistentesIds = tomas.map { it.idReceta }.toSet()
            val esHoy = esMismoDia(fechaActual, Calendar.getInstance())
            
            val nuevasTomas = mutableListOf<TomaMedicamento>()
            
            if (esHoy) {
                todasLasRecetas.forEach { receta ->
                    if (receta.tratamientoIniciado && receta.id !in tomasExistentesIds && applicaParaFecha(receta, receta.fechaCaptura, fechaActual)) {
                        nuevasTomas.addAll(generarTomasDeUnDia(receta, fechaString))
                    }
                }
            }

            if (nuevasTomas.isNotEmpty()) {
                repositorio.guardarTomasMasivas(nuevasTomas) { exito ->
                    estaCargando = false
                    if (exito) {
                        nuevasTomas.forEach { scheduler.programarAlarmasToma(it) }
                        cargarTomasDelDia()
                    }
                }
            } else {
                agruparYActualizar(tomas)
                estaCargando = false
            }
        }
    }

    private fun agruparYActualizar(tomas: List<TomaMedicamento>) {
        val recetasMap = todasLasRecetas.associateBy { it.id }
        
        val gruposMapeados = tomas.groupBy { it.idReceta }.mapNotNull { (idReceta, listaTomas) ->
            val receta = recetasMap[idReceta]
            
            val eventos = listaTomas.map { toma ->
                EventoTratamiento(
                    id = toma.id,
                    nombre = toma.nombreMedicamento,
                    hora = toma.horaProgramada,
                    completado = toma.estado == "TOMADA",
                    idReceta = toma.idReceta,
                    dosis = toma.dosis
                )
            }.sortedBy { it.hora }
            
            MedicamentoGrupo(
                idReceta = idReceta,
                nombreMedicamento = receta?.nombreMedicamento ?: listaTomas.first().nombreMedicamento,
                dosis = receta?.dosis ?: listaTomas.first().dosis,
                frecuencia = receta?.frecuencia ?: "Cada cierto tiempo",
                eventos = eventos
            )
        }

        _grupos.postValue(gruposMapeados)
        _eventos.postValue(gruposMapeados.flatMap { it.eventos })
        actualizarResumen(tomas)
    }

    private fun actualizarResumen(tomas: List<TomaMedicamento>) {
        if (tomas.isEmpty()) {
            _resumen.postValue(ResumenTratamiento())
            return
        }

        val tomadas = tomas.count { it.estado == "TOMADA" }
        val pendientes = tomas.count { it.estado == "PENDIENTE" }
        
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        val ahora = Calendar.getInstance()
        val proxima = tomas.filter { it.estado == "PENDIENTE" }
            .mapNotNull { toma ->
                try {
                    val date = sdf.parse(toma.horaProgramada)
                    val cal = Calendar.getInstance().apply { 
                        time = date!!
                        set(ahora.get(Calendar.YEAR), ahora.get(Calendar.MONTH), ahora.get(Calendar.DAY_OF_MONTH))
                    }
                    if (cal.after(ahora)) cal else null
                } catch (e: Exception) { null }
            }.minByOrNull { it.timeInMillis }

        val proximaStr = proxima?.let { sdf.format(it.time) } ?: "--"
        val adherencia = (tomadas * 100) / tomas.size

        _resumen.postValue(ResumenTratamiento(tomadas, pendientes, proximaStr, adherencia))
    }

    private fun esMismoDia(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun applicaParaFecha(receta: Receta, fechaInicioReceta: Long, cal: Calendar): Boolean {
        val inicio = Calendar.getInstance().apply { 
            timeInMillis = fechaInicioReceta
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

    fun marcarToma(tomaId: String, completado: Boolean) {
        val nuevoEstado = if (completado) "TOMADA" else "PENDIENTE"
        val fechaCompletada = if (completado) System.currentTimeMillis() else null
        
        repositorio.actualizarEstadoToma(tomaId, nuevoEstado, fechaCompletada) { exito ->
            if (exito) {
                if (completado) scheduler.cancelarAlarmasToma(tomaId)
                cargarTomasDelDia()
            }
        }
    }
}
