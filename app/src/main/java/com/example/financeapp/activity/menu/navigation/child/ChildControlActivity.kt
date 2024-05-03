package com.example.financeapp.activity.menu.navigation.child

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.financeapp.R
import com.example.financeapp.adapter.ExpenseBudgetAdapter
import com.example.financeapp.model.Budget
import com.example.financeapp.databinding.ActivityChildControlBinding
import com.example.financeapp.databinding.DialogPocketMoneyBinding
import com.example.financeapp.enums.Currency
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class ChildControlActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var binding: ActivityChildControlBinding
    private lateinit var dialogBinding: DialogPocketMoneyBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildControlBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val childId = intent.getStringExtra("childId")

        binding.btnAddPocketMoney.setOnClickListener {
            println("You have clicked")
            showAddPocketMoneyDialog()
        }

        if (childId != null){
            launch{
                setChildAttributes(childId)
            }
        }
    }

    private fun setChildAttributes(childId: String){
        fetchAllChildAttributes(childId) { attributes ->
            if (attributes != null) {
                // Access individual attributes
                val firstName = attributes["firstName"] as? String
                val childBalance = attributes["balance"] as? String
                val childCurrency = attributes["currency"] as String
                val currencySymbol = Currency.valueOf(childCurrency).symbol
                val formattedText = getString(R.string.spending_label, firstName)

                binding.childName.text = firstName
                binding.childAvailableBalance.text = childBalance
                binding.childCurrency.text = currencySymbol
                binding.childExpenses.text = formattedText

                // Access the expense budgets
                val expenseBudgets = attributes["expenseBudgets"] as? List<Map<String, Any?>>
                expenseBudgets?.let {
                    setUpRecyclerView(it)
                }
            } else {
                println("No attributes found for the child.")
            }
        }
    }
    private fun fetchAllChildAttributes(childId: String, callback: (Map<String, Any?>?) -> Unit) {
        val database = FirebaseDatabase.getInstance().reference
        val childRef = database.child("child").child(childId)

        childRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val attributes = mutableMapOf<String, Any?>()

                    // Fetch child's general attributes
                    for (childSnapshot in dataSnapshot.children) {
                        val attributeName = childSnapshot.key
                        val attributeValue = childSnapshot.value

                        if (attributeName != null) {
                            attributes[attributeName] = attributeValue
                        }
                    }

                    // Fetch child's budgets
                    val budgetsSnapshot = dataSnapshot.child("budgets")
                    val expenseBudgets = mutableListOf<Map<String, Any?>>()

                    // Iterate through each budget
                    for (budgetSnapshot in budgetsSnapshot.children) {
                        val budget = budgetSnapshot.getValue(Budget::class.java)
                        if (budget != null && budget.type == "Expense") {
                            // Budget is an Expense, extract title and amount
                            val expenseAttributes = mutableMapOf<String, Any?>(
                                "title" to budget.title,
                                "amount" to budget.amount,
                                "currency" to budget.currency
                            )
                            expenseBudgets.add(expenseAttributes)
                        }
                    }

                    // Add the expense budgets to the attributes map
                    attributes["expenseBudgets"] = expenseBudgets

                    callback(attributes)
                } else {
                    // No data found
                    callback(null)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Error handling
                callback(null)
            }
        })
    }

    private fun setUpRecyclerView(expenseBudgets: List<Map<String, Any?>>) {
        // Initialize RecyclerView
        val recyclerView: RecyclerView = binding.childrenList
        val expenseBudgetAdapter = ExpenseBudgetAdapter(expenseBudgets)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChildControlActivity)
            adapter = expenseBudgetAdapter
        }
    }

    private fun showAddPocketMoneyDialog() {
        dialogBinding = DialogPocketMoneyBinding.inflate(layoutInflater)
        val dialogView = dialogBinding.root

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)

        val dialog = builder.create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnAdd.setOnClickListener {
            // Perform your action here, for example, get the input amount
            val pocketMoneyInput = dialogBinding.pocketMoney
            val amount = pocketMoneyInput.text.toString().toInt()

            // Update childBalance with the entered amount
            val currentBalance = binding.childAvailableBalance.text.toString().toInt()
            val newBalance = (currentBalance + amount).toString()
            val childId = intent.getStringExtra("childId")

            // Update the child's balance in Firebase
            childId?.let { id ->
                launch{
                    updateChildBalance(id, newBalance)
                }
            }

            // Update the UI with the new balance
            binding.childAvailableBalance.text = newBalance

            // Dismiss the dialog
            dialog.dismiss()
            recreate()
        }
        dialog.show()
    }

    private fun updateChildBalance(childId: String, newBalance: String) {
        val childRef = database.child("child").child(childId)
        childRef.child("balance").setValue(newBalance)
    }
}




