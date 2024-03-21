package com.example.financeapp.activity.menu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.financeapp.R
import com.example.financeapp.databinding.ActivityControlledChildBinding
import com.google.firebase.auth.FirebaseAuth

class ControlledChildActivity : AppCompatActivity() {

    private lateinit var binding: ActivityControlledChildBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controlled_child)

        binding = ActivityControlledChildBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        // Get the current user from FirebaseAuth
        val currentUser = auth.currentUser

        // Check if the current user is not null
        currentUser?.let { user ->
            // Get the full name of the user
            val fullName = "${user.displayName}"
            // Set the full name to the TextView
            binding.textViewFullName.text = fullName
        }
    }
}
