package com.example.medly_proyecto.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.medly_proyecto.R
import com.example.medly_proyecto.databinding.ActivityEstadisticasBinding
import com.example.medly_proyecto.viewmodel.EstadisticasViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import java.util.Locale

class EstadisticasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEstadisticasBinding
    private val viewModel: EstadisticasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEstadisticasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCharts()
        observeViewModel()

        viewModel.cargarEstadisticas()
    }

    private fun setupToolbar() {
        // Ahora menuIcon está directamente en el binding raíz, no dentro de includeTopBar
        binding.menuIcon.setOnClickListener { finish() }
    }

    private fun setupCharts() {
        // Pie Chart Configuration
        binding.pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            transparentCircleRadius = 61f
            holeRadius = 58f
            setDrawCenterText(true)
            centerText = "Cumplimiento"
            setCenterTextSize(16f)
            legend.isEnabled = true
        }

        // Bar Chart Configuration
        binding.barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setPinchZoom(false)
            setDrawValueAboveBar(true)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
        }

        // Line Chart Configuration
        binding.lineChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(true)
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.estaCargando.observe(this) { cargando ->
            binding.progressBar.visibility = if (cargando) View.VISIBLE else View.GONE
        }

        viewModel.resumen.observe(this) { resumen ->
            binding.tvTotalProgramadas.text = resumen.totalProgramadas.toString()
            binding.tvTotalRealizadas.text = resumen.totalRealizadas.toString()
            binding.tvTotalOmitidas.text = resumen.totalOmitidas.toString()
            binding.tvPorcentajeAdherencia.text = String.format(Locale.getDefault(), "%.1f%%", resumen.porcentajeAdherencia)

            updatePieChart(resumen.totalRealizadas, resumen.totalOmitidas)
        }

        viewModel.datosBarra.observe(this) { datos ->
            updateBarChart(datos)
        }

        viewModel.datosLinea.observe(this) { datos ->
            updateLineChart(datos)
        }
    }

    private fun updatePieChart(realizadas: Int, omitidas: Int) {
        val entries = ArrayList<PieEntry>()
        if (realizadas > 0) entries.add(PieEntry(realizadas.toFloat(), "Realizadas"))
        if (omitidas > 0) entries.add(PieEntry(omitidas.toFloat(), "Omitidas"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(Color.parseColor("#10B981"), Color.parseColor("#EF4444"))
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueFormatter = PercentFormatter(binding.pieChart)

        val data = PieData(dataSet)
        binding.pieChart.data = data
        binding.pieChart.animateY(1400, Easing.EaseInOutQuad)
        binding.pieChart.invalidate()
    }

    private fun updateBarChart(datos: List<com.example.medly_proyecto.viewmodel.DatosGraficoBarra>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        datos.forEachIndexed { index, d ->
            entries.add(BarEntry(index.toFloat(), d.cantidad.toFloat()))
            labels.add(d.dia)
        }

        val dataSet = BarDataSet(entries, "Tomas Realizadas")
        dataSet.color = Color.parseColor("#93D6E0")
        dataSet.valueTextSize = 10f

        val data = BarData(dataSet)
        binding.barChart.data = data
        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.barChart.animateY(1000)
        binding.barChart.invalidate()
    }

    private fun updateLineChart(datos: List<com.example.medly_proyecto.viewmodel.DatosGraficoLinea>) {
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        datos.forEachIndexed { index, d ->
            entries.add(Entry(index.toFloat(), d.porcentaje))
            labels.add(d.fecha.substring(5)) // Mostrar solo MM-DD
        }

        val dataSet = LineDataSet(entries, "Adherencia %")
        dataSet.color = Color.parseColor("#93D6E0")
        dataSet.setCircleColor(Color.parseColor("#93D6E0"))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextSize = 10f
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val data = LineData(dataSet)
        binding.lineChart.data = data
        binding.lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.lineChart.invalidate()
    }
}
