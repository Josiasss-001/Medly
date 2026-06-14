package com.example.medly_proyecto.model

data class ClassificationResult(
    val isValid: Boolean = false,
    val type: String = "INVALID", // "RECETA", "CITA", "DOCUMENTO", "INVALID"
    val data: String = "",  // JSON string of the specific result
    val error: String? = null
)
