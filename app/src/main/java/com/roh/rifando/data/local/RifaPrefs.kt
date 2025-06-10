package com.roh.rifando.data.local

import android.content.Context
import com.google.gson.Gson
import com.roh.rifando.data.model.Rifa

object RifaPrefs {

    private const val PREFS_NAME = "rifas"
    private const val KEY_LISTA_RIFAS = "lista_rifas"

    fun listarRifas(context: Context): MutableList<Rifa> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LISTA_RIFAS, null)
        return if (!json.isNullOrEmpty()) {
            Gson().fromJson(json, Array<Rifa>::class.java).toMutableList()
        } else {
            mutableListOf()
        }
    }

    fun salvarRifas(context: Context, rifas: List<Rifa>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(rifas)
        prefs.edit().putString(KEY_LISTA_RIFAS, json).apply()
    }

    fun removerRifa(context: Context, id: String) {
        val lista = listarRifas(context)
        lista.removeAll { it.id == id }
        salvarRifas(context, lista)
    }
}
