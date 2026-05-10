package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.medly_proyecto.repository.AuthRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableLiveData<Pair<Boolean, String?>>()
    val authState: LiveData<Pair<Boolean, String?>> get() = _authState

    private val _userVerificationState = MutableLiveData<UserType>()
    val userVerificationState: LiveData<UserType> get() = _userVerificationState

    enum class UserType {
        EXISTING, NEW, ERROR
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
        repository.iniciarSesion(email, password) { success, error ->
            _authState.value = Pair(success, error)
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
                    _userVerificationState.value = UserType.EXISTING
                } else {
                    _userVerificationState.value = UserType.NEW
                }
            }
            .addOnFailureListener {
                _userVerificationState.value = UserType.ERROR
            }
    }
}
