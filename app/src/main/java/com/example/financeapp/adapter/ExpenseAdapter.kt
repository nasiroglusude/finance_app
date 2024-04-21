package com.example.financeapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.financeapp.R

class ExpenseBudgetAdapter(private val expenseBudgets: List<Map<String, Any?>>) :
    RecyclerView.Adapter<ExpenseBudgetAdapter.ExpenseBudgetViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseBudgetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.kid_expense_item, parent, false)
        return ExpenseBudgetViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseBudgetViewHolder, position: Int) {
        val budget = expenseBudgets[position]
        val title = budget["title"] as String
        val amount = budget["amount"] as String

        holder.bind(title, amount)
    }

    override fun getItemCount(): Int {
        return expenseBudgets.size
    }

    inner class ExpenseBudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.tvExpenseTitle)
        private val amountTextView: TextView = itemView.findViewById(R.id.tvPriceTitle)

        fun bind(title: String, amount: String) {
            titleTextView.text = title
            amountTextView.text = amount
        }
    }
}
