package com.example.medly_proyecto.repository

import com.example.medly_proyecto.model.Documento
import com.example.medly_proyecto.util.SecurityUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DocumentoRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getDocumentos(userId: String, callback: (List<Documento>?) -> Unit) {
        db.collection("documentos_encriptados")
            .whereEqualTo("userId", userId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val lista = documents.map { doc ->
                    Documento(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        titulo = SecurityUtils.decrypt(doc.getString("titulo") ?: ""),
                        tipo = doc.getString("tipo") ?: "OTRO",
                        fecha = doc.getLong("fecha") ?: 0L,
                        fechaTexto = doc.getString("fechaTexto") ?: "",
                        institucion = SecurityUtils.decrypt(doc.getString("institucion") ?: ""),
                        resumen = SecurityUtils.decrypt(doc.getString("resumen") ?: ""),
                        imagenUri = doc.getString("imagenUri") ?: ""
                    )
                }
                callback(lista)
            }
            .addOnFailureListener { callback(null) }
    }

    fun guardarDocumento(documento: Documento, callback: (Boolean) -> Unit) {
        val docData = hashMapOf(
            "userId" to documento.userId,
            "titulo" to SecurityUtils.encrypt(documento.titulo),
            "tipo" to documento.tipo,
            "fecha" to documento.fecha,
            "fechaTexto" to documento.fechaTexto,
            "institucion" to SecurityUtils.encrypt(documento.institucion),
            "resumen" to SecurityUtils.encrypt(documento.resumen),
            "imagenUri" to documento.imagenUri
        )

        db.collection("documentos_encriptados").add(docData)
            .addOnCompleteListener { callback(it.isSuccessful) }
    }

    fun eliminarDocumento(docId: String, callback: (Boolean, String?) -> Unit) {
        db.collection("documentos_encriptados").document(docId).delete()
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }
}
