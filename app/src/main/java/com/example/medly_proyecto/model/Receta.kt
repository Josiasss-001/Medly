package com.example.medly_proyecto.model

import java.io.Serializable

data class Receta(
    val id: String = "",
    val userId: String = "",
    val nombreMedicamento: String = "",
    val nombreMedico: String = "",
    val dosis: String = "",
    val frecuencia: String = "",
    val duracion: String = "",
    val cantidadTotal: String = "", 
    val cantidadEnvase: String = "", 
    val vecesAlDia: String = "",
    val instrucciones: String = "",
    val metodoUso: String = "",
    val fechaCaptura: Long = System.currentTimeMillis(),
    val imagenUri: String = "",
    val tratamientoIniciado: Boolean = false
) : Serializable
