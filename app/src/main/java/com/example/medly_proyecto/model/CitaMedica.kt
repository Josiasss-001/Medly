package com.example.medly_proyecto.model

import java.io.Serializable

data class CitaMedica(
    val id: String = "",
    val userId: String = "",
    val especialidad: String = "",
    val nombreMedico: String = "",
    val centroMedico: String = "",
    val fechaCita: Long = 0L, // Timestamp de la fecha
    val horaCita: String = "", // Ej: "10:30 AM"
    val motivoConsulta: String = "",
    val instruccionesPrevias: String = "", // Ayuno, exámenes, etc.
    val notas: String = "",
    val fechaCaptura: Long = System.currentTimeMillis(),
    val imagenUri: String = "" // Por si le toman foto al carnet de la cita
) : Serializable
