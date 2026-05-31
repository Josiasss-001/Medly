package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.medly_proyecto.model.Receta
import com.example.medly_proyecto.repository.RecetasRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.*

data class EventoTratamiento(
    val id: String = "",
    val nombre: String = "",
    val hora: String = "",
    val tiempoRestante: String = "",
    val completado: Boolean = false
)

class CalendarioTratamientoViewModel : ViewModel() {
    private val repository = RecetasRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _eventos = MutableLiveData<List<EventoTratamiento>>()
    val eventos: LiveData<List<EventoTratamiento>> = _eventos

    private var todasLasRecetas: List<Receta> = emptyList()
    private var fechaSeleccionada: Calendar = Calendar.getInstance()
    private var recetaIdEspecifica: String? = null

    init {
        cargarRecetas()
    }

    private fun cargarRecetas() {
        val userId = auth.currentUser?.uid ?: return
        repository.getRecetasDesencriptadas(userId) { recetas ->
            if (recetas != null) {
                todasLasRecetas = recetas
                filtrarEventosParaFecha(fechaSeleccionada)
            }
        }
    }

    fun setRecetaIdEspecifica(id: String?) {
        recetaIdEspecifica = id
        filtrarEventosParaFecha(fechaSeleccionada)
    }

    fun seleccionarFecha(year: Int, month: Int, day: Int) {
        fechaSeleccionada.set(year, month, day)
        filtrarEventosParaFecha(fechaSeleccionada)
    }

    private fun filtrarEventosParaFecha(cal: Calendar) {
        val nuevosEventos = mutableListOf<EventoTratamiento>()
        
        val recetasAProcesar = if (recetaIdEspecifica != null) {
            todasLasRecetas.filter { it.id == recetaIdEspecifica }
        } else {
            todasLasRecetas
        }
        
        for (receta in recetasAProcesar) {
            if (aplicaParaFecha(receta, cal)) {
                val eventosDelDia = generarEventosParaReceta(receta)
                nuevosEventos.addAll(eventosDelDia)
            }
        }
        
        _eventos.postValue(nuevosEventos.sortedBy { it.hora })
    }

    private fun aplicaParaFecha(receta: Receta, cal: Calendar): Boolean {
        val fechaInicio = Calendar.getInstance().apply { timeInMillis = receta.fechaCaptura }
        val inicio = fechaInicio.clone() as Calendar
        inicio.set(Calendar.HOUR_OF_DAY, 0)
        inicio.set(Calendar.MINUTE, 0)
        inicio.set(Calendar.SECOND, 0)
        inicio.set(Calendar.MILLISECOND, 0)
        
        val actual = cal.clone() as Calendar
        actual.set(Calendar.HOUR_OF_DAY, 0)
        actual.set(Calendar.MINUTE, 0)
        actual.set(Calendar.SECOND, 0)
        actual.set(Calendar.MILLISECOND, 0)

        if (actual.before(inicio)) return false

        val duracionDias = try {
            receta.duracion.filter { it.isDigit() }.toInt()
        } catch (e: Exception) {
            30
        }

        val fechaFin = inicio.clone() as Calendar
        fechaFin.add(Calendar.DAY_OF_YEAR, duracionDias)

        return actual.before(fechaFin) || actual == fechaFin
    }

    private fun generarEventosParaReceta(receta: Receta): List<EventoTratamiento> {
        val eventos = mutableListOf<EventoTratamiento>()
        
        val veces = try {
            receta.vecesAlDia.filter { it.isDigit() }.toInt().takeIf { it > 0 } ?: 1
        } catch (e: Exception) {
            1
        }

        // Mapeo de periodos según la cantidad de veces al día
        val etiquetas = when (veces) {
            1 -> listOf("Mañana")
            2 -> listOf("Mañana", "Noche")
            3 -> listOf("Mañana", "Tarde", "Noche")
            4 -> listOf("Mañana", "Mediodía", "Tarde", "Noche")
            else -> List(veces) { "Toma ${it + 1}" }
        }

        val intervaloHoras = 24 / veces
        
        for (i in 0 until veces) {
            val etiqueta = etiquetas.getOrElse(i) { "Toma ${i + 1}" }
            val horaC = (8 + (i * intervaloHoras)) % 24
            val displayHour = if (horaC > 12) horaC - 12 else if (horaC == 0) 12 else horaC
            val amPm = if (horaC >= 12) "PM" else "AM"
            
            val horaStr = String.format(Locale.getDefault(), "%02d:00 %s", displayHour, amPm)
            
            eventos.add(EventoTratamiento(
                id = "${receta.id}_$i",
                nombre = receta.nombreMedicamento,
                hora = "Toma: $etiqueta ($horaStr)",
                tiempoRestante = "Estado: Pendiente"
            ))
        }
        
        return eventos
    }
}
