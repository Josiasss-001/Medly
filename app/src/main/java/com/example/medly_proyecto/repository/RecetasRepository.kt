package com.example.medly_proyecto.repository

import com.example.medly_proyecto.model.Receta
import com.example.medly_proyecto.util.SecurityUtils
import com.google.firebase.firestore.FirebaseFirestore

class RecetasRepository {
    private val db = FirebaseFirestore.getInstance()

    fun guardarRecetaEncriptada(receta: Receta, callback: (Boolean, String?) -> Unit) {
        val recetaEncriptada = hashMapOf(
            "userId" to receta.userId,
            "nombreMedicamento" to SecurityUtils.encrypt(receta.nombreMedicamento),
            "dosis" to SecurityUtils.encrypt(receta.dosis),
            "frecuencia" to SecurityUtils.encrypt(receta.frecuencia),
            "duracion" to SecurityUtils.encrypt(receta.duracion),
            "cantidadTotal" to SecurityUtils.encrypt(receta.cantidadTotal),
            "cantidadEnvase" to SecurityUtils.encrypt(receta.cantidadEnvase),
            "vecesAlDia" to SecurityUtils.encrypt(receta.vecesAlDia),
            "instrucciones" to SecurityUtils.encrypt(receta.instrucciones),
            "metodoUso" to SecurityUtils.encrypt(receta.metodoUso),
            "fechaCaptura" to receta.fechaCaptura,
            "imagenUri" to receta.imagenUri
        )

        if (receta.id.isNotEmpty()) {
            // ACTUALIZAR receta existente
            db.collection("recetas_encriptadas")
                .document(receta.id)
                .set(recetaEncriptada)
                .addOnSuccessListener {
                    callback(true, receta.id)
                }
                .addOnFailureListener { e ->
                    callback(false, e.message)
                }
        } else {
            // CREAR nueva receta
            db.collection("recetas_encriptadas")
                .add(recetaEncriptada)
                .addOnSuccessListener { documentReference ->
                    callback(true, documentReference.id)
                }
                .addOnFailureListener { e ->
                    callback(false, e.message)
                }
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
                        dosis = SecurityUtils.decrypt(doc.getString("dosis") ?: ""),
                        frecuencia = SecurityUtils.decrypt(doc.getString("frecuencia") ?: ""),
                        duracion = SecurityUtils.decrypt(doc.getString("duracion") ?: ""),
                        cantidadTotal = SecurityUtils.decrypt(doc.getString("cantidadTotal") ?: ""),
                        cantidadEnvase = SecurityUtils.decrypt(doc.getString("cantidadEnvase") ?: ""),
                        vecesAlDia = SecurityUtils.decrypt(doc.getString("vecesAlDia") ?: ""),
                        instrucciones = SecurityUtils.decrypt(doc.getString("instrucciones") ?: ""),
                        metodoUso = SecurityUtils.decrypt(doc.getString("metodoUso") ?: ""),
                        fechaCaptura = doc.getLong("fechaCaptura") ?: 0L,
                        imagenUri = doc.getString("imagenUri") ?: ""
                    )
                }
                callback(lista)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun eliminarReceta(recetaId: String, callback: (Boolean, String?) -> Unit) {
        db.collection("recetas_encriptadas").document(recetaId).delete()
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }
}
