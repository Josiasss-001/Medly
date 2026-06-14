package com.example.medly_proyecto.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.viewmodel.EventoTratamiento

class DosisIndividualAdapter(
    private val dosis: List<EventoTratamiento>,
    private val onDosisClick: (EventoTratamiento, Boolean) -> Unit
) : RecyclerView.Adapter<DosisIndividualAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvDoseTime)
        val lavStatus: LottieAnimationView = view.findViewById(R.id.lavDoseStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dosis_individual, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dosis[position]
        holder.tvTime.text = item.hora
        
        // 0.17f es el estado verde en tu checkbox.json (frame 30 de 177 aprox)
        holder.lavStatus.progress = if (item.completado) 0.17f else 0f
        
        holder.lavStatus.setOnClickListener {
            val nuevoEstado = !item.completado
            
            holder.lavStatus.cancelAnimation()
            if (nuevoEstado) {
                // Animar de Gris a Verde (0% a 17%)
                holder.lavStatus.setMinAndMaxProgress(0f, 0.17f)
            } else {
                // Animar de Verde a Gris (usando el tramo 51% - 68% del JSON para suavidad)
                holder.lavStatus.setMinAndMaxProgress(0.51f, 0.68f)
            }
            holder.lavStatus.speed = 2.0f
            holder.lavStatus.playAnimation()
            
            onDosisClick(item, nuevoEstado)
        }
    }

    override fun getItemCount() = dosis.size
}
