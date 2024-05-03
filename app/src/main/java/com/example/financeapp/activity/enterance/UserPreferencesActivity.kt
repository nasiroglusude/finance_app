package com.example.financeapp.activity.enterance

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.financeapp.activity.menu.MenuActivity
import com.example.financeapp.adapter.CurrencyAdapter
import com.example.financeapp.model.User
import com.example.financeapp.databinding.ActivityUserPreferencesBinding
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

class UserPreferencesActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var binding: ActivityUserPreferencesBinding
    private lateinit var adapter: CurrencyAdapter
    private var selectedCurrencyPosition: Int = 0
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var isDropdownVisible = false
    private lateinit var database: DatabaseReference

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUserPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = FirebaseDatabase.getInstance().reference


        setSpinner()
        setListeners()
    }

    private fun setListeners(){
        binding.btnContinue.setOnClickListener {
            launch {
                savePreferencesToFirebase()
            }
        }
        binding.materialSpinner.setOnClickListener {
            if (isDropdownVisible) {
                // If dropdown is visible, dismiss it
                binding.materialSpinner.dismissDropDown()
                isDropdownVisible = false
            } else {
                // If dropdown is not visible, show it
                binding.materialSpinner.showDropDown()
                isDropdownVisible = true
            }
        }

        // Optional: Set a click listener to get the selected currency
        binding.materialSpinner.setOnItemClickListener { _, _, position, _ ->
            selectedCurrencyPosition = position
            // Do something with the selected currency, such as displaying it
        }
    }

    private fun setSpinner(){
        val currencies = Currency.entries.toList()
        // Create the adapter with your custom layout and the Currency array
        adapter = CurrencyAdapter(this, android.R.layout.simple_spinner_dropdown_item, currencies)

        binding.materialSpinner.setAdapter(adapter)
        // Set the initial selection to the first item in the list
        binding.materialSpinner.setText(adapter.getItem(0).toString(), false)
    }

    private fun savePreferencesToFirebase() {
        val selectedCurrency = Currency.entries[selectedCurrencyPosition].name
        val startingBudget = binding.startingBudget.text.toString()

        if (currentUser != null) {
            val userId = currentUser.uid
            val userRef = database.child("users").child(userId)

            // Fetch the user's data
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val user = dataSnapshot.getValue(User::class.java)
                        user?.let {
                            // Update the user's currency and balance
                            user.balance = startingBudget
                            user.currency = selectedCurrency

                            // Save the updated user object
                            userRef.setValue(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this@UserPreferencesActivity, "Preferences saved successfully", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@UserPreferencesActivity, ProfilePhotoActivity::class.java)
                                    startActivity(intent)
                                    finish() // Optionally: Finish the current activity to prevent user from going back
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this@UserPreferencesActivity, "Failed to save preferences: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        // User data not found
                        Toast.makeText(this@UserPreferencesActivity, "User data not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Error handling
                    Toast.makeText(this@UserPreferencesActivity, "Failed to save preferences: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

}
