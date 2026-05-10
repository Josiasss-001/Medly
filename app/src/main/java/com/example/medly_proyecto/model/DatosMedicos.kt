package com.example.medly_proyecto.model

data class DatosMedicos(
    val uid: String = "",
    val ciudad: String = "",
    val fechaNacimiento: String = "",
    val edad: Int = 0,
    val peso: Double = 0.0,
    val estatura: Int = 0,
    val sexo: String = "",
    val enfermedadCronica: Boolean = false,
    val detalleEnfermedad: String = ""
)
