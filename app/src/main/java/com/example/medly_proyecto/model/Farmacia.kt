package com.example.medly_proyecto.model

import com.google.android.gms.maps.model.LatLng

data class Farmacia(
    val id: String,
    val nombre: String,
    val direccion: String,
    val ubicacion: LatLng,
    var distancia: String = "",
    var tiempo: String = "",
    var distanciaMetros: Float = 0f
)
