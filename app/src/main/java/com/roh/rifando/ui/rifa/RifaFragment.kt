package com.roh.rifando.ui.rifa

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.roh.rifando.data.model.NumeroRifa
import com.roh.rifando.R
import com.roh.rifando.data.local.RifaPrefs
import com.roh.rifando.data.model.StatusNumero
import com.roh.rifando.databinding.FragmentRifaBinding
import com.roh.rifando.data.model.Rifa
import com.roh.rifando.adapters.NumeroAdapter
import com.roh.rifando.adapters.SpinnerFiltroAdapter

class RifaFragment : Fragment() {

    private var _binding: FragmentRifaBinding? = null
    private val binding get() = _binding!!

    private lateinit var rifa: Rifa
    private lateinit var adapter: NumeroAdapter
    private lateinit var numerosOriginais: MutableList<NumeroRifa>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRifaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerNumeros.visibility = View.GONE
        binding.layoutRifaInfo.visibility = View.GONE


        binding.root.postDelayed({

            rifa = RifaFragmentArgs.fromBundle(requireArguments()).rifa

            with(binding) {
                textTitulo.text = "Título: ${rifa.titulo}"
                textQtdNumeros.text = "Qtd. números: ${rifa.qtdNumeros}"
                textValorNumero.text = "Valor da rifa: R$ %.2f".format(rifa.valorNumero)
                textValorFinal.text = "Valor final: R$ %.2f".format(rifa.qtdNumeros * rifa.valorNumero)
                textDataSorteio.text = if (!rifa.dataSorteio.isNullOrBlank()) {
                    textDataSorteio.visibility = View.VISIBLE
                    "Data do sorteio: ${rifa.dataSorteio}"
                } else {
                    textDataSorteio.visibility = View.GONE
                    ""
                }
            }

            val numeros = if (rifa.numeros.isNotEmpty()) {
                rifa.numeros.toMutableList()
            } else {
                val novosNumeros = MutableList(rifa.qtdNumeros) {
                    NumeroRifa(numero = it, status = StatusNumero.LIVRE)
                }
                rifa.numeros = novosNumeros
                novosNumeros.toMutableList()
            }

            numerosOriginais = numeros.toMutableList()

            adapter = NumeroAdapter(numeros) { qtdSelecionados ->
                binding.fabEditarSelecionados.visibility =
                    if (qtdSelecionados > 0) View.VISIBLE else View.GONE
            }.apply {
                onEditarSelecionados = { selecionados ->
                    if (selecionados.all { it.status == StatusNumero.LIVRE }) {
                        abrirDialogoEdicao(selecionados, this)
                    }
                }
                onEditarIndividual = { numero ->
                    abrirDialogoEdicao(listOf(numero), this)
                }
            }

            binding.recyclerNumeros.apply {
                layoutManager = GridLayoutManager(requireContext(), 5)
                adapter = this@RifaFragment.adapter
            }

            val filtros = listOf("Todos", "Pagos", "Pendentes", "Livres")
            val spinnerAdapter = SpinnerFiltroAdapter(requireContext(), filtros)
            binding.spinnerFiltro.adapter = spinnerAdapter


            binding.spinnerFiltro.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, position: Int, id: Long
                ) {
                    val filtrados = when (position) {
                        0 -> numerosOriginais
                        1 -> numerosOriginais.filter { it.status == StatusNumero.PAGO }
                        2 -> numerosOriginais.filter { it.status == StatusNumero.PENDENTE }
                        3 -> numerosOriginais.filter { it.status == StatusNumero.LIVRE }
                        else -> numerosOriginais
                    }
                    adapter.atualizarLista(filtrados)
                    atualizarResumoStatus()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            binding.fabEditarSelecionados.setOnClickListener {
                val selecionados = adapter.getSelecionados()
                if (selecionados.isEmpty()) return@setOnClickListener

                val todosLivres = selecionados.all { it.status == StatusNumero.LIVRE }
                val todosPreenchidos = selecionados.all { it.status != StatusNumero.LIVRE }

                when {
                    todosLivres -> abrirDialogoEdicao(selecionados, adapter)
                    todosPreenchidos -> abrirDialogoConfirmacaoExclusao(selecionados)
                    else -> Toast.makeText(
                        requireContext(),
                        "Selecione somente números livres para editar ou somente preenchidos para liberar.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            (requireActivity() as AppCompatActivity).supportActionBar?.title = rifa.titulo

            val menuHost: MenuHost = requireActivity()
            menuHost.addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_criar_rifa, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_salvar -> {
                            salvarRifa(true)
                            true
                        }
                        R.id.action_compartilhar_livres -> {
                            compartilharNumerosLivres()
                            true
                        }
                        else -> false
                    }
                }
            }, viewLifecycleOwner, Lifecycle.State.RESUMED)

            binding.progressBar.visibility = View.GONE
            binding.recyclerNumeros.visibility = View.VISIBLE
            binding.layoutRifaInfo.visibility = View.VISIBLE
            binding.flowLegenda.visibility = View.VISIBLE
            binding.spinnerFiltro.visibility = View.VISIBLE

            atualizarResumoStatus()

        }, 300)


    }

    private fun atualizarResumoStatus() {
        val pago = numerosOriginais.count { it.status == StatusNumero.PAGO }
        val pendente = numerosOriginais.count { it.status == StatusNumero.PENDENTE }
        val livre = numerosOriginais.count { it.status == StatusNumero.LIVRE }

        binding.textContadorPago.text = "$pago"
        binding.textContadorPendente.text = "$pendente"
        binding.textContadorLivre.text = "$livre"
    }

    private fun salvarRifa(mostrarMensagem: Boolean = false) {
        val listaRifas = RifaPrefs.listarRifas(requireContext())
        val atualizada = listaRifas.filter { it.id != rifa.id }.toMutableList()
        atualizada.add(rifa)
        RifaPrefs.salvarRifas(requireContext(), atualizada)
        if (mostrarMensagem) {
            Toast.makeText(requireContext(), "Rifa salva com sucesso", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirDialogoEdicao(numeros: List<NumeroRifa>, adapter: NumeroAdapter) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_info_comprador, null)
        val textNumeroSelecionado = dialogView.findViewById<TextView>(R.id.textNumeroSelecionado)
        val editNome = dialogView.findViewById<EditText>(R.id.editNome)
        val editTelefone = dialogView.findViewById<EditText>(R.id.editTelefone)
        val editEndereco = dialogView.findViewById<EditText>(R.id.editEndereco)
        val checkPago = dialogView.findViewById<CheckBox>(R.id.checkPago)

        aplicarMascaraTelefone(editTelefone)

        textNumeroSelecionado.text = if (numeros.size == 1) {
            "Número: ${numeros.first().numero}"
        } else {
            "${numeros.size} números selecionados"
        }

        if (numeros.size == 1) {
            val numero = numeros.first()
            editNome.setText(numero.nome.orEmpty())
            editTelefone.setText(numero.telefone.orEmpty())
            editEndereco.setText(numero.endereco.orEmpty())
            checkPago.isChecked = numero.status == StatusNumero.PAGO
        }

        val dialog = BottomSheetDialog(requireContext()).apply {
            setContentView(dialogView)
            show()
        }

        dialogView.findViewById<Button>(R.id.btnSalvar).setOnClickListener {
            val nome = editNome.text?.toString()?.trim().orEmpty()
            val telefone = editTelefone.text?.toString()?.trim().orEmpty()
            val endereco = editEndereco.text?.toString()?.trim().orEmpty()
            val pago = checkPago.isChecked

            if (nome.isEmpty() && telefone.isEmpty() && endereco.isEmpty() && !pago) {
                dialog.dismiss()
                return@setOnClickListener
            }

            if (pago && nome.isEmpty()) {
                Toast.makeText(context, "Informe o nome para marcar como PAGO", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            numeros.forEach { n ->
                if (nome.isNotEmpty()) n.nome = nome
                if (telefone.isNotEmpty()) n.telefone = telefone
                if (endereco.isNotEmpty()) n.endereco = endereco
                n.status = when {
                    pago -> StatusNumero.PAGO
                    nome.isNotEmpty() || telefone.isNotEmpty() || endereco.isNotEmpty() -> StatusNumero.PENDENTE
                    else -> n.status
                }
            }

            salvarRifa()
            adapter.notifyDataSetChanged()
            adapter.limparSelecao()
            binding.fabEditarSelecionados.hide()
            dialog.dismiss()
            atualizarResumoStatus()
        }

        dialogView.findViewById<Button>(R.id.btnLiberar).setOnClickListener {
            numeros.forEach {
                it.nome = null
                it.telefone = null
                it.endereco = null
                it.status = StatusNumero.LIVRE
            }
            salvarRifa()
            adapter.notifyDataSetChanged()
            adapter.limparSelecao()
            binding.fabEditarSelecionados.hide()
            dialog.dismiss()
            atualizarResumoStatus()
        }
    }

    private fun abrirDialogoConfirmacaoExclusao(numeros: List<NumeroRifa>) {
        AlertDialog.Builder(requireContext())
            .setTitle("Liberar números")
            .setMessage("Deseja realmente liberar ${numeros.size} números selecionados? Os dados do comprador serão excluídos.")
            .setPositiveButton("Liberar") { dialog, _ ->
                numeros.forEach {
                    it.nome = null
                    it.telefone = null
                    it.endereco = null
                    it.status = StatusNumero.LIVRE
                }
                salvarRifa()
                adapter.notifyDataSetChanged()
                adapter.limparSelecao()
                binding.fabEditarSelecionados.hide()
                dialog.dismiss()
                atualizarResumoStatus()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun compartilharNumerosLivres() {
        val numerosLivres = rifa.numeros.filter { it.status == StatusNumero.LIVRE }
        if (numerosLivres.isEmpty()) {
            Toast.makeText(requireContext(), "Nenhum número livre para compartilhar.", Toast.LENGTH_SHORT).show()
            return
        }
        val numerosStr = numerosLivres.joinToString(separator = ", ") { String.format("%02d", it.numero) }
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Números livres da rifa '${rifa.titulo}': $numerosStr")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, "Compartilhar números livres"))
    }

    private fun aplicarMascaraTelefone(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private val mask = "(##) #####-####"
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) {
                    isUpdating = false
                    return
                }
                val str = s.toString().filter { it.isDigit() }
                var formatted = ""
                var i = 0
                for (m in mask) {
                    if (m != '#' && i < str.length) {
                        formatted += m
                    } else if (i < str.length) {
                        formatted += str[i]
                        i++
                    }
                }
                isUpdating = true
                editText.setText(formatted)
                editText.setSelection(formatted.length)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
