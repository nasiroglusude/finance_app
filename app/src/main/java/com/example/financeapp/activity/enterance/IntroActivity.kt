package com.example.financeapp.activity.enterance

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import com.example.financeapp.R
import com.example.financeapp.databinding.ActivityIntroBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class IntroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseDatabase.getInstance("https://finansuygulama-c3e68-default-rtdb.europe-west1.firebasedatabase.app")
        clickListeners()
        setupLanguageSelectionMenu()

    }

    fun loginPage() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    fun signupPage() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    fun childLoginPage() {
        val intent = Intent(this, ChildLoginActivity::class.java)
        startActivity(intent)
    }


    private fun clickListeners() {
        binding.btnSignup.setOnClickListener{
            signupPage()
        }
        binding.btnLogin.setOnClickListener{
            loginPage()
        }
        binding.btnChildrenLogin.setOnClickListener{
            childLoginPage()
        }
    }

    private fun setupLanguageSelectionMenu() {
        val languageButton: MaterialButton = findViewById(R.id.language_button)

        // Set up the menu
        languageButton.setOnClickListener { view ->
            PopupMenu(this@IntroActivity, view).apply {
                // Inflate your menu resource here
                menuInflater.inflate(R.menu.language_menu, menu)

                // Set a click listener for menu items
                setOnMenuItemClickListener { menuItem ->
                    // Handle menu item clicks here
                    when (menuItem.itemId) {
                        R.id.action_language_en -> {
                            // Handle English language selection
                            updateAppLanguage("en")
                            true
                        }
                        R.id.action_language_tr -> {
                            // Handle Turkish language selection
                            updateAppLanguage("tr")
                            true
                        }
                        // Add more menu items as needed
                        else -> false
                    }
                }
            }.show()
        }
    }



    private fun updateAppLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val configuration = resources.configuration
        configuration.setLocale(locale)

        val displayMetrics = resources.displayMetrics
        baseContext.resources.updateConfiguration(configuration, displayMetrics)

        // You may need to recreate the activity for the changes to take effect
        recreate()
    }

}


