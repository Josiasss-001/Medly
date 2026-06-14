package com.example.medly_proyecto.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.google.android.material.card.MaterialCardView
import java.util.Calendar
import java.util.Locale

class CalendarioDiasAdapter(
    private var dias: List<DiaCalendario>,
    private val onDiaSelected: (Calendar) -> Unit
) : RecyclerView.Adapter<CalendarioDiasAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cvContainer: MaterialCardView = view.findViewById(R.id.cvDiaContainer)
        val tvNombre: TextView = view.findViewById(R.id.tvDiaNombre)
        val tvNumero: TextView = view.findViewById(R.id.tvDiaNumero)
        val viewIndicator: View = view.findViewById(R.id.viewIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dia_calendario, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dia = dias[position]
        val localeEs = Locale("es", "ES")
        
        holder.tvNombre.text = dia.fecha.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, localeEs)?.uppercase()
        holder.tvNumero.text = dia.fecha.get(Calendar.DAY_OF_MONTH).toString()

        if (dia.seleccionado) {
            holder.cvContainer.setCardBackgroundColor(Color.parseColor("#6366F1"))
            holder.tvNombre.setTextColor(Color.WHITE)
            holder.tvNumero.setTextColor(Color.WHITE)
            holder.viewIndicator.visibility = View.VISIBLE
        } else {
            holder.cvContainer.setCardBackgroundColor(Color.WHITE)
            holder.tvNombre.setTextColor(Color.parseColor("#94A3B8"))
            holder.tvNumero.setTextColor(Color.parseColor("#1E293B"))
            holder.viewIndicator.visibility = View.INVISIBLE
        }

        holder.itemView.setOnClickListener {
            onDiaSelected(dia.fecha)
        }
    }

    override fun getItemCount() = dias.size

    fun updateDias(nuevosDias: List<DiaCalendario>) {
        dias = nuevosDias
        notifyDataSetChanged()
    }
}
