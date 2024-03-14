package com.example.financeapp.activity

import android.content.ContentValues.TAG
import android.os.Bundle
import android.view.View
import android.content.Intent
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.*
import androidx.appcompat.app.AppCompatActivity
import com.example.financeapp.R
import com.example.financeapp.databinding.ActivityRegisterBinding
import com.example.financeapp.data.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        EditTextListener()
        setupClickListener()
        setupClickableSpan()
    }

    private fun EditTextListener() {
        binding.email.setOnFocusChangeListener{_, focused->
           if(!focused){
               binding.emailContainer.helperText = validEmail()
           }
        }

        binding.dateContainer.setOnFocusChangeListener{_, focused->
            if(!focused){
                binding.dateContainer.helperText = validDate()
            }
        }
    }

    private fun validDate(): String? {
        val dateText = binding.dateOfBirth.text.toString()
        val dateRegex = Regex("""^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/([0-9]{2})$""")
        if (!dateRegex.matches(dateText)) {
            return "Invalid Date"
        }
        return null
    }


    private fun validEmail(): String? {
        val emailText = binding.email.text.toString()
        if(!Patterns.EMAIL_ADDRESS.matcher(emailText).matches())
        {
            return "Invalid Email Adress"
        }
        return null
    }

    private fun setupClickListener() {
        binding.dateOfBirth.setOnClickListener {
            showDatePicker()
        }

        binding.btnSignup.setOnClickListener{
            val firstName = binding.firstName.text.toString()
            val lastName = binding.lastName.text.toString()
            val phoneNumber = binding.phoneNumber.text.toString()
            val dateOfBirth = binding.dateOfBirth.text.toString()
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()
            val currentDateString = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())

            auth = Firebase.auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // User creation successful, update user profile
                        val user = auth.currentUser
                        val userProfileChangeRequest = UserProfileChangeRequest.Builder()
                            .setDisplayName("$firstName $lastName")
                            .build()
                        user?.updateProfile(userProfileChangeRequest)
                            ?.addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    // Profile updated successfully
                                    Log.d(TAG, "User profile updated.")
                                } else {
                                    // Failed to update profile
                                    Log.e(TAG, "Failed to update user profile.", profileTask.exception)
                                }
                            }

                        // Create User object with current date as creation date
                        val newUser = User(
                            user?.uid ?: "", // If user is null, set id to empty string
                            firstName,
                            lastName,
                            phoneNumber,
                            dateOfBirth,
                            email,
                            password,
                            currentDateString // Set current date as creation date
                        )

                        // Now you can save the newUser object to Firebase Realtime Database or Firestore
                        // For simplicity, let's assume we are using Firebase Realtime Database
                        val databaseReference = FirebaseDatabase.getInstance().reference.child("users").child(user?.uid ?: "")
                        databaseReference.setValue(newUser)
                            .addOnCompleteListener { databaseTask ->
                                if (databaseTask.isSuccessful) {
                                    Log.d(TAG, "User information saved to database.")
                                } else {
                                    Log.e(TAG, "Failed to save user information to database.", databaseTask.exception)
                                }
                            }

                        // Update UI after successful registration
                        updateUI(user)
                    } else {
                        // If user creation fails, display an error message
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateUI(null)
                    }
                }
        }
    }
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // User is signed in, you can navigate to the main activity or perform any other action
            val intent = Intent(this, AppActivity::class.java)
            startActivity(intent)
            finish() // Optional: Finish the current activity to prevent the user from navigating back
        } else {
            // User registration failed or user is null, handle the UI accordingly
            // For example, display an error message
            Toast.makeText(this, "User registration failed", Toast.LENGTH_SHORT).show()
        }
    }
    private fun setupClickableSpan() {
        val spannableString =
            SpannableString(getString(R.string.do_you_have_an_account) + " " + getString(R.string.Login))
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                navigateToLoginActivity()
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.color = resources.getColor(R.color.purple_500)
            }
        }
        spannableString.setSpan(
            clickableSpan,
            getString(R.string.Login).length + 1,
            spannableString.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.logInText.text = spannableString
        binding.logInText.movementMethod = LinkMovementMethod.getInstance()
        binding.logInText.highlightColor = resources.getColor(android.R.color.transparent)
    }

    private fun showDatePicker() {
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val builder = MaterialDatePicker.Builder.datePicker()
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            // Format the selected date
            val formattedDate = dateFormat.format(Date(selection))
            // Set the formatted date to the EditText
            binding.dateOfBirth.setText(formattedDate)
        }
        picker.show(supportFragmentManager, picker.toString())
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}
