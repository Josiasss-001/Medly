package com.example.medly_proyecto.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
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
import com.example.medly_proyecto.ui.adapter.EventosTratamientoAdapter
import com.example.medly_proyecto.viewmodel.CalendarioTratamientoViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class CalendarioTratamientoActivity : AppCompatActivity() {

    private val modeloVista: CalendarioTratamientoViewModel by viewModels()
    private lateinit var gridCalendario: GridLayout
    private lateinit var tvMesActual: TextView
    private lateinit var rvEventos: RecyclerView
    private lateinit var adaptador: EventosTratamientoAdapter
    private lateinit var fabAgregar: FloatingActionButton
    
    private lateinit var tvEtiquetaDia: TextView
    private lateinit var tvNumeroDia: TextView
    private lateinit var tvNombreDia: TextView
    private lateinit var tvResumenEventos: TextView
    private lateinit var btnAtras: ImageButton

    private var calendario = Calendar.getInstance()
    private var fechaSeleccionada = Calendar.getInstance()
    private val localeEs = Locale("es", "ES")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendario_tratamiento)

        inicializarVistas()
        configurarEventos()
        observarModelo()
        
        // Cargar receta específica si viene del intent
        val recetaId = intent.getStringExtra("RECETA_ID")
        if (recetaId != null) {
            modeloVista.setRecetaIdEspecifica(recetaId)
        }

        actualizarCalendario()
        actualizarInfoDiaSeleccionado(fechaSeleccionada)
    }

    private fun inicializarVistas() {
        gridCalendario = findViewById(R.id.gridCalendario)
        tvMesActual = findViewById(R.id.tvMesActual)
        rvEventos = findViewById(R.id.rvEventosTratamiento)
        fabAgregar = findViewById(R.id.fabAgregarTratamiento)
        btnAtras = findViewById(R.id.btnAtras)
        
        tvEtiquetaDia = findViewById(R.id.tvEtiquetaDia)
        tvNumeroDia = findViewById(R.id.tvNumeroDia)
        tvNombreDia = findViewById(R.id.tvNombreDia)
        tvResumenEventos = findViewById(R.id.tvResumenEventos)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }
    }

    private fun configurarEventos() {
        btnAtras.setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.btnMesAnterior).setOnClickListener {
            calendario.add(Calendar.MONTH, -1)
            actualizarCalendario()
        }

        findViewById<ImageButton>(R.id.btnMesSiguiente).setOnClickListener {
            calendario.add(Calendar.MONTH, 1)
            actualizarCalendario()
        }

        fabAgregar.setOnClickListener {
            Toast.makeText(this, "Añadir tratamiento manual", Toast.LENGTH_SHORT).show()
        }

        // Inicializamos el adaptador con el callback para guardar en Firestore
        adaptador = EventosTratamientoAdapter(emptyList()) { evento, estaMarcado ->
            modeloVista.marcarToma(evento, estaMarcado)
        }
        rvEventos.layoutManager = LinearLayoutManager(this)
        rvEventos.adapter = adaptador
    }

    private fun observarModelo() {
        modeloVista.eventos.observe(this) { eventos ->
            adaptador.updateEventos(eventos)
            val cantidad = eventos.size
            tvResumenEventos.text = when (cantidad) {
                0 -> "No hay recordatorios para hoy"
                1 -> "1 recordatorio programado"
                else -> "$cantidad recordatorios programados"
            }
        }
    }

    private fun actualizarCalendario() {
        val formatoMes = SimpleDateFormat("MMMM yyyy", localeEs)
        tvMesActual.text = formatoMes.format(calendario.time).replaceFirstChar { it.uppercase() }

        gridCalendario.removeAllViews()

        val tempCal = calendario.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        
        var primerDiaSemana = tempCal.get(Calendar.DAY_OF_WEEK) - 2
        if (primerDiaSemana < 0) primerDiaSemana = 6

        val diasEnMes = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 0 until primerDiaSemana) {
            gridCalendario.addView(crearEspacioVacio())
        }

        val hoy = Calendar.getInstance()
        for (dia in 1..diasEnMes) {
            val loopCal = tempCal.clone() as Calendar
            loopCal.set(Calendar.DAY_OF_MONTH, dia)
            
            val esSeleccionado = esMismoDia(loopCal, fechaSeleccionada)
            val esHoy = esMismoDia(loopCal, hoy)

            val vistaDia = crearVistaDia(dia, esSeleccionado, esHoy) {
                fechaSeleccionada = loopCal
                actualizarInfoDiaSeleccionado(loopCal)
                actualizarCalendario()
            }
            gridCalendario.addView(vistaDia)
        }
    }

    private fun crearVistaDia(dia: Int, seleccionado: Boolean, esHoy: Boolean, alClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = dia.toString()
            gravity = Gravity.CENTER
            textSize = 15f
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = dpToPx(44)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
            }
            
            if (seleccionado) {
                setBackgroundResource(R.drawable.calendar_day_selected)
                setTextColor(Color.WHITE)
            } else if (esHoy) {
                setBackgroundResource(R.drawable.calendar_day_today)
                setTextColor(Color.BLACK)
            } else {
                setTextColor(Color.parseColor("#475569"))
                setBackgroundResource(0)
            }
            
            setOnClickListener { alClick() }
        }
    }

    private fun crearEspacioVacio(): View {
        return View(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = dpToPx(44)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
            }
        }
    }

    private fun actualizarInfoDiaSeleccionado(cal: Calendar) {
        val hoy = Calendar.getInstance()
        tvEtiquetaDia.text = if (esMismoDia(cal, hoy)) "HOY" else "FECHA"
        tvNumeroDia.text = cal.get(Calendar.DAY_OF_MONTH).toString()
        
        val formatoNombreDia = SimpleDateFormat("EEEE", localeEs)
        tvNombreDia.text = formatoNombreDia.format(cal.time).replaceFirstChar { it.uppercase() }

        modeloVista.seleccionarFecha(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun esMismoDia(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
