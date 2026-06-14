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
import com.example.medly_proyecto.model.Documento
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentoAdapter(
    private var documentos: List<Documento>,
    private val onItemClick: (Documento) -> Unit
) : RecyclerView.Adapter<DocumentoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivDocIcon)
        val tvTitulo: TextView = view.findViewById(R.id.tvDocTitulo)
        val tvTipo: TextView = view.findViewById(R.id.tvDocTipo)
        val tvFecha: TextView = view.findViewById(R.id.tvDocFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_documento, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc = documentos[position]
        val context = holder.itemView.context
        holder.tvTitulo.text = doc.titulo
        holder.tvTipo.text = doc.tipo
        holder.tvFecha.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(doc.fecha))
        
        if (doc.imagenUri.isNotEmpty()) {
            val uri = Uri.parse(doc.imagenUri)
            val isPdf = doc.imagenUri.lowercase().contains(".pdf") || 
                        context.contentResolver.getType(uri) == "application/pdf"

            if (isPdf) {
                holder.ivIcon.setImageResource(R.drawable.imagenpdf)
                holder.ivIcon.scaleType = ImageView.ScaleType.FIT_CENTER
                holder.ivIcon.setPadding(10, 10, 10, 10)
            } else {
                val iconRes = when (doc.tipo.uppercase()) {
                    "RECETA" -> R.drawable.archivo
                    "CITA" -> R.mipmap.watch
                    "EXAMEN" -> R.drawable.examen
                    else -> R.drawable.archivo
                }
                holder.ivIcon.setImageResource(iconRes)
                holder.ivIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                holder.ivIcon.setPadding(0, 0, 0, 0)
            }
        } else {
            holder.ivIcon.setImageResource(R.drawable.archivo)
            holder.ivIcon.scaleType = ImageView.ScaleType.CENTER_CROP
            holder.ivIcon.setPadding(0, 0, 0, 0)
        }
        
        holder.itemView.setOnClickListener { onItemClick(doc) }
    }

    override fun getItemCount() = documentos.size

    fun getDocumentoAt(position: Int): Documento {
        return documentos[position]
    }

    fun actualizarLista(nuevaLista: List<Documento>) {
        documentos = nuevaLista
        notifyDataSetChanged()
    }
}
