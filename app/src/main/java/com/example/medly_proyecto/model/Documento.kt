package com.example.medly_proyecto.model

import java.io.Serializable

data class Documento(
    val id: String = "",
    val userId: String = "",
    val titulo: String = "",
    val tipo: String = "", // RECETA, CITA, EXAMEN, CERTIFICADO, ORDEN, OTRO
    val fecha: Long = 0L,
    val fechaTexto: String = "",
    val institucion: String = "",
    val resumen: String = "",
    val imagenUri: String = ""
) : Serializable
