package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.medly_proyecto.model.Receta
import com.example.medly_proyecto.model.TomaMedicamento
import com.example.medly_proyecto.repository.RecetasRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

data class EventoTratamiento(
    val id: String = "", 
    val nombre: String = "",
    val hora: String = "",
    val completado: Boolean = false,
    val idReceta: String = ""
)

class CalendarioTratamientoViewModel : ViewModel() {
    private val repositorio = RecetasRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _eventos = MutableLiveData<List<EventoTratamiento>>()
    val eventos: LiveData<List<EventoTratamiento>> = _eventos

    private var todasLasRecetas: List<Receta> = emptyList()
    private var fechaActual: Calendar = Calendar.getInstance()
    private var recetaIdEspecifica: String? = null
    private var estaCargando = false

    init {
        cargarRecetasBase()
    }

    private fun cargarRecetasBase() {
        val userId = auth.currentUser?.uid ?: return
        repositorio.getRecetasDesencriptadas(userId) { recetas ->
            if (recetas != null) {
                todasLasRecetas = recetas
                if (_eventos.value == null) cargarTomasDelDia()
            }
        }
    }

    fun setRecetaIdEspecifica(id: String?) {
        recetaIdEspecifica = id
        cargarTomasDelDia()
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
            
            val recetasAProcesar = if (recetaIdEspecifica != null) {
                todasLasRecetas.filter { it.id == recetaIdEspecifica }
            } else {
                todasLasRecetas
            }

            val idsRecetasConTomas = tomas.map { it.idReceta }.toSet()
            
            // SOLO buscamos recetas faltantes si es el día de HOY
            val esHoy = esMismoDia(fechaActual, Calendar.getInstance())
            
            val recetasFaltantes = if (esHoy) {
                recetasAProcesar.filter { receta ->
                    applicaParaFecha(receta, fechaActual) && receta.id !in idsRecetasConTomas
                }
            } else {
                emptyList()
            }

            if (recetasFaltantes.isNotEmpty()) {
                val nuevasTomas = mutableListOf<TomaMedicamento>()
                recetasFaltantes.forEach { receta ->
                    nuevasTomas.addAll(generarTomasDeUnDia(receta, fechaString))
                }
                
                repositorio.guardarTomasMasivas(nuevasTomas) { exito ->
                    estaCargando = false
                    if (exito) cargarTomasDelDia()
                }
            } else {
                val eventosMapeados = tomas
                    .filter { recetaIdEspecifica == null || it.idReceta == recetaIdEspecifica }
                    .map { toma ->
                        EventoTratamiento(
                            id = toma.id,
                            nombre = toma.nombreMedicamento,
                            hora = toma.horaProgramada,
                            completado = toma.estado == "TOMADA",
                            idReceta = toma.idReceta
                        )
                    }.sortedBy { it.hora }
                
                _eventos.postValue(eventosMapeados)
                estaCargando = false
            }
        }
    }

    private fun esMismoDia(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun applicaParaFecha(receta: Receta, cal: Calendar): Boolean {
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

    fun marcarToma(evento: EventoTratamiento, completado: Boolean) {
        val nuevoEstado = if (completado) "TOMADA" else "PENDIENTE"
        val fechaCompletada = if (completado) System.currentTimeMillis() else null
        
        repositorio.actualizarEstadoToma(evento.id, nuevoEstado, fechaCompletada) { exito ->
            if (exito) {
                val listaActual = _eventos.value?.map {
                    if (it.id == evento.id) it.copy(completado = completado) else it
                }
                _eventos.postValue(listaActual)
            }
        }
    }
}
