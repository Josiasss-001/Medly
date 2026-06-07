package com.example.medly_proyecto.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class AuthRepository {

    private val firebaseAuth = FirebaseAuth.getInstance()

    fun iniciarSesion(
        email: String,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña inválidos"
                        is FirebaseAuthInvalidUserException -> "El usuario no existe"
                        else -> "Error al iniciar sesión"
                    }
                    callback(false, errorMessage)
                }
            }
    }

    fun registrar(
        email: String,
        password: String,
        callback: (Boolean, String?) -> Unit
    ) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    fun cambiarPassword(newPassword: String, callback: (Boolean, String?) -> Unit) {
        val user = firebaseAuth.currentUser
        user?.updatePassword(newPassword)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message)
                }
            } ?: callback(false, "Usuario no autenticado")
    }

    fun reautenticar(email: String, pass: String, callback: (Boolean, String?) -> Unit) {
        val credential = EmailAuthProvider.getCredential(email, pass)
        reautenticarConCredencial(credential, callback)
    }

    fun reautenticarConCredencial(credential: AuthCredential, callback: (Boolean, String?) -> Unit) {
        val user = firebaseAuth.currentUser
        user?.reauthenticate(credential)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true, null)
            } else {
                callback(false, task.exception?.message)
            }
        } ?: callback(false, "Usuario no encontrado")
    }

    fun enviarCorreoRecuperacion(email: String, callback: (Boolean, String?) -> Unit) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }
}
