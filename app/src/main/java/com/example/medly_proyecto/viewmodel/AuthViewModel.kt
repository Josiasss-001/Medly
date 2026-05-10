package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.medly_proyecto.model.Usuario
import com.example.medly_proyecto.repository.AuthRepository
import com.example.medly_proyecto.repository.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthCredential
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val usuarioRepository = UsuarioRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableLiveData<Pair<Boolean, String?>>()
    val authState: LiveData<Pair<Boolean, String?>> get() = _authState

    private val _userVerificationState = MutableLiveData<UserType>()
    val userVerificationState: LiveData<UserType> get() = _userVerificationState

    enum class UserType {
        EXISTING, INCOMPLETE, NEW, ERROR
    }

    fun verificarSesionActual() {
        if (auth.currentUser != null) {
            verificarUsuario()
        }
    }

    fun validarEIniciarSesion(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = Pair(false, "Por favor completa todos los campos")
            return
        }
        authRepository.iniciarSesion(email, password) { success, error ->
            _authState.value = Pair(success, error)
        }
    }

    fun validarYRegistrar(email: String, pass: String, confirmPass: String) {
        if (email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            _authState.value = Pair(false, "Todos los campos son obligatorios")
            return
        }
        if (pass != confirmPass) {
            _authState.value = Pair(false, "Las contraseñas no coinciden")
            return
        }
        if (pass.length < 6) {
            _authState.value = Pair(false, "La contraseña debe tener al menos 6 caracteres")
            return
        }

        authRepository.registrar(email, pass) { success, error ->
            if (success) {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val nuevoUsuario = Usuario(
                        uid = uid,
                        correo = email,
                        nombres = "", // Se completarán en el recolector
                        apellidos = "",
                        perfilCompleto = false
                    )
                    
                    usuarioRepository.guardarUsuario(nuevoUsuario) { saved, saveError ->
                        if (saved) {
                            auth.signOut()
                            _authState.value = Pair(true, "Registro exitoso. Por favor inicia sesión.")
                        } else {
                            _authState.value = Pair(false, "Error al guardar perfil: $saveError")
                        }
                    }
                } else {
                    _authState.value = Pair(false, "Error al obtener UID")
                }
            } else {
                _authState.value = Pair(false, error)
            }
        }
    }

    fun autenticarConGoogle(credential: AuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = Pair(true, null)
                } else {
                    _authState.value = Pair(false, task.exception?.message)
                }
            }
    }

    fun verificarUsuario() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val usuario = document.toObject(Usuario::class.java)
                    if (usuario?.perfilCompleto == true) {
                        _userVerificationState.value = UserType.EXISTING
                    } else {
                        _userVerificationState.value = UserType.INCOMPLETE
                    }
                } else {
                    _userVerificationState.value = UserType.NEW
                }
            }
            .addOnFailureListener {
                _userVerificationState.value = UserType.ERROR
            }
    }
}
