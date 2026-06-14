package com.example.medly_proyecto.repository

import com.example.medly_proyecto.model.DatosMedicos
import com.example.medly_proyecto.model.PerfilImagenes
import com.example.medly_proyecto.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.android.gms.tasks.Tasks

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

    fun actualizarCredencialUrl(uid: String, url: String, callback: (Boolean) -> Unit) {
        db.collection("usuarios").document(uid).update("credencialPdfUrl", url)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun actualizarCredencialUrlYHash(uid: String, url: String, hash: String, callback: (Boolean) -> Unit) {
        val updates = mapOf(
            "credencialPdfUrl" to url,
            "lastCredentialHash" to hash
        )
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

    fun listenToUsuario(uid: String, callback: (Usuario?) -> Unit): ListenerRegistration {
        return db.collection("usuarios").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    callback(null)
                    return@addSnapshotListener
                }
                callback(snapshot?.toObject(Usuario::class.java))
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

        // Lista de todas las colecciones donde el usuario puede tener datos
        val collectionsWithUserId = listOf(
            "usuarios", 
            "datos_medicos", 
            "perfil_imagenes", 
            "recetas_encriptadas", 
            "citas_encriptadas", 
            "documentos_encriptados",
            "notificaciones_historial"
        )
        val collectionsWithIdUsuario = listOf("tomas_programadas")

        val queryTasks = mutableListOf<com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot>>()
        
        collectionsWithUserId.forEach { coll ->
            queryTasks.add(db.collection(coll).whereEqualTo("userId", uid).get())
        }
        collectionsWithIdUsuario.forEach { coll ->
            queryTasks.add(db.collection(coll).whereEqualTo("idUsuario", uid).get())
        }

        Tasks.whenAllComplete(queryTasks).addOnCompleteListener { 
            val batch = db.batch()
            
            queryTasks.forEach { task ->
                if (task.isSuccessful) {
                    task.result?.documents?.forEach { doc ->
                        batch.delete(doc.reference)
                    }
                }
            }
            
            // Documentos directos que se identifican por UID
            batch.delete(db.collection("usuarios").document(uid))
            batch.delete(db.collection("datos_medicos").document(uid))
            batch.delete(db.collection("perfil_imagenes").document(uid))

            batch.commit()
                .addOnSuccessListener {
                    // Una vez borrada la base de datos, borramos el usuario de Auth
                    user.delete()
                        .addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                callback(true, null)
                            } else {
                                // Error común: "This operation is sensitive and requires recent authentication"
                                callback(false, authTask.exception?.message ?: "Error al eliminar de Auth")
                            }
                        }
                }
                .addOnFailureListener { e ->
                    callback(false, e.message)
                }
        }
    }
}
