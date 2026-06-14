package com.example.medly_proyecto.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.ui.adapter.CalendarioDiasAdapter
import com.example.medly_proyecto.ui.adapter.DiaCalendario
import com.example.medly_proyecto.ui.adapter.MedicamentoGrupoAdapter
import com.example.medly_proyecto.viewmodel.CalendarioTratamientoViewModel
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.text.SimpleDateFormat
import java.util.*

class CalendarioTratamientoActivity : AppCompatActivity() {

    private val modeloVista: CalendarioTratamientoViewModel by viewModels()
    
    private lateinit var tvMesActual: TextView
    private lateinit var rvGrupos: RecyclerView
    private lateinit var adaptadorGrupos: MedicamentoGrupoAdapter
    
    private lateinit var rvCalendarioHorizontal: RecyclerView
    private lateinit var adaptadorDias: CalendarioDiasAdapter
    
    private lateinit var btnAtras: ImageButton

    // Vistas del Resumen Superior
    private lateinit var tvTomadasCount: TextView
    private lateinit var tvPendientesCount: TextView
    private lateinit var tvProximaHora: TextView
    private lateinit var tvAdherenciaPorc: TextView
    private lateinit var cpAdherencia: CircularProgressIndicator

    private var calendarioBase = Calendar.getInstance()
    private var fechaSeleccionada = Calendar.getInstance()
    private val localeEs = Locale("es", "ES")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendario_tratamiento)

        inicializarVistas()
        configurarRecyclerViews()
        configurarEventos()
        observarModelo()
        
        actualizarMesYCalendario()
    }

    private fun inicializarVistas() {
        tvMesActual = findViewById(R.id.tvMesActual)
        rvGrupos = findViewById(R.id.rvGruposMedicamentos)
        rvCalendarioHorizontal = findViewById(R.id.rvCalendarioHorizontal)
        btnAtras = findViewById(R.id.btnAtras)
        
        tvTomadasCount = findViewById(R.id.tvTomadasCount)
        tvPendientesCount = findViewById(R.id.tvPendientesCount)
        tvProximaHora = findViewById(R.id.tvProximaHora)
        tvAdherenciaPorc = findViewById(R.id.tvAdherenciaPorc)
        cpAdherencia = findViewById(R.id.cpAdherencia)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun configurarRecyclerViews() {
        // Lista de medicamentos
        adaptadorGrupos = MedicamentoGrupoAdapter(emptyList()) { evento, estaMarcado ->
            modeloVista.marcarToma(evento.id, estaMarcado)
        }
        rvGrupos.layoutManager = LinearLayoutManager(this)
        rvGrupos.adapter = adaptadorGrupos

        // Calendario horizontal
        adaptadorDias = CalendarioDiasAdapter(emptyList()) { nuevaFecha ->
            fechaSeleccionada = nuevaFecha
            actualizarFiltroFecha()
            actualizarListaDias()
        }
        rvCalendarioHorizontal.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvCalendarioHorizontal.adapter = adaptadorDias
    }

    private fun configurarEventos() {
        btnAtras.setOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btnMesAnterior).setOnClickListener {
            calendarioBase.add(Calendar.MONTH, -1)
            actualizarMesYCalendario()
        }

        findViewById<ImageButton>(R.id.btnMesSiguiente).setOnClickListener {
            calendarioBase.add(Calendar.MONTH, 1)
            actualizarMesYCalendario()
        }
    }

    private fun observarModelo() {
        modeloVista.grupos.observe(this) { grupos ->
            adaptadorGrupos.updateGrupos(grupos)
        }

        modeloVista.resumen.observe(this) { resumen ->
            tvTomadasCount.text = resumen.tomadas.toString()
            tvPendientesCount.text = resumen.pendientes.toString()
            tvProximaHora.text = resumen.proximaDosis
            tvAdherenciaPorc.text = "${resumen.adherencia}%"
            cpAdherencia.progress = resumen.adherencia
        }
    }

    private fun actualizarMesYCalendario() {
        val formatoMes = SimpleDateFormat("MMMM yyyy", localeEs)
        tvMesActual.text = formatoMes.format(calendarioBase.time).replaceFirstChar { it.uppercase() }
        actualizarListaDias()
    }

    private fun actualizarListaDias() {
        val dias = mutableListOf<DiaCalendario>()
        val tempCal = calendarioBase.clone() as Calendar
        val maxDias = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        for (i in 1..maxDias) {
            val diaCal = tempCal.clone() as Calendar
            diaCal.set(Calendar.DAY_OF_MONTH, i)
            dias.add(DiaCalendario(diaCal, esMismoDia(diaCal, fechaSeleccionada)))
        }
        
        adaptadorDias.updateDias(dias)
        
        // Scroll automático al día seleccionado
        if (fechaSeleccionada.get(Calendar.MONTH) == calendarioBase.get(Calendar.MONTH)) {
            rvCalendarioHorizontal.scrollToPosition(fechaSeleccionada.get(Calendar.DAY_OF_MONTH) - 1)
        }
    }

    private fun actualizarFiltroFecha() {
        modeloVista.seleccionarFecha(
            fechaSeleccionada.get(Calendar.YEAR),
            fechaSeleccionada.get(Calendar.MONTH),
            fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun esMismoDia(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
