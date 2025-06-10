package com.roh.rifando.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.roh.rifando.R
import com.roh.rifando.data.model.Rifa

class RifaAdapter(
    private val rifas: MutableList<Rifa>,
    private val onItemClick: (Rifa) -> Unit,
    private val onDeleteClick: (Rifa) -> Unit
) : RecyclerView.Adapter<RifaAdapter.RifaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RifaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rifa, parent, false)
        return RifaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RifaViewHolder, position: Int) {
        val rifa = rifas[position]
        holder.bind(rifa)
        holder.itemView.setOnClickListener {
            onItemClick(rifa)
        }
        holder.btnExcluir.setOnClickListener {
            onDeleteClick(rifa)
        }
    }

    override fun getItemCount() = rifas.size

    fun removerRifa(rifa: Rifa) {
        val index = rifas.indexOf(rifa)
        if (index != -1) {
            rifas.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun adicionarRifa(rifa: Rifa) {
        rifas.add(rifa)
        notifyItemInserted(rifas.size - 1)
    }

    class RifaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTitulo: TextView = itemView.findViewById(R.id.textTitulo)
        val textQtdNumeros: TextView = itemView.findViewById(R.id.textQtdNumeros)
        val dataSorteioTextView: TextView = itemView.findViewById(R.id.dataSorteio)
        val textValorTotal: TextView = itemView.findViewById(R.id.textValorTotal)
        val btnExcluir: ImageButton = itemView.findViewById(R.id.btnExcluir)

        fun bind(rifa: Rifa) {
            textTitulo.text = rifa.titulo.ifBlank { "Rifa sem título" }
            textQtdNumeros.text = "Qtd números: ${rifa.qtdNumeros}"
            textValorTotal.text = "Valor total: R$ %.2f".format(rifa.qtdNumeros * rifa.valorNumero)

            if (!rifa.dataSorteio.isNullOrBlank()) {
                dataSorteioTextView.text = "Data do sorteio: ${rifa.dataSorteio}"
                dataSorteioTextView.visibility = View.VISIBLE
            } else {
                dataSorteioTextView.visibility = View.GONE
            }
        }
    }
}


