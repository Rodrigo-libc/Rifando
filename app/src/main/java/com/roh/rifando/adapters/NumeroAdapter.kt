package com.roh.rifando.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.roh.rifando.data.model.NumeroRifa
import com.roh.rifando.R
import com.roh.rifando.data.model.StatusNumero

class NumeroAdapter(
    private val numeros: MutableList<NumeroRifa>,
    private val onSelecaoAlterada: (Int) -> Unit
) : RecyclerView.Adapter<NumeroAdapter.NumeroViewHolder>() {

    private val numerosSelecionados = mutableListOf<NumeroRifa>()
    fun getNumeros(): List<NumeroRifa> = numeros.toList()

    // Duas flags para os modos de seleção
    var emModoSelecaoEdicaoLivres = false
        private set
    var emModoSelecaoExclusaoPreenchidos = false
        private set

    var onEditarSelecionados: ((List<NumeroRifa>) -> Unit)? = null
    var onEditarIndividual: ((NumeroRifa) -> Unit)? = null
    var onExcluirSelecionados: ((List<NumeroRifa>) -> Unit)? = null

    class NumeroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textNumero: TextView = itemView.findViewById(R.id.textNumero)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumeroViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_numero, parent, false)
        return NumeroViewHolder(view)
    }

    override fun onBindViewHolder(holder: NumeroViewHolder, position: Int) {
        val numero = numeros[position]
        holder.textNumero.text = "%02d".format(numero.numero)

        val context = holder.itemView.context
        val cor = when (numero.status) {
            StatusNumero.LIVRE -> ContextCompat.getColor(context, R.color.status_livre)
            StatusNumero.PAGO -> ContextCompat.getColor(context, R.color.status_pago)
            StatusNumero.PENDENTE -> ContextCompat.getColor(context, R.color.status_pendente)
        }

        val corFinal = if (numerosSelecionados.contains(numero)) {
            ContextCompat.getColor(context, R.color.status_selecionado)
        } else {
            cor
        }

        holder.textNumero.setBackgroundColor(corFinal)

        // Clique longo
        holder.itemView.setOnLongClickListener {
            when (numero.status) {
                StatusNumero.LIVRE -> {
                    if (!emModoSelecaoEdicaoLivres && !emModoSelecaoExclusaoPreenchidos) {
                        // Entrar em modo seleção edição para LIVRES
                        emModoSelecaoEdicaoLivres = true
                        numerosSelecionados.clear()
                        numerosSelecionados.add(numero)
                        notifyItemChanged(position)
                        onSelecaoAlterada(numerosSelecionados.size)
                    }
                    true
                }
                StatusNumero.PAGO, StatusNumero.PENDENTE -> {
                    if (!emModoSelecaoEdicaoLivres && !emModoSelecaoExclusaoPreenchidos) {
                        // Entrar em modo seleção exclusão para preenchidos
                        emModoSelecaoExclusaoPreenchidos = true
                        numerosSelecionados.clear()
                        numerosSelecionados.add(numero)
                        notifyItemChanged(position)
                        onSelecaoAlterada(numerosSelecionados.size)
                    } else {
                        // Se já em modo seleção, talvez ignore ou outra ação
                    }
                    true
                }
            }
        }

        // Clique simples
        holder.itemView.setOnClickListener {
            when {
                emModoSelecaoEdicaoLivres && numero.status == StatusNumero.LIVRE -> {
                    toggleSelecao(numero, position)
                }
                emModoSelecaoExclusaoPreenchidos && (numero.status == StatusNumero.PAGO || numero.status == StatusNumero.PENDENTE) -> {
                    toggleSelecao(numero, position)
                }
                !emModoSelecaoEdicaoLivres && !emModoSelecaoExclusaoPreenchidos -> {
                    // Fora do modo seleção
                    if (numero.status == StatusNumero.LIVRE) {
                        onEditarSelecionados?.invoke(listOf(numero))
                    } else {
                        onEditarIndividual?.invoke(numero)
                    }
                }
            }
        }
    }

    private fun toggleSelecao(numero: NumeroRifa, position: Int) {
        if (numerosSelecionados.contains(numero)) {
            numerosSelecionados.remove(numero)
            if (numerosSelecionados.isEmpty()) {
                // Sai dos modos de seleção se vazio
                emModoSelecaoEdicaoLivres = false
                emModoSelecaoExclusaoPreenchidos = false
            }
        } else {
            numerosSelecionados.add(numero)
        }
        notifyItemChanged(position)
        onSelecaoAlterada(numerosSelecionados.size)
    }

    fun getSelecionados() = numerosSelecionados.toList()

    override fun getItemCount() = numeros.size

    fun limparSelecao() {
        numerosSelecionados.clear()
        emModoSelecaoEdicaoLivres = false
        emModoSelecaoExclusaoPreenchidos = false
        notifyDataSetChanged()
        onSelecaoAlterada(0)
    }

    fun atualizarLista(novaLista: List<NumeroRifa>) {
        numeros.clear()
        numeros.addAll(novaLista)
        limparSelecao()
        notifyDataSetChanged()
    }
}
