package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.medly_proyecto.model.DatosMedicos
import com.example.medly_proyecto.model.Usuario
import com.example.medly_proyecto.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth

class PerfilViewModel : ViewModel() {
    private val usuarioRepository = UsuarioRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val _datosMedicos = MutableLiveData<DatosMedicos?>()
    val datosMedicos: LiveData<DatosMedicos?> = _datosMedicos

    private val _loggedOut = MutableLiveData<Boolean>()
    val loggedOut: LiveData<Boolean> = _loggedOut

    private val _deleteStatus = MutableLiveData<Pair<Boolean, String?>>()
    val deleteStatus: LiveData<Pair<Boolean, String?>> = _deleteStatus

    fun loadProfile(userId: String) {
        usuarioRepository.getUsuario(userId) { user ->
            _usuario.value = user
        }
        usuarioRepository.getDatosMedicos(userId) { medicalData ->
            _datosMedicos.value = medicalData
        }
    }

    fun signOut() {
        auth.signOut()
        _loggedOut.value = true
    }

    fun eliminarCuenta() {
        usuarioRepository.eliminarCuenta { success, error ->
            _deleteStatus.value = Pair(success, error)
        }
    }
}
