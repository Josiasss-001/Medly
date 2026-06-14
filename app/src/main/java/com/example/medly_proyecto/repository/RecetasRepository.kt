package com.example.medly_proyecto.repository

import com.example.medly_proyecto.model.Receta
import com.example.medly_proyecto.model.TomaMedicamento
import com.example.medly_proyecto.util.SecurityUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class RecetasRepository {
    private val db = FirebaseFirestore.getInstance()

    fun guardarRecetaEncriptada(receta: Receta, callback: (Boolean, String?) -> Unit) {
        val recetaEncriptada = hashMapOf(
            "userId" to receta.userId,
            "nombreMedicamento" to SecurityUtils.encrypt(receta.nombreMedicamento),
            "nombreMedico" to SecurityUtils.encrypt(receta.nombreMedico),
            "dosis" to SecurityUtils.encrypt(receta.dosis),
            "frecuencia" to SecurityUtils.encrypt(receta.frecuencia),
            "duracion" to SecurityUtils.encrypt(receta.duracion),
            "cantidadTotal" to SecurityUtils.encrypt(receta.cantidadTotal),
            "cantidadEnvase" to SecurityUtils.encrypt(receta.cantidadEnvase),
            "vecesAlDia" to SecurityUtils.encrypt(receta.vecesAlDia),
            "instrucciones" to SecurityUtils.encrypt(receta.instrucciones),
            "metodoUso" to SecurityUtils.encrypt(receta.metodoUso),
            "fechaCaptura" to receta.fechaCaptura,
            "imagenUri" to receta.imagenUri,
            "tratamientoIniciado" to receta.tratamientoIniciado
        )

        if (receta.id.isNotEmpty()) {
            db.collection("recetas_encriptadas").document(receta.id).set(recetaEncriptada)
                .addOnSuccessListener { callback(true, receta.id) }
                .addOnFailureListener { e -> callback(false, e.message) }
        } else {
            db.collection("recetas_encriptadas").add(recetaEncriptada)
                .addOnSuccessListener { doc -> callback(true, doc.id) }
                .addOnFailureListener { e -> callback(false, e.message) }
        }
    }

    fun getRecetasDesencriptadas(userId: String, callback: (List<Receta>?) -> Unit) {
        db.collection("recetas_encriptadas")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val lista = documents.map { doc ->
                    Receta(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        nombreMedicamento = SecurityUtils.decrypt(doc.getString("nombreMedicamento") ?: ""),
                        nombreMedico = SecurityUtils.decrypt(doc.getString("nombreMedico") ?: ""),
                        dosis = SecurityUtils.decrypt(doc.getString("dosis") ?: ""),
                        frecuencia = SecurityUtils.decrypt(doc.getString("frecuencia") ?: ""),
                        duracion = SecurityUtils.decrypt(doc.getString("duracion") ?: ""),
                        cantidadTotal = SecurityUtils.decrypt(doc.getString("cantidadTotal") ?: ""),
                        cantidadEnvase = SecurityUtils.decrypt(doc.getString("cantidadEnvase") ?: ""),
                        vecesAlDia = SecurityUtils.decrypt(doc.getString("vecesAlDia") ?: ""),
                        instrucciones = SecurityUtils.decrypt(doc.getString("instrucciones") ?: ""),
                        metodoUso = SecurityUtils.decrypt(doc.getString("metodoUso") ?: ""),
                        fechaCaptura = doc.getLong("fechaCaptura") ?: 0L,
                        imagenUri = doc.getString("imagenUri") ?: "",
                        tratamientoIniciado = doc.getBoolean("tratamientoIniciado") ?: false
                    )
                }
                callback(lista)
            }
            .addOnFailureListener { callback(null) }
    }

    fun eliminarReceta(recetaId: String, callback: (Boolean, String?) -> Unit) {
        db.collection("recetas_encriptadas").document(recetaId).delete()
            .addOnSuccessListener {
                eliminarTomasDeReceta(recetaId)
                callback(true, null)
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }

    fun guardarTomasMasivas(tomas: List<TomaMedicamento>, callback: (Boolean) -> Unit) {
        if (tomas.isEmpty()) { callback(true); return }
        val batch = db.batch()
        tomas.forEach { toma ->
            val docRef = db.collection("tomas_programadas").document(toma.id)
            batch.set(docRef, toma)
        }
        batch.commit().addOnCompleteListener { callback(it.isSuccessful) }
    }

    fun getTomasPorFecha(userId: String, fecha: String, callback: (List<TomaMedicamento>?) -> Unit) {
        db.collection("tomas_programadas")
            .whereEqualTo("idUsuario", userId)
            .whereEqualTo("fecha", fecha)
            .get()
            .addOnSuccessListener { callback(it.toObjects(TomaMedicamento::class.java)) }
            .addOnFailureListener { callback(null) }
    }

    fun getTomaById(tomaId: String, callback: (TomaMedicamento?) -> Unit) {
        db.collection("tomas_programadas").document(tomaId).get()
            .addOnSuccessListener { callback(it.toObject(TomaMedicamento::class.java)) }
            .addOnFailureListener { callback(null) }
    }

    fun actualizarEstadoToma(tomaId: String, nuevoEstado: String, timestamp: Long?, callback: (Boolean) -> Unit) {
        val updates = mutableMapOf<String, Any>("estado" to nuevoEstado)
        if (timestamp != null) updates["fechaCompletada"] = timestamp
        else updates["fechaCompletada"] = com.google.firebase.firestore.FieldValue.delete()

        db.collection("tomas_programadas").document(tomaId).update(updates)
            .addOnCompleteListener { callback(it.isSuccessful) }
    }

    private fun eliminarTomasDeReceta(recetaId: String) {
        db.collection("tomas_programadas")
            .whereEqualTo("idReceta", recetaId)
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                documents.forEach { batch.delete(it.reference) }
                batch.commit()
            }
    }
}
