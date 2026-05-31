package com.example.medly_proyecto.repository

import com.example.medly_proyecto.model.CitaMedica
import com.example.medly_proyecto.util.SecurityUtils
import com.google.firebase.firestore.FirebaseFirestore

class CitasRepository {
    private val db = FirebaseFirestore.getInstance()

    fun guardarCitaEncriptada(cita: CitaMedica, callback: (Boolean, String?) -> Unit) {
        val citaEncriptada = hashMapOf(
            "userId" to cita.userId,
            "especialidad" to SecurityUtils.encrypt(cita.especialidad),
            "nombreMedico" to SecurityUtils.encrypt(cita.nombreMedico),
            "centroMedico" to SecurityUtils.encrypt(cita.centroMedico),
            "fechaCita" to cita.fechaCita,
            "horaCita" to SecurityUtils.encrypt(cita.horaCita),
            "motivoConsulta" to SecurityUtils.encrypt(cita.motivoConsulta),
            "instruccionesPrevias" to SecurityUtils.encrypt(cita.instruccionesPrevias),
            "notas" to SecurityUtils.encrypt(cita.notas),
            "fechaCaptura" to cita.fechaCaptura,
            "imagenUri" to cita.imagenUri
        )

        if (cita.id.isNotEmpty()) {
            db.collection("citas_encriptadas")
                .document(cita.id)
                .set(citaEncriptada)
                .addOnSuccessListener {
                    callback(true, cita.id)
                }
                .addOnFailureListener { e ->
                    callback(false, e.message)
                }
        } else {
            db.collection("citas_encriptadas")
                .add(citaEncriptada)
                .addOnSuccessListener { documentReference ->
                    callback(true, documentReference.id)
                }
                .addOnFailureListener { e ->
                    callback(false, e.message)
                }
        }
    }

    fun getCitasDesencriptadas(userId: String, callback: (List<CitaMedica>?) -> Unit) {
        db.collection("citas_encriptadas")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val lista = documents.map { doc ->
                    CitaMedica(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        especialidad = SecurityUtils.decrypt(doc.getString("especialidad") ?: ""),
                        nombreMedico = SecurityUtils.decrypt(doc.getString("nombreMedico") ?: ""),
                        centroMedico = SecurityUtils.decrypt(doc.getString("centroMedico") ?: ""),
                        fechaCita = doc.getLong("fechaCita") ?: 0L,
                        horaCita = SecurityUtils.decrypt(doc.getString("horaCita") ?: ""),
                        motivoConsulta = SecurityUtils.decrypt(doc.getString("motivoConsulta") ?: ""),
                        instruccionesPrevias = SecurityUtils.decrypt(doc.getString("instruccionesPrevias") ?: ""),
                        notas = SecurityUtils.decrypt(doc.getString("notas") ?: ""),
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

    fun eliminarCita(citaId: String, callback: (Boolean, String?) -> Unit) {
        db.collection("citas_encriptadas").document(citaId).delete()
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }
}
