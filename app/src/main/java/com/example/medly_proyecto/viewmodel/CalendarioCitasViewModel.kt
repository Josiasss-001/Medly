package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.medly_proyecto.model.CitaMedica
import com.example.medly_proyecto.model.PerfilImagenes
import com.example.medly_proyecto.repository.CitasRepository
import com.example.medly_proyecto.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class CalendarioCitasViewModel : ViewModel() {
    private val repository = CitasRepository()
    private val usuarioRepo = UsuarioRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _citasDelMes = MutableLiveData<List<CitaMedica>>()
    val citasDelMes: LiveData<List<CitaMedica>> = _citasDelMes

    private val _citasDiaSeleccionado = MutableLiveData<List<CitaMedica>>()
    val citasDiaSeleccionado: LiveData<List<CitaMedica>> = _citasDiaSeleccionado

    private val _perfilImagenes = MutableLiveData<PerfilImagenes?>()
    val perfilImagenes: LiveData<PerfilImagenes?> = _perfilImagenes

    private var todasLasCitas: List<CitaMedica> = emptyList()

    init {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            cargarTodasLasCitas(userId)
            cargarPerfilImagenes(userId)
        }
    }

    private fun cargarTodasLasCitas(userId: String) {
        repository.getCitasDesencriptadas(userId) { citas ->
            if (citas != null) {
                todasLasCitas = citas
                _citasDelMes.postValue(citas)
                // Por defecto, cargar las de hoy
                seleccionarFecha(Calendar.getInstance())
            }
        }
    }

    private fun cargarPerfilImagenes(uid: String) {
        usuarioRepo.getPerfilImagenes(uid) { imagenes ->
            _perfilImagenes.postValue(imagenes)
        }
    }

    fun seleccionarFecha(fecha: Calendar) {
        val citasDelDia = todasLasCitas.filter { cita ->
            val calCita = Calendar.getInstance().apply { timeInMillis = cita.fechaCita }
            esMismoDia(calCita, fecha)
        }
        _citasDiaSeleccionado.postValue(citasDelDia)
    }

    private fun esMismoDia(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun tieneCitaEnFecha(fecha: Calendar): Boolean {
        return todasLasCitas.any { cita ->
            val calCita = Calendar.getInstance().apply { timeInMillis = cita.fechaCita }
            esMismoDia(calCita, fecha)
        }
    }
}
