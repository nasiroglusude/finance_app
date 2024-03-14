package com.example.financeapp.activity

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.financeapp.R
import com.example.financeapp.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        binding.btnLogin.alpha = 0.5f

        setupTextChangeListeners()
        setupClickListener()
        setupSignUpText()
    }

    private fun setupTextChangeListeners() {

        binding.email.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateLoginButtonState()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not needed
            }
        })

        binding.password.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateLoginButtonState()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not needed
            }
        })
    }

    private fun updateLoginButtonState() {
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()

        val isValidInput = email.isNotEmpty() && password.isNotEmpty()
        binding.btnLogin.isEnabled = isValidInput
        binding.btnLogin.alpha = if (isValidInput) 1.0f else 0.5f

    }

    private fun setupClickListener() {
        binding.btnLogin.setOnClickListener {

            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                // If email or password is empty, show a message and return
                Toast.makeText(
                    baseContext,
                    "Please enter both email and password.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
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
            // User sign-in failed or user is null, handle the UI accordingly
            // For example, display an error message
            Toast.makeText(this, "User sign-in failed", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupSignUpText() {
        val signUpText = findViewById<TextView>(R.id.sign_up_text)

        val spannableString = createClickableSpan()

        signUpText.apply {
            text = spannableString
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = resources.getColor(android.R.color.transparent)
        }
    }

    private fun createClickableSpan(): SpannableString {
        val spannableString = SpannableString(getString(R.string.new_member) + " " + getString(R.string.click_here_to_sign_up))

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                navigateToRegisterActivity()
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.color = resources.getColor(R.color.purple_500)
            }
        }

        spannableString.setSpan(clickableSpan, getString(R.string.new_member).length + 1, spannableString.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannableString
    }

    private fun navigateToRegisterActivity() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}
