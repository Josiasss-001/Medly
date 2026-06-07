package com.example.medly_proyecto.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.CitaMedica
import com.example.medly_proyecto.model.TomaMedicamento
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.SimpleDateFormat
import java.util.*

class HomeCarouselAdapter(
    private val onCitaClick: () -> Unit,
    private val onMedClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var proximaCita: CitaMedica? = null
    private var proximaToma: TomaMedicamento? = null
    private var progresoMed: Pair<Int, Int>? = null

    companion object {
        private const val TYPE_CITA = 0
        private const val TYPE_MED = 1
    }

    fun updateData(cita: CitaMedica?, toma: TomaMedicamento?, progreso: Pair<Int, Int>?) {
        this.proximaCita = cita
        this.proximaToma = toma
        this.progresoMed = progreso
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_CITA) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_home_cita_card, parent, false)
            CitaViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_home_medicamento_card, parent, false)
            MedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CitaViewHolder) holder.bind(proximaCita, onCitaClick)
        else if (holder is MedViewHolder) holder.bind(proximaToma, progresoMed, onMedClick)
    }

    override fun getItemCount(): Int = 2

    class CitaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvFecha: TextView = view.findViewById(R.id.tvFechaCita)
        private val tvHora: TextView = view.findViewById(R.id.tvHoraCita)
        private val tvDoctor: TextView = view.findViewById(R.id.tvDoctorCita)
        private val tvEspecialidad: TextView = view.findViewById(R.id.tvEspecialidadCita)
        private val tvBadge: TextView = view.findViewById(R.id.tvBadgeDias)
        private val btnVer: View = view.findViewById(R.id.btnVerDetallesCita)

        fun bind(cita: CitaMedica?, onClick: () -> Unit) {
            if (cita == null) {
                tvFecha.text = "Sin citas"
                tvHora.text = ""
                tvDoctor.text = "No hay próximas citas agendadas"
                tvEspecialidad.text = ""
                tvBadge.visibility = View.GONE
            } else {
                val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("es", "ES"))
                tvFecha.text = sdf.format(Date(cita.fechaCita))
                tvHora.text = cita.horaCita + " hrs"
                tvDoctor.text = cita.nombreMedico
                tvEspecialidad.text = cita.especialidad
                
                val diff = cita.fechaCita - System.currentTimeMillis()
                val days = (diff / (1000 * 60 * 60 * 24)).toInt()
                tvBadge.visibility = View.VISIBLE
                tvBadge.text = when {
                    days == 0 -> "Hoy"
                    days == 1 -> "Mañana"
                    else -> "En $days días"
                }
            }
            itemView.setOnClickListener { onClick() }
        }
    }

    class MedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombre: TextView = view.findViewById(R.id.tvNombreMed)
        private val tvHora: TextView = view.findViewById(R.id.tvHoraMed)
        private val tvProgreso: TextView = view.findViewById(R.id.tvProgresoTexto)
        private val progressBar: LinearProgressIndicator = view.findViewById(R.id.progressMed)

        fun bind(toma: TomaMedicamento?, progreso: Pair<Int, Int>?, onClick: () -> Unit) {
            if (toma == null) {
                tvNombre.text = "Sin medicamentos"
                tvHora.text = "Todo al día"
            } else {
                tvNombre.text = toma.nombreMedicamento + " " + (toma.dosis ?: "")
                tvHora.text = toma.horaProgramada + " hrs"
            }

            if (progreso != null) {
                tvProgreso.text = "${progreso.first} / ${progreso.second}"
                if (progreso.second > 0) {
                    progressBar.progress = (progreso.first * 100) / progreso.second
                } else {
                    progressBar.progress = 0
                }
            }
            itemView.setOnClickListener { onClick() }
        }
    }
}
