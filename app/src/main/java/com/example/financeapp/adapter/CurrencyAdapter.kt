package com.example.financeapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.financeapp.enums.Currency

class CurrencyAdapter(context: Context, resource: Int, private val currencies: List<Currency>) :
    ArrayAdapter<Currency>(context, resource, currencies) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = convertView ?: inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)

        val currencyNameAndSymbol = "${currencies[position].code} (${currencies[position].symbol})"
        view.findViewById<TextView>(android.R.id.text1).text = currencyNameAndSymbol

        return view
    }
}
