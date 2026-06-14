package com.example.medly_proyecto.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.viewmodel.MedicamentoGrupo
import com.example.medly_proyecto.viewmodel.EventoTratamiento
import com.google.android.material.card.MaterialCardView

class MedicamentoGrupoAdapter(
    private var grupos: List<MedicamentoGrupo>,
    private val onDosisClick: (EventoTratamiento, Boolean) -> Unit
) : RecyclerView.Adapter<MedicamentoGrupoAdapter.GrupoViewHolder>() {

    class GrupoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMedName: TextView = view.findViewById(R.id.tvMedName)
        val tvMedFreq: TextView = view.findViewById(R.id.tvMedFreq)
        val ivExpandArrow: ImageView = view.findViewById(R.id.ivExpandArrow)
        val clGroupHeader: View = view.findViewById(R.id.clGroupHeader)
        val llDosesContainer: LinearLayout = view.findViewById(R.id.llDosesContainer)
        val rvDoses: RecyclerView = view.findViewById(R.id.rvDoses)
        val viewSideAccent: View = view.findViewById(R.id.viewSideAccent)
        val cvIconContainer: MaterialCardView = view.findViewById(R.id.cvIconContainer)
        val ivMedIcon: ImageView = view.findViewById(R.id.ivMedIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GrupoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medicamento_grupo, parent, false)
        return GrupoViewHolder(view)
    }

    override fun onBindViewHolder(holder: GrupoViewHolder, position: Int) {
        val grupo = grupos[position]
        holder.tvMedName.text = "${grupo.nombreMedicamento} ${grupo.dosis}"
        holder.tvMedFreq.text = grupo.frecuencia
        
        // Colores temáticos (Principal y Fondo Suave)
        val (accentColor, lightBgColor) = when(position % 3) {
            0 -> "#F43F5E" to "#FFF1F2" // Rosa/Rojo
            1 -> "#10B981" to "#F0FDF4" // Verde
            2 -> "#F59E0B" to "#FFFBEB" // Naranja/Ámbar
            else -> "#0EA5E9" to "#F0F9FF" // Azul
        }

        val colorInt = Color.parseColor(accentColor)
        val lightColorInt = Color.parseColor(lightBgColor)

        // Aplicar colores a los elementos del item
        holder.viewSideAccent.setBackgroundColor(colorInt)
        holder.tvMedFreq.setTextColor(colorInt)
        holder.ivMedIcon.setColorFilter(colorInt)
        holder.cvIconContainer.setCardBackgroundColor(lightColorInt)

        val doseAdapter = DosisIndividualAdapter(grupo.eventos) { evento, isChecked ->
            onDosisClick(evento, isChecked)
        }
        holder.rvDoses.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.rvDoses.adapter = doseAdapter

        actualizarVisibilidad(holder, grupo.estaExpandido)
        
        holder.clGroupHeader.setOnClickListener {
            grupo.estaExpandido = !grupo.estaExpandido
            actualizarVisibilidad(holder, grupo.estaExpandido)
        }
    }

    private fun actualizarVisibilidad(holder: GrupoViewHolder, expandido: Boolean) {
        holder.llDosesContainer.visibility = if (expandido) View.VISIBLE else View.GONE
        holder.ivExpandArrow.rotation = if (expandido) 180f else 0f
    }

    override fun getItemCount() = grupos.size

    fun updateGrupos(newGrupos: List<MedicamentoGrupo>) {
        grupos = newGrupos
        notifyDataSetChanged()
    }
}
