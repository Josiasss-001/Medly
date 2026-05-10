package com.example.medly_proyecto.model

data class Usuario(
    val uid: String = "",
    val nombreCompleto: String = "",
    val correo: String = "",
    val fechaRegistro: Long = System.currentTimeMillis(),
    val perfilCompleto: Boolean = false
)
