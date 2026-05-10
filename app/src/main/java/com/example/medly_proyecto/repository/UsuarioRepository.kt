package com.example.medly_proyecto.repository

import com.example.medly_proyecto.model.PerfilImagenes
import com.example.medly_proyecto.model.Usuario
import com.google.firebase.firestore.FirebaseFirestore

class UsuarioRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getUsuario(uid: String, callback: (Usuario?) -> Unit) {
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                callback(document.toObject(Usuario::class.java))
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun getPerfilImagenes(uid: String, callback: (PerfilImagenes?) -> Unit) {
        db.collection("perfil_imagenes").document(uid).get()
            .addOnSuccessListener { document ->
                callback(document.toObject(PerfilImagenes::class.java))
            }
            .addOnFailureListener {
                callback(null)
            }
    }
}
