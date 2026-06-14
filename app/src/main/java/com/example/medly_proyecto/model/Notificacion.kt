package com.example.medly_proyecto.model

data class Notificacion(
    val id: String = "",
    val userId: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val leida: Boolean = false,
    val tipo: String = "" // MEDICAMENTO, CITA, DOCUMENTO, SISTEMA
)
