package com.example.medly_proyecto.model

data class TomaMedicamento(
    val id: String = "",
    val idReceta: String = "",
    val idUsuario: String = "",
    val nombreMedicamento: String = "",
    val fecha: String = "", // Formato YYYY-MM-DD para fácil consulta
    val horaProgramada: String = "", // Formato HH:mm
    val estado: String = "PENDIENTE", // PENDIENTE, TOMADA, OMITIDA
    val fechaCompletada: Long? = null,
    val dosis: String = ""
)
