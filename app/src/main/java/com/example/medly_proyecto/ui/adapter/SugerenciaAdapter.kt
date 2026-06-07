package com.example.medly_proyecto.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.Sugerencia

class SugerenciaAdapter(
    private var sugerencias: List<Sugerencia>,
    private val alHacerClick: (Sugerencia) -> Unit
) : RecyclerView.Adapter<SugerenciaAdapter.SugerenciaViewHolder>() {

    class SugerenciaViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        val icono: ImageView = vista.findViewById(R.id.ivIconoSugerencia)
        val texto: TextView = vista.findViewById(R.id.tvTextoSugerencia)
    }

    override fun onCreateViewHolder(padre: ViewGroup, viewType: Int): SugerenciaViewHolder {
        val vista = LayoutInflater.from(padre.context).inflate(R.layout.item_sugerencia, padre, false)
        return SugerenciaViewHolder(vista)
    }

    override fun onBindViewHolder(holder: SugerenciaViewHolder, position: Int) {
        val sugerencia = sugerencias[position]
        holder.texto.text = sugerencia.texto
        holder.icono.setImageResource(sugerencia.iconoRes)
        holder.itemView.setOnClickListener { alHacerClick(sugerencia) }
    }

    override fun getItemCount(): Int = sugerencias.size

    fun actualizarLista(nuevaLista: List<Sugerencia>) {
        sugerencias = nuevaLista
        notifyDataSetChanged()
    }
}
