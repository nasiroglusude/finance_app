package com.example.financeapp.activity.menu.navigation.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.example.financeapp.R
import com.example.financeapp.activity.enterance.IntroActivity
import com.example.financeapp.activity.enterance.LoginActivity
import com.example.financeapp.activity.menu.MenuActivity
import com.example.financeapp.activity.menu.navigation.HomeFragment
import com.example.financeapp.adapter.CurrencyAdapter
import com.example.financeapp.databinding.FragmentProfileBinding
import com.example.financeapp.enums.Currency
import com.example.financeapp.model.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.CoroutineContext

class SettingsFragment : Fragment(), CoroutineScope{

    private lateinit var binding: FragmentProfileBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: CurrencyAdapter
    private lateinit var selectedCurrency:String
    private var selectedCurrencyPosition: Int = 0

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root
        // Firebase bileşenlerini başlat
        databaseReference = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        val currencies = Currency.entries.toList()
        adapter = CurrencyAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, currencies)
        launch {
            fetchCurrentUserCurrency()
        }

        setCurrencySpinner()
        setLanguageSelectionMenu()
        setClickListeners()
        whenBackPressed()
        return view
    }

    private fun setClickListeners(){
        binding.btnUserSetting.setOnClickListener{
            switchToFragment(UserSettingsFragment())
        }
        binding.btnChangePassword.setOnClickListener {
            switchToFragment(ChangePasswordFragment())
        }
        binding.btnLogout.setOnClickListener {
            logOutFromCurrentUser()
        }
    }
    private fun logOutFromCurrentUser(){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_message)
            .setPositiveButton(R.string.yes) { dialog, _ ->
                auth.signOut()
                navigateToActivity(requireContext(), IntroActivity::class.java)
                requireActivity().finish()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun navigateToActivity(context: Context, targetActivity: Class<*>) {
        val intent = Intent(context, targetActivity)
        context.startActivity(intent)
    }

    private fun setLanguageSelectionMenu() {
        // Set up the menu
        binding.btnLanguage.setOnClickListener { view ->
            PopupMenu(requireContext(), view).apply {
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
    private fun switchToFragment(fragment: Fragment){
        // Reload or refresh the profile fragment
        val targetFragment = fragment
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, targetFragment)
            .commit()
    }

    private fun updateAppLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val configuration = resources.configuration
        configuration.setLocale(locale)

        val displayMetrics = resources.displayMetrics
        requireContext().resources.updateConfiguration(configuration, displayMetrics)


        // Reload or refresh the profile fragment (SettingsFragment)
        val profileFragment = SettingsFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, profileFragment)
            .commit()
    }



    private fun setCurrencySpinner(){
        selectedCurrency = ""
        binding.currencySpinner.setAdapter(adapter)

        // Optional: Set a click listener to get the selected currency
        binding.currencySpinner.setOnItemClickListener { _, _, position, _ ->
            selectedCurrencyPosition = position
            selectedCurrency = Currency.entries[selectedCurrencyPosition].toString()
            launch {
                updateUserData(selectedCurrency)
            }
        }
        var isDropdownVisible = false
        binding.currencySpinner.setOnClickListener {
            if (isDropdownVisible) {
                // If dropdown is visible, dismiss it
                binding.currencySpinner.dismissDropDown()
                isDropdownVisible = false
            } else {
                // If dropdown is not visible, show it
                binding.currencySpinner.showDropDown()
                isDropdownVisible = true
            }
        }
    }

    private fun fetchCurrentUserCurrency() {
        val currentUser = auth.currentUser
        currentUser?.let { currentUserIndex ->
            val userId = currentUserIndex.uid
            val userRef = databaseReference.child("users").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val user = dataSnapshot.getValue(User::class.java)
                        user?.let { userIndex ->
                            val currency = userIndex.currency // Assuming currency is stored in the 'currency' field
                            // Set the fetched currency as the currentCurrency
                            val currentCurrency = currency
                            // Update the currency spinner with the fetched currency
                            binding.currencySpinner.setText(currentCurrency, false)
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle error
                }
            })
        }
    }


    private fun updateUserData(newCurrency:String) {
        val currentUser = auth.currentUser
        currentUser?.let { currentUserIndex ->
            val userId = currentUserIndex.uid
            val userRef = databaseReference.child("users").child(userId)
            userRef.child("currency").setValue(newCurrency)
        }
    }

    private fun switchToHomeFragment() {
        if (isAdded) {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, HomeFragment())
                .commit()
            (requireActivity() as MenuActivity).updateSelectedNavItem(R.id.home)
        }
    }

    private fun whenBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                switchToHomeFragment()
            }
        })
    }
}



