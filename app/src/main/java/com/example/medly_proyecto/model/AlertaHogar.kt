package com.example.medly_proyecto.model

data class AlertaHogar(
    val id: String,
    val tipo: TipoAlerta,
    val titulo: String,
    val mensaje: String,
    val iconoRes: Int,
    val priority: Int, // 1: Dosis, 2: Citas, 3: Recetas
    val dataId: String // ID de la receta, cita o toma para navegación
)

enum class TipoAlerta {
    DOSIS, CITA, RECETA
}
