package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.medly_proyecto.model.CitaMedica
import com.example.medly_proyecto.model.PerfilImagenes
import com.example.medly_proyecto.model.Usuario
import com.example.medly_proyecto.repository.CitasRepository
import com.example.medly_proyecto.repository.UsuarioRepository

class CitasViewModel : ViewModel() {
    private val repository = CitasRepository()
    private val usuarioRepo = UsuarioRepository()

    private val _citas = MutableLiveData<List<CitaMedica>?>()
    val citas: LiveData<List<CitaMedica>?> = _citas

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val _perfilImagenes = MutableLiveData<PerfilImagenes?>()
    val perfilImagenes: LiveData<PerfilImagenes?> = _perfilImagenes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _deleteStatus = MutableLiveData<Pair<Boolean, String?>>()
    val deleteStatus: LiveData<Pair<Boolean, String?>> = _deleteStatus

    fun cargarCitas(userId: String) {
        _isLoading.value = true
        repository.getCitasDesencriptadas(userId) { lista ->
            _citas.postValue(lista)
            _isLoading.postValue(false)
        }
        
        // Cargar también datos de perfil para el Drawer
        usuarioRepo.getUsuario(userId) { user ->
            _usuario.postValue(user)
        }
        usuarioRepo.getPerfilImagenes(userId) { imagenes ->
            _perfilImagenes.postValue(imagenes)
        }
    }

    fun eliminarCita(citaId: String, userId: String) {
        _isLoading.value = true
        repository.eliminarCita(citaId) { success, message ->
            _deleteStatus.postValue(Pair(success, message))
            if (success) {
                cargarCitas(userId)
            } else {
                _isLoading.postValue(false)
            }
        }
    }
}
