package com.example.financeapp.activity.menu

import android.R
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.financeapp.activity.menu.navigation.HomeFragment
import com.example.financeapp.databinding.ActivityNewBudgetBinding
import com.example.financeapp.databinding.DialogColorPickerBinding
import com.example.financeapp.enums.Currency
import com.example.financeapp.data.Budget
import com.example.financeapp.databinding.ActivityMenuBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

class NewBudgetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewBudgetBinding
    private lateinit var dialogBinding: DialogColorPickerBinding
    private lateinit var menuBinding: ActivityMenuBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuBinding = ActivityMenuBinding.inflate(layoutInflater)

        binding = ActivityNewBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.colorPickerButton.setOnClickListener {
            showColorPickerDialog()
        }
        binding.saveButton.setOnClickListener {
            saveBudgetToFirebase()
        }

        val spinnerAdapter = ArrayAdapter(
            this,
            R.layout.simple_spinner_item,
            Currency.values().map { it.displayName }
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.currencySpinner.adapter = spinnerAdapter
    }

    private fun showColorPickerDialog() {
        val dialog = Dialog(this)
        dialogBinding = DialogColorPickerBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val colorPickerView = dialogBinding.colorPickerView
        val setButton = dialogBinding.setButton

        colorPickerView.setColorListener(ColorEnvelopeListener { envelope, _ ->
            val selectedColor = envelope.color
            dialogBinding.colorPreview.setBackgroundColor(selectedColor)
            binding.colorPickerButton.setBackgroundColor(selectedColor)
        })

        setButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveBudgetToFirebase() {
        val userId = firebaseAuth.currentUser?.uid
        userId?.let { uid ->
            val budgetId =
                FirebaseDatabase.getInstance().reference.child("users").child(uid)
                    .child("budgets").push().key
            val title = binding.title.text.toString()
            val amount = binding.amount.text.toString()
            val color =
                "#" + Integer.toHexString(binding.colorPickerButton.backgroundTintList?.defaultColor
                    ?: 0)
            val currency =
                Currency.values()[binding.currencySpinner.selectedItemPosition].code
            val radioButtonId = binding.budgetTypeRadioGroup.checkedRadioButtonId
            val type = if (radioButtonId == binding.incomeRadioButton.id) "Income" else "Expense"


            val budget = Budget(
                budgetId ?: "",
                title,
                amount,
                color,
                currency,
                type
            )

            FirebaseDatabase.getInstance().reference.child("users").child(uid)
                .child("budgets").child(budgetId ?: "").setValue(budget)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Budget saved successfully

                        finish()
                    } else {
                        // Handle error
                    }
                }
        }
    }
    fun navigateToActivity(context: Context, targetActivity: Class<*>) {
        val intent = Intent(context, targetActivity)
        context.startActivity(intent)
    }
}
