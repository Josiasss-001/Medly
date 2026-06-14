package com.example.medly_proyecto.ui.adapter

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.CitaMedica
import java.text.SimpleDateFormat
import java.util.*

class CitasAdapter(
    private var citas: List<CitaMedica>,
    private val onVerDetallesClick: (CitaMedica) -> Unit
) : RecyclerView.Adapter<CitasAdapter.CitaViewHolder>() {

    class CitaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEspecialidad: TextView = view.findViewById(R.id.itemEspecialidad)
        val tvMedico: TextView = view.findViewById(R.id.itemMedico)
        val tvFechaHora: TextView = view.findViewById(R.id.itemFechaHora)
        val ivFoto: ImageView = view.findViewById(R.id.itemCitaIcon)
        val btnVerDetalles: View = view.findViewById(R.id.btnVerDetallesCita)
        val rootCard: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cita, parent, false)
        return CitaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        val cita = citas[position]
        val context = holder.itemView.context
        holder.tvEspecialidad.text = cita.especialidad
        holder.tvMedico.text = if (cita.nombreMedico.isNotEmpty()) "Dr. ${cita.nombreMedico}" else "Médico por asignar"
        
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("es", "ES"))
        val fechaStr = sdf.format(Date(cita.fechaCita))
        holder.tvFechaHora.text = "$fechaStr - ${cita.horaCita}"

        if (cita.imagenUri.isNotEmpty()) {
            try {
                val uri = Uri.parse(cita.imagenUri)
                val isPdf = cita.imagenUri.lowercase().contains(".pdf") || 
                            context.contentResolver.getType(uri) == "application/pdf"

                if (isPdf) {
                    holder.ivFoto.setImageResource(R.drawable.imagenpdf)
                    holder.ivFoto.scaleType = ImageView.ScaleType.FIT_CENTER
                    holder.ivFoto.setPadding(25, 25, 25, 25)
                    holder.ivFoto.setBackgroundColor(Color.parseColor("#E53935"))
                } else {
                    holder.ivFoto.setImageURI(uri)
                    holder.ivFoto.scaleType = ImageView.ScaleType.CENTER_CROP
                    holder.ivFoto.setPadding(0, 0, 0, 0)
                    holder.ivFoto.setBackgroundColor(Color.TRANSPARENT)
                }
            } catch (e: Exception) {
                holder.ivFoto.setImageResource(R.mipmap.watch)
                holder.ivFoto.scaleType = ImageView.ScaleType.CENTER_CROP
                holder.ivFoto.setPadding(0, 0, 0, 0)
                holder.ivFoto.setBackgroundColor(Color.TRANSPARENT)
            }
        } else {
            holder.ivFoto.setImageResource(R.mipmap.watch)
            holder.ivFoto.scaleType = ImageView.ScaleType.CENTER_CROP
            holder.ivFoto.setPadding(0, 0, 0, 0)
            holder.ivFoto.setBackgroundColor(Color.TRANSPARENT)
        }

        val listener = View.OnClickListener { onVerDetallesClick(cita) }
        holder.btnVerDetalles.setOnClickListener(listener)
        holder.rootCard.setOnClickListener(listener)
    }

    override fun getItemCount() = citas.size

    fun getCitaAt(position: Int): CitaMedica {
        return citas[position]
    }

    fun updateCitas(newCitas: List<CitaMedica>) {
        citas = newCitas
        notifyDataSetChanged()
    }
}
