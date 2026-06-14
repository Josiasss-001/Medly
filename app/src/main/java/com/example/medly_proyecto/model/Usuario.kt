package com.example.medly_proyecto.model

data class Usuario(
    val uid: String = "",
    val nombres: String = "",
    val apellidos: String = "",
    val nombreCompleto: String = "",
    val correo: String = "",
    val ciudad: String = "",
    val fechaRegistro: Long = System.currentTimeMillis(),
    val perfilCompleto: Boolean = false,
    val credencialPdfUrl: String = "",
    val lastCredentialHash: String = ""
)
