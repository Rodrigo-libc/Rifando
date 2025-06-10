package com.roh.rifando.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.roh.rifando.data.local.RifaPrefs
import com.roh.rifando.databinding.FragmentHomeBinding
import com.roh.rifando.data.model.Rifa
import com.roh.rifando.adapters.RifaAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: RifaAdapter
    private var rifasList = mutableListOf<Rifa>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Carrega rifas do storage como MutableList
        rifasList = RifaPrefs.listarRifas(requireContext())

        if (rifasList.isEmpty()) {
            binding.textViewEmpty.visibility = View.VISIBLE
            binding.rvRifas.visibility = View.GONE
        } else {
            binding.textViewEmpty.visibility = View.GONE
            binding.rvRifas.visibility = View.VISIBLE
        }

        // Configura adapter com clique e exclusão
        adapter = RifaAdapter(
            rifasList,
            onItemClick = { rifaSelecionada ->
                val action = HomeFragmentDirections.actionHomeFragmentToRifaFragment(rifaSelecionada)
                findNavController().navigate(action)
            },
            onDeleteClick = { rifaParaExcluir ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Excluir Rifa")
                    .setMessage("Tem certeza que deseja excluir a rifa \"${rifaParaExcluir.titulo}\"?")
                    .setPositiveButton("Excluir") { _, _ ->
                        RifaPrefs.removerRifa(requireContext(), rifaParaExcluir.id)
                        adapter.removerRifa(rifaParaExcluir)

                        // Atualiza a lista local
                        rifasList = rifasList.filter { it.id != rifaParaExcluir.id }.toMutableList()

                        // Verifica se está vazia após exclusão
                        if (rifasList.isEmpty()) {
                            binding.textViewEmpty.visibility = View.VISIBLE
                            binding.rvRifas.visibility = View.GONE
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )

        binding.rvRifas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRifas.adapter = adapter
        // Observa nova rifa vinda do CriarRifaFragment
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle
        savedStateHandle?.getLiveData<Rifa>("novaRifa")?.observe(viewLifecycleOwner) { novaRifa ->
            adapter.adicionarRifa(novaRifa)         // adiciona no adapter
            rifasList.add(novaRifa)                // adiciona na lista local

            binding.textViewEmpty.visibility = View.GONE
            binding.rvRifas.visibility = View.VISIBLE

            savedStateHandle.remove<Rifa>("novaRifa") // remove após usar
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
