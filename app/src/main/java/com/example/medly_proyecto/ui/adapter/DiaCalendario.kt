package com.example.medly_proyecto.ui.adapter

import java.util.Calendar

data class DiaCalendario(
    val fecha: Calendar,
    var seleccionado: Boolean = false
)
