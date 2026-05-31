package com.example.medly_proyecto.repository

import com.example.medly_proyecto.model.DatosMedicos
import com.example.medly_proyecto.model.PerfilImagenes
import com.example.medly_proyecto.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class UsuarioRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun guardarUsuario(usuario: Usuario, callback: (Boolean, String?) -> Unit) {
        db.collection("usuarios").document(usuario.uid).set(usuario)
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }

    fun actualizarCamposUsuario(uid: String, updates: Map<String, Any>, callback: (Boolean) -> Unit) {
        db.collection("usuarios").document(uid).update(updates)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun actualizarPerfilImagenes(uid: String, updates: Map<String, Any>, callback: (Boolean) -> Unit) {
        db.collection("perfil_imagenes").document(uid).set(updates, SetOptions.merge())
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun actualizarPerfilCompleto(
        usuario: Usuario,
        datosMedicos: DatosMedicos,
        callback: (Boolean, String?) -> Unit
    ) {
        val batch = db.batch()

        val userRef = db.collection("usuarios").document(usuario.uid)
        val medicalRef = db.collection("datos_medicos").document(usuario.uid)

        batch.set(userRef, usuario)
        batch.set(medicalRef, datosMedicos)

        batch.commit()
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }

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

    fun getDatosMedicos(uid: String, callback: (DatosMedicos?) -> Unit) {
        db.collection("datos_medicos").document(uid).get()
            .addOnSuccessListener { document ->
                callback(document.toObject(DatosMedicos::class.java))
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun eliminarCuenta(callback: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        val uid = user?.uid ?: return callback(false, "No hay usuario autenticado")

        db.collection("usuarios").document(uid).delete()
            .addOnSuccessListener {
                user.delete()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            callback(true, null)
                        } else {
                            callback(false, task.exception?.message)
                        }
                    }
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }
}
