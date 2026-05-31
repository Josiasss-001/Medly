package com.example.medly_proyecto.ui.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medly_proyecto.R
import com.example.medly_proyecto.model.Receta
import com.google.android.material.imageview.ShapeableImageView
import java.text.SimpleDateFormat
import java.util.*

class RecetasAdapter(
    private var recetas: List<Receta>,
    private val onVerDetallesClick: (Receta) -> Unit
) : RecyclerView.Adapter<RecetasAdapter.RecetaViewHolder>() {

    class RecetaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgRecipe: ShapeableImageView = view.findViewById(R.id.itemRecipeImage)
        val tvTitle: TextView = view.findViewById(R.id.itemRecipeTitle)
        val tvDate: TextView = view.findViewById(R.id.itemRecipeDate)
        val btnVerDetalles: View = view.findViewById(R.id.btnVerDetallesItem)
        val rootCard: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecetaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_receta, parent, false)
        return RecetaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecetaViewHolder, position: Int) {
        val receta = recetas[position]
        holder.tvTitle.text = receta.nombreMedicamento
        
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.tvDate.text = "Fecha: ${sdf.format(Date(receta.fechaCaptura))}"

        if (receta.imagenUri.isNotEmpty()) {
            try {
                holder.imgRecipe.setImageURI(Uri.parse(receta.imagenUri))
            } catch (e: Exception) {
                holder.imgRecipe.setImageResource(R.mipmap.fondo)
            }
        } else {
            holder.imgRecipe.setImageResource(R.mipmap.fondo)
        }

        val listener = View.OnClickListener { onVerDetallesClick(receta) }
        holder.btnVerDetalles.setOnClickListener(listener)
        holder.rootCard.setOnClickListener(listener)
    }

    override fun getItemCount() = recetas.size

    fun getRecetaAt(position: Int): Receta {
        return recetas[position]
    }

    fun updateRecetas(newRecetas: List<Receta>) {
        recetas = newRecetas
        notifyDataSetChanged()
    }
}
