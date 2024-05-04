package com.example.financeapp.activity.menu_child.navigation.settings

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
import com.example.financeapp.activity.menu_child.ChildMenuActivity
import com.example.financeapp.activity.menu_child.navigation.ChildHomeFragment
import com.example.financeapp.adapter.CurrencyAdapter
import com.example.financeapp.databinding.FragmentChildSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.util.*
import kotlin.coroutines.CoroutineContext

class ChildSettingsFragment : Fragment(), CoroutineScope{

    private lateinit var binding: FragmentChildSettingsBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: CurrencyAdapter

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChildSettingsBinding.inflate(inflater, container, false)
        val view = binding.root

        databaseReference = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        setLanguageSelectionMenu()
        setClickListeners()
        whenBackPressed()

        return view
    }

    private fun setClickListeners(){
        binding.btnUserSetting.setOnClickListener{
            switchToFragment(ChildProfileSettingsFragment())
        }
        binding.btnChangePassword.setOnClickListener {
            switchToFragment(ChildChangePasswordFragment())
        }
        binding.btnLogout.setOnClickListener {
            logOutFromKid()
        }
    }

    private fun logOutFromKid(){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_message)
            .setPositiveButton(R.string.yes) { dialog, _ ->
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

        // Reload or refresh the profile fragment
        val childSettingsFragment = ChildSettingsFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, childSettingsFragment)
            .commit()
    }

    private fun switchToHomeFragment() {
        if (isAdded) {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, ChildHomeFragment())
                .commit()
            (requireActivity() as ChildMenuActivity).updateSelectedNavItem(R.id.home)
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


