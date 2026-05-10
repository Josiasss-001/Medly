package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.medly_proyecto.repository.AuthRepository

class RegisterViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _registerState = MutableLiveData<Pair<Boolean, String?>>()
    val registerState: LiveData<Pair<Boolean, String?>> = _registerState

    fun validarYRegistrar(email: String, pass: String, confirmPass: String) {
        if (email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            _registerState.value = Pair(false, "Todos los campos son obligatorios")
            return
        }
        if (pass != confirmPass) {
            _registerState.value = Pair(false, "Las contraseñas no coinciden")
            return
        }
        if (pass.length < 6) {
            _registerState.value = Pair(false, "La contraseña debe tener al menos 6 caracteres")
            return
        }

        repository.registrar(email, pass) { success, error ->
            _registerState.value = Pair(success, error)
        }
    }
}
