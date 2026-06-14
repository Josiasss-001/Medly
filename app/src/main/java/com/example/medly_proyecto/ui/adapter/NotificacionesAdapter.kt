package com.example.medly_proyecto.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.Notificacion
import java.text.SimpleDateFormat
import java.util.*

class NotificacionesAdapter(
    private var notificaciones: List<Notificacion>
) : RecyclerView.Adapter<NotificacionesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvNotifTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvNotifDesc)
        val tvTime: TextView = view.findViewById(R.id.tvNotifTime)
        val viewUnread: View = view.findViewById(R.id.viewUnreadIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notificacion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notif = notificaciones[position]
        holder.tvTitle.text = notif.titulo
        holder.tvDesc.text = notif.descripcion
        
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        holder.tvTime.text = sdf.format(Date(notif.timestamp))
        
        holder.viewUnread.visibility = if (notif.leida) View.GONE else View.VISIBLE
    }

    override fun getItemCount() = notificaciones.size

    fun updateLista(newList: List<Notificacion>) {
        notificaciones = newList
        notifyDataSetChanged()
    }
}
