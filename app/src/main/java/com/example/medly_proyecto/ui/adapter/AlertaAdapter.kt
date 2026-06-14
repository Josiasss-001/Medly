package com.example.medly_proyecto.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.AlertaHogar
import com.example.medly_proyecto.model.TipoAlerta

class AlertaAdapter(private val onAlertaClick: (AlertaHogar) -> Unit) :
    ListAdapter<AlertaHogar, AlertaAdapter.AlertaViewHolder>(AlertaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alerta_home, parent, false)
        return AlertaViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertaViewHolder, position: Int) {
        val alerta = getItem(position)
        holder.bind(alerta, onAlertaClick)
    }

    class AlertaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivAlertaIcon)
        private val tvTitulo: TextView = itemView.findViewById(R.id.tvAlertaTitulo)
        private val tvMensaje: TextView = itemView.findViewById(R.id.tvAlertaMensaje)

        fun bind(alerta: AlertaHogar, onClick: (AlertaHogar) -> Unit) {
            tvTitulo.text = alerta.titulo
            tvMensaje.text = alerta.mensaje
            ivIcon.setImageResource(alerta.iconoRes)
            
            itemView.setOnClickListener { onClick(alerta) }
        }
    }

    class AlertaDiffCallback : DiffUtil.ItemCallback<AlertaHogar>() {
        override fun areItemsTheSame(oldItem: AlertaHogar, newItem: AlertaHogar): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AlertaHogar, newItem: AlertaHogar): Boolean =
            oldItem == newItem
    }
}
