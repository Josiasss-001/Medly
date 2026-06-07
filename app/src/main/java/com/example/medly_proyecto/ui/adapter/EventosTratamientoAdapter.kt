package com.example.medly_proyecto.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.viewmodel.EventoTratamiento

class EventosTratamientoAdapter(
    private var eventos: List<EventoTratamiento>,
    private val onDosisClick: (EventoTratamiento, Boolean) -> Unit
) : RecyclerView.Adapter<EventosTratamientoAdapter.EventoViewHolder>() {

    class EventoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvEventName)
        val tvTime: TextView = view.findViewById(R.id.tvEventTime)
        val cbDosis: CheckBox = view.findViewById(R.id.cbDosisCompletada)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evento_tratamiento, parent, false)
        return EventoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        val evento = eventos[position]
        holder.tvName.text = evento.nombre
        holder.tvTime.text = evento.hora
        
        holder.cbDosis.setOnCheckedChangeListener(null)
        holder.cbDosis.isChecked = evento.completado
        
        holder.cbDosis.setOnCheckedChangeListener { _, isChecked ->
            onDosisClick(evento, isChecked)
        }
    }

    override fun getItemCount() = eventos.size

    fun updateEventos(newEventos: List<EventoTratamiento>) {
        eventos = newEventos
        notifyDataSetChanged()
    }
}
