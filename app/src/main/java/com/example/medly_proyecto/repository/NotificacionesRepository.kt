package com.example.medly_proyecto.repository

import com.example.medly_proyecto.model.Notificacion
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificacionesRepository {
    private val db = FirebaseFirestore.getInstance()

    fun guardarNotificacion(notificacion: Notificacion, id: String? = null, callback: (Boolean) -> Unit = {}) {
        val ref = if (id != null) db.collection("notificaciones_historial").document(id)
                  else db.collection("notificaciones_historial").document()

        val notifConId = notificacion.copy(id = ref.id)
        ref.set(notifConId)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun getNotificaciones(userId: String, callback: (List<Notificacion>?) -> Unit) {
        db.collection("notificaciones_historial")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    callback(null)
                    return@addSnapshotListener
                }
                val lista = snapshot?.toObjects(Notificacion::class.java)
                callback(lista)
            }
    }

    fun marcarComoLeidas(userId: String) {
        db.collection("notificaciones_historial")
            .whereEqualTo("userId", userId)
            .whereEqualTo("leida", false)
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                for (doc in documents) {
                    batch.update(doc.reference, "leida", true)
                }
                batch.commit()
            }
    }
}
