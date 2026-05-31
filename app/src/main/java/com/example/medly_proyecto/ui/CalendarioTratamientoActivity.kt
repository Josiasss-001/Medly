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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.ui.adapter.EventosTratamientoAdapter
import com.example.medly_proyecto.viewmodel.CalendarioTratamientoViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class CalendarioTratamientoActivity : AppCompatActivity() {

    private val viewModel: CalendarioTratamientoViewModel by viewModels()
    private lateinit var calendarGrid: GridLayout
    private lateinit var currentMonthYear: TextView
    private lateinit var rvEvents: RecyclerView
    private lateinit var adapter: EventosTratamientoAdapter
    private lateinit var fabAddTreatment: FloatingActionButton
    
    private var calendar = Calendar.getInstance()
    private var selectedDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendario_tratamiento)

        // Inicializar vistas
        calendarGrid = findViewById(R.id.calendarGrid)
        currentMonthYear = findViewById(R.id.currentMonthYear)
        rvEvents = findViewById(R.id.rvEvents)
        fabAddTreatment = findViewById(R.id.fabAddTreatment)
        
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        findViewById<ImageButton>(R.id.prevMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        findViewById<ImageButton>(R.id.nextMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        fabAddTreatment.setOnClickListener {
            Toast.makeText(this, "Función para agregar tratamiento manual próximamente", Toast.LENGTH_SHORT).show()
        }

        // Recibir ID de receta si viene de RecetaDetalleActivity
        val recetaId = intent.getStringExtra("RECETA_ID")
        if (recetaId != null) {
            viewModel.setRecetaIdEspecifica(recetaId)
        }

        setupRecyclerView()
        updateCalendar()
        
        actualizarListaEventos(selectedDate)
    }

    private fun setupRecyclerView() {
        adapter = EventosTratamientoAdapter(emptyList())
        rvEvents.layoutManager = LinearLayoutManager(this)
        rvEvents.adapter = adapter
        // Asegurar que el scroll sea manejado por el NestedScrollView
        rvEvents.isNestedScrollingEnabled = false

        viewModel.eventos.observe(this) { eventos ->
            adapter.updateEventos(eventos)
        }
    }

    private fun updateCalendar() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        currentMonthYear.text = sdf.format(calendar.time).replaceFirstChar { it.uppercase() }

        calendarGrid.removeAllViews()

        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        
        // Ajuste para que Lunes sea 0 y Domingo sea 6
        val firstDayOfWeek = (tempCal.get(Calendar.DAY_OF_WEEK) + 5) % 7

        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Espacios vacíos
        for (i in 0 until firstDayOfWeek) {
            val emptyView = View(this)
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dpToPx(40)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
            }
            emptyView.layoutParams = params
            calendarGrid.addView(emptyView)
        }

        val today = Calendar.getInstance()
        for (day in 1..daysInMonth) {
            val loopCal = tempCal.clone() as Calendar
            loopCal.set(Calendar.DAY_OF_MONTH, day)
            
            val isSelected = isSameDay(loopCal, selectedDate)
            val isToday = isSameDay(loopCal, today)

            val dayView = TextView(this).apply {
                text = day.toString()
                gravity = Gravity.CENTER
                textSize = 14f
                
                if (isSelected) {
                    setBackgroundResource(R.drawable.selector_pill_active)
                    setTextColor(Color.WHITE)
                } else if (isToday) {
                    setTextColor(resources.getColor(R.color.Celeste, null))
                    setBackgroundResource(R.drawable.selector_pill_inactive)
                } else {
                    setTextColor(Color.BLACK)
                    setBackgroundResource(0)
                }
                
                setOnClickListener {
                    selectedDate = loopCal
                    actualizarListaEventos(loopCal)
                    updateCalendar()
                }
            }
            
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dpToPx(40)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
                setMargins(2, 2, 2, 2)
            }
            dayView.layoutParams = params
            calendarGrid.addView(dayView)
        }
    }

    private fun actualizarListaEventos(cal: Calendar) {
        viewModel.seleccionarFecha(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
