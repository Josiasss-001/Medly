package com.example.medly_proyecto.ui

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.ui.adapter.CitasAdapter
import com.example.medly_proyecto.viewmodel.CalendarioCitasViewModel
import com.google.android.material.imageview.ShapeableImageView
import java.text.SimpleDateFormat
import java.util.*

class CalendarioCitasActivity : AppCompatActivity() {

    private val viewModel: CalendarioCitasViewModel by viewModels()
    private lateinit var gridCalendario: GridLayout
    private lateinit var tvMesActual: TextView
    private lateinit var rvCitasHoy: RecyclerView
    private lateinit var tvNoCitas: TextView
    private lateinit var tvSectionTitle: TextView
    private lateinit var ivUserProfile: ShapeableImageView
    private lateinit var adapter: CitasAdapter

    private var calendarDisplay = Calendar.getInstance()
    private var selectedDate = Calendar.getInstance()
    private val localeEs = Locale("es", "ES")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendario_citas)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        gridCalendario = findViewById(R.id.gridCalendario)
        tvMesActual = findViewById(R.id.tvMesActual)
        rvCitasHoy = findViewById(R.id.rvCitasHoy)
        tvNoCitas = findViewById(R.id.tvNoCitas)
        tvSectionTitle = findViewById(R.id.tvSectionTitle)
        ivUserProfile = findViewById(R.id.ivUserProfile)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        
        // Listeners para cambiar de mes
        findViewById<ImageButton>(R.id.btnMesAnterior).setOnClickListener {
            calendarDisplay.add(Calendar.MONTH, -1)
            updateCalendarGrid()
        }

        findViewById<ImageButton>(R.id.btnMesSiguiente).setOnClickListener {
            calendarDisplay.add(Calendar.MONTH, 1)
            updateCalendarGrid()
        }

        findViewById<View>(R.id.btnAddCita).setOnClickListener {
            val intent = Intent(this, DocumentosActivity::class.java).apply {
                putExtra("INITIAL_FILTER", "CITA")
            }
            startActivity(intent, ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle())
        }

        rvCitasHoy.layoutManager = LinearLayoutManager(this)
        adapter = CitasAdapter(emptyList()) { cita ->
            val intent = Intent(this, CitaDetalleActivity::class.java).apply {
                putExtra("CITA_OBJ", cita)
                putExtra("IS_NEW_APPOINTMENT", false)
            }
            startActivity(intent)
        }
        rvCitasHoy.adapter = adapter

        val rootLayout = findViewById<View>(R.id.clHeader).parent as View
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        updateCalendarGrid()
    }

    private fun observeViewModel() {
        viewModel.citasDelMes.observe(this) {
            updateCalendarGrid()
        }

        viewModel.citasDiaSeleccionado.observe(this) { citas ->
            if (citas.isEmpty()) {
                tvNoCitas.visibility = View.VISIBLE
                rvCitasHoy.visibility = View.GONE
            } else {
                tvNoCitas.visibility = View.GONE
                rvCitasHoy.visibility = View.VISIBLE
                adapter.updateCitas(citas)
            }
            
            val sdf = SimpleDateFormat("dd 'de' MMMM", localeEs)
            tvSectionTitle.text = getString(R.string.citas_para_el, sdf.format(selectedDate.time))
        }

        viewModel.perfilImagenes.observe(this) { imagenes ->
            imagenes?.profileImageUrl?.let { base64 ->
                if (base64.isNotEmpty()) {
                    val bitmap = base64ToBitmap(base64)
                    ivUserProfile.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun updateCalendarGrid() {
        val formatoMes = SimpleDateFormat("MMMM yyyy", localeEs)
        tvMesActual.text = formatoMes.format(calendarDisplay.time).replaceFirstChar { it.uppercase() }

        gridCalendario.removeAllViews()

        val tempCal = calendarDisplay.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        
        var firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 2
        if (firstDayOfWeek < 0) firstDayOfWeek = 6

        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 0 until firstDayOfWeek) {
            gridCalendario.addView(createEmptyView())
        }

        val today = Calendar.getInstance()
        for (day in 1..daysInMonth) {
            val dateInLoop = tempCal.clone() as Calendar
            dateInLoop.set(Calendar.DAY_OF_MONTH, day)
            
            val isSelected = isSameDay(dateInLoop, selectedDate)
            val isToday = isSameDay(dateInLoop, today)
            val hasCita = viewModel.tieneCitaEnFecha(dateInLoop)

            val dayView = createDayView(day, isSelected, isToday, hasCita) {
                selectedDate = dateInLoop
                viewModel.seleccionarFecha(dateInLoop)
                updateCalendarGrid()
            }
            gridCalendario.addView(dayView)
        }
    }

    private fun createDayView(day: Int, selected: Boolean, today: Boolean, hasCita: Boolean, onClick: () -> Unit): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = dpToPx(55)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
            }
            setOnClickListener { onClick() }
        }

        val tvDay = TextView(this).apply {
            text = day.toString()
            gravity = Gravity.CENTER
            textSize = 16f
            typeface = if (selected || today) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36))
            
            if (selected) {
                setBackgroundResource(R.drawable.dot_blue)
                setTextColor(Color.WHITE)
            } else if (today) {
                setTextColor(Color.parseColor("#4DB6AC"))
            } else {
                setTextColor(Color.parseColor("#333333"))
            }
        }

        container.addView(tvDay)

        if (hasCita) {
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(6), dpToPx(6)).apply {
                    topMargin = dpToPx(2)
                }
                setBackgroundResource(R.drawable.dot_red)
            }
            container.addView(dot)
        }

        return container
    }

    private fun createEmptyView(): View {
        return View(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = dpToPx(55)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) { null }
    }
}
