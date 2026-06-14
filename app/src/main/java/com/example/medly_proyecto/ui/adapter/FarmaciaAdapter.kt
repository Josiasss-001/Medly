package com.example.medly_proyecto.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.Farmacia

class FarmaciaAdapter(
    private var farmacias: List<Farmacia>,
    private val onItemClick: (Farmacia) -> Unit
) : RecyclerView.Adapter<FarmaciaAdapter.FarmaciaViewHolder>() {

    class FarmaciaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvDireccion: TextView = view.findViewById(R.id.tvDireccion)
        val tvDistancia: TextView = view.findViewById(R.id.tvDistancia)
        val ivTipoLugar: ImageView = view.findViewById(R.id.ivTipoLugar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FarmaciaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_farmacia, parent, false)
        return FarmaciaViewHolder(view)
    }

    override fun onBindViewHolder(holder: FarmaciaViewHolder, position: Int) {
        val farmacia = farmacias[position]
        holder.tvNombre.text = farmacia.nombre
        holder.tvDireccion.text = farmacia.direccion
        holder.tvDistancia.text = farmacia.distancia
        
        // Cambiar icono según el tipo
        if (farmacia.tipo == "CESFAM") {
            holder.ivTipoLugar.setImageResource(R.drawable.centrosalud)
        } else {
            holder.ivTipoLugar.setImageResource(R.drawable.farmacia)
        }
        
        holder.itemView.setOnClickListener { onItemClick(farmacia) }
    }

    override fun getItemCount() = farmacias.size

    fun getLista(): List<Farmacia> = farmacias

    fun updateList(newList: List<Farmacia>) {
        farmacias = newList.sortedBy { it.distanciaMetros }
        notifyDataSetChanged()
    }
}
