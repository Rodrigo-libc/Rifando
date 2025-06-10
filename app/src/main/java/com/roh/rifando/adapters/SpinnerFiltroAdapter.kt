package com.roh.rifando.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.roh.rifando.R

class SpinnerFiltroAdapter(
    private val context: Context,
    private val items: List<String>
) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = LayoutInflater.from(context).inflate(R.layout.spinner_item_selected, parent, false)
        val text = view.findViewById<TextView>(R.id.spinnerText)
        text.text = items[position]
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = LayoutInflater.from(context).inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)
        val text = view.findViewById<TextView>(android.R.id.text1)
        text.text = items[position]
        return view
    }
}

