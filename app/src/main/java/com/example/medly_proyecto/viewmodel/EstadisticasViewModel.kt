package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.medly_proyecto.model.TomaMedicamento
import com.example.medly_proyecto.repository.RecetasRepository
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

data class EstadisticasResumen(
    val totalProgramadas: Int = 0,
    val totalRealizadas: Int = 0,
    val totalOmitidas: Int = 0,
    val porcentajeAdherencia: Float = 0f
)

data class DatosGraficoBarra(val dia: String, val cantidad: Int)
data class DatosGraficoLinea(val fecha: String, val porcentaje: Float)

class EstadisticasViewModel : ViewModel() {
    private val recetasRepo = RecetasRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _resumen = MutableLiveData<EstadisticasResumen>()
    val resumen: LiveData<EstadisticasResumen> = _resumen

    private val _datosBarra = MutableLiveData<List<DatosGraficoBarra>>()
    val datosBarra: LiveData<List<DatosGraficoBarra>> = _datosBarra

    private val _datosLinea = MutableLiveData<List<DatosGraficoLinea>>()
    val datosLinea: LiveData<List<DatosGraficoLinea>> = _datosLinea

    private val _estaCargando = MutableLiveData<Boolean>()
    val estaCargando: LiveData<Boolean> = _estaCargando

    fun cargarEstadisticas() {
        val userId = auth.currentUser?.uid ?: return
        _estaCargando.value = true

        // Obtenemos todas las tomas del usuario para un análisis completo
        // En una app real, podríamos filtrar por los últimos 30 días
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("tomas_programadas")
            .whereEqualTo("idUsuario", userId)
            .get()
            .addOnSuccessListener { documents ->
                val tomas = documents.toObjects(TomaMedicamento::class.java)
                procesarDatos(tomas)
                _estaCargando.value = false
            }
            .addOnFailureListener {
                _estaCargando.value = false
            }
    }

    private fun procesarDatos(tomas: List<TomaMedicamento>) {
        if (tomas.isEmpty()) {
            _resumen.postValue(EstadisticasResumen())
            _datosBarra.postValue(emptyList())
            _datosLinea.postValue(emptyList())
            return
        }

        val total = tomas.size
        val realizadas = tomas.count { it.estado == "TOMADA" }
        val omitidas = tomas.count { it.estado == "OMITIDA" }
        val adherencia = if (total > 0) (realizadas.toFloat() / total.toFloat()) * 100f else 0f

        _resumen.postValue(EstadisticasResumen(total, realizadas, omitidas, adherencia))

        // Procesar datos para Bar Chart (Por día de la semana)
        val tomasPorDiaSemana = mutableMapOf<Int, Int>() // 1=Dom, 2=Lun, ...
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        tomas.filter { it.estado == "TOMADA" }.forEach { toma ->
            try {
                val date = sdf.parse(toma.fecha)
                if (date != null) {
                    cal.time = date
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                    tomasPorDiaSemana[dayOfWeek] = (tomasPorDiaSemana[dayOfWeek] ?: 0) + 1
                }
            } catch (e: Exception) {}
        }

        val diasNombres = arrayOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
        val listaBarra = mutableListOf<DatosGraficoBarra>()
        // Ordenamos de Lunes a Domingo (2, 3, 4, 5, 6, 7, 1)
        val ordenDias = listOf(2, 3, 4, 5, 6, 7, 1)
        ordenDias.forEach { d ->
            listaBarra.add(DatosGraficoBarra(diasNombres[d - 1], tomasPorDiaSemana[d] ?: 0))
        }
        _datosBarra.postValue(listaBarra)

        // Procesar datos para Line Chart (Evolución diaria últimos 7 días con datos)
        val tomasPorFecha = tomas.groupBy { it.fecha }
        val listaLinea = tomasPorFecha.map { (fecha, lista) ->
            val totalDia = lista.size
            val tomadasDia = lista.count { it.estado == "TOMADA" }
            val porc = if (totalDia > 0) (tomadasDia.toFloat() / totalDia.toFloat()) * 100f else 0f
            DatosGraficoLinea(fecha, porc)
        }.sortedBy { it.fecha }.takeLast(7) // Últimos 7 días

        _datosLinea.postValue(listaLinea)
    }
}
