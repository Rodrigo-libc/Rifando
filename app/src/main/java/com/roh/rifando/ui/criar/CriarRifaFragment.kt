package com.roh.rifando.ui.criar

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.roh.rifando.data.local.RifaPrefs
import com.roh.rifando.databinding.FragmentCriarRifaBinding
import com.roh.rifando.data.model.Rifa
import java.util.Calendar

class CriarRifaFragment : Fragment() {

    private var _binding: FragmentCriarRifaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCriarRifaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Limitar quantidade entre 1 e 1000
        binding.edtQuantidadeNumeros.filters = arrayOf(InputFilterMinMax(1, 1000))

        // Mostrar/ocultar campo de data conforme switch
        binding.switchDataSorteio.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutDataSorteio.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                binding.editTextDataSorteio.text?.clear()
            }
        }

        // Abre o DatePicker ao clicar no campo de data
        binding.editTextDataSorteio.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val dataFormatada = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                    binding.editTextDataSorteio.setText(dataFormatada)
                }, year, month, day)

            datePickerDialog.show()
        }

        // Máscara para valor monetário R$ 0,00 até R$ 100,00
        binding.edtValorNumero.addTextChangedListener(object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    binding.edtValorNumero.removeTextChangedListener(this)

                    val cleanString = s.toString().replace("[R$,.\\s]".toRegex(), "")
                    val parsed = cleanString.toDoubleOrNull() ?: 0.0
                    val valor = parsed / 100


                    val limitado = if (valor > 100.0) 100.0 else valor

                    val formatted = "R$ %.2f".format(limitado).replace('.', ',')

                    current = formatted
                    binding.edtValorNumero.setText(formatted)
                    binding.edtValorNumero.setSelection(formatted.length)

                    binding.edtValorNumero.addTextChangedListener(this)
                }
            }
        })

        binding.saveRifa.setOnClickListener {
            val titulo = binding.edtNomeCartela.text.toString().trim()
            val qtdNumeros = binding.edtQuantidadeNumeros.text.toString().toIntOrNull() ?: 0

            val valorTexto = binding.edtValorNumero.text.toString()
                .replace("[R$\\s.]".toRegex(), "")
                .replace(",", ".")
            val valorNumero = valorTexto.toDoubleOrNull() ?: 0.0

            val dataSorteio = if (binding.switchDataSorteio.isChecked)
                binding.editTextDataSorteio.text.toString().trim()
            else null

            // Validações
            if (titulo.isEmpty()) {
                binding.edtNomeCartela.error = "Informe o título da rifa"
                return@setOnClickListener
            }
            if (qtdNumeros <= 0 || qtdNumeros > 1000) {
                binding.edtQuantidadeNumeros.error = "Informe uma quantidade válida (1 a 1000)"
                return@setOnClickListener
            }
            if (valorNumero <= 0 || valorNumero > 100.0) {
                binding.edtValorNumero.error = "Informe um valor válido (até R$ 100,00)"
                return@setOnClickListener
            }
            if (binding.switchDataSorteio.isChecked && dataSorteio.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Informe a data do sorteio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Criar objeto rifa
            val rifa = Rifa(
                titulo = titulo,
                qtdNumeros = qtdNumeros,
                valorNumero = valorNumero,
                dataSorteio = dataSorteio
            )

            // Salvar rifa
            val listaAtual = RifaPrefs.listarRifas(requireContext()).toMutableList()
            val tituloDuplicado = listaAtual.any { it.titulo.equals(titulo, ignoreCase = true) }
            if (tituloDuplicado) {
                binding.edtNomeCartela.error = "Já existe uma rifa com esse título"
                return@setOnClickListener
            }
            listaAtual.add(rifa)
            RifaPrefs.salvarRifas(requireContext(), listaAtual)

            // Navegar para o fragmento da rifa criada
            val action = CriarRifaFragmentDirections.actionCriarRifaFragmentToRifaFragment(rifa)
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// InputFilter para limitar valores numéricos no EditText
class InputFilterMinMax(private val min: Int, private val max: Int) : InputFilter {
    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        try {
            val input = (dest.toString().substring(0, dstart) +
                    source.toString().substring(start, end) +
                    dest.toString().substring(dend)).toInt()
            if (isInRange(min, max, input)) {
                return null
            }
        } catch (e: NumberFormatException) {
            // Ignora erro de número inválido
        }
        return ""
    }

    private fun isInRange(a: Int, b: Int, c: Int): Boolean {
        return if (b > a) c in a..b else c in b..a
    }
}
