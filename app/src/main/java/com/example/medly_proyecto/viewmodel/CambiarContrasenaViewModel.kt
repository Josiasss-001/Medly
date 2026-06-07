package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.medly_proyecto.repository.AuthRepository
import com.google.firebase.auth.AuthCredential

class CambiarContrasenaViewModel : ViewModel() {

    private val repositorio = AuthRepository()

    private val _resultadoCambio = MutableLiveData<Pair<Boolean, String?>>()
    val resultadoCambio: LiveData<Pair<Boolean, String?>> = _resultadoCambio

    private val _resultadoReautenticacion = MutableLiveData<Pair<Boolean, String?>>()
    val resultadoReautenticacion: LiveData<Pair<Boolean, String?>> = _resultadoReautenticacion

    private val _estaCargando = MutableLiveData<Boolean>()
    val estaCargando: LiveData<Boolean> = _estaCargando

    fun reautenticarUsuario(correo: String, contrasenaActual: String) {
        if (correo.isEmpty() || contrasenaActual.isEmpty()) {
            _resultadoReautenticacion.value = Pair(false, "Por favor completa todos los campos")
            return
        }
        _estaCargando.value = true
        repositorio.reautenticar(correo, contrasenaActual) { exito, error ->
            _estaCargando.value = false
            _resultadoReautenticacion.value = Pair(exito, error)
        }
    }

    fun reautenticarConGoogle(credencial: AuthCredential) {
        _estaCargando.value = true
        repositorio.reautenticarConCredencial(credencial) { exito, error ->
            _estaCargando.value = false
            _resultadoReautenticacion.value = Pair(exito, error)
        }
    }

    fun actualizarContrasena(nuevaContrasena: String, confirmarContrasena: String) {
        if (nuevaContrasena.isEmpty()) {
            _resultadoCambio.value = Pair(false, "La contraseña no puede estar vacía")
            return
        }
        if (nuevaContrasena.length < 6) {
            _resultadoCambio.value = Pair(false, "La contraseña debe tener al menos 6 caracteres")
            return
        }
        if (nuevaContrasena != confirmarContrasena) {
            _resultadoCambio.value = Pair(false, "Las contraseñas no coinciden")
            return
        }

        _estaCargando.value = true
        repositorio.cambiarPassword(nuevaContrasena) { exito, error ->
            _estaCargando.value = false
            _resultadoCambio.value = Pair(exito, error)
        }
    }
}
