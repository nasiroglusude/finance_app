package com.example.financeapp.activity.menu.navigation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.financeapp.R
import com.example.financeapp.activity.menu.MenuActivity
import com.example.financeapp.databinding.FragmentChangePasswordBinding
import com.example.financeapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class ChangePasswordFragment : Fragment(),CoroutineScope {

    private lateinit var binding: FragmentChangePasswordBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var userData: User

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        val view = binding.root
        userData = User()

        databaseReference = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
        setClickListeners()
        whenBackPressed()
        return view
    }

    private fun setClickListeners() {
        binding.btnChange.setOnClickListener {
            val currentPassword = getUserPasswordData()
            val oldPassword = binding.currentPassword.text.toString()
            val newPasswordFirst = binding.newPasswordFirst.text.toString()
            val newPasswordSecond = binding.newPasswordSecond.text.toString()
            if (currentPassword != null && oldPassword.isNotEmpty() && newPasswordFirst.isNotEmpty() && newPasswordSecond.isNotEmpty()){
                changeUserPassword(currentPassword, oldPassword, newPasswordFirst, newPasswordSecond)
            }

            switchToFragment(SettingsFragment())

        }
    }

    private fun changeUserPassword(
        currentPassword: String,
        oldPassword: String,
        newPasswordFirst: String,
        newPasswordSecond: String
    ) {
        val currentUser = auth.currentUser
        currentUser?.let { currentUserIndex ->
            if ((newPasswordFirst == newPasswordSecond) && currentPassword == oldPassword) {
                currentUserIndex.updatePassword(newPasswordFirst)
                    .addOnCompleteListener { passwordUpdateTask ->
                        if (passwordUpdateTask.isSuccessful) {
                            val userId = currentUserIndex.uid
                            val userRef = databaseReference.child("users").child(userId)
                            userRef.child("password").setValue(newPasswordFirst)
                                .addOnSuccessListener {
                                    println("Password successfully updated")
                                }
                                .addOnFailureListener { exception ->
                                    println("Failed to update password in database: ${exception.message}")
                                }
                        } else {
                            println("Failed to update password: ${passwordUpdateTask.exception?.message}")
                        }
                    }
            } else {
                println("Password is not valid")
            }
        }
    }


    private fun getUserPasswordData(): String? {
        var currentPassword: String? = null
        if (activity is MenuActivity) {
            val userData = (activity as MenuActivity).getUserData()
            currentPassword = userData.password
        }
        return currentPassword
    }



    private fun switchToFragment(fragment: Fragment){
        // Reload or refresh the profile fragment
        val targetFragment = fragment
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, targetFragment)
            .commit()
    }


    private fun whenBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                switchToFragment((SettingsFragment()))
            }
        })
    }
}