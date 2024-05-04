package com.example.financeapp.activity.menu.navigation.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.financeapp.R
import com.example.financeapp.activity.menu.MenuActivity
import com.example.financeapp.databinding.FragmentUserSettingsBinding
import com.example.financeapp.model.User
import com.google.android.material.textfield.TextInputEditText
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

class UserSettingsFragment : Fragment(), CoroutineScope {

    private lateinit var binding: FragmentUserSettingsBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var parentActivity: MenuActivity

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserSettingsBinding.inflate(inflater, container, false)
        val view = binding.root

        parentActivity = activity as MenuActivity

        databaseReference = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        launch{
            fetchUserData()
        }
        setClickListeners()
        whenBackPressed()
        return view
    }

    private fun setClickListeners(){
        binding.btnSave.setOnClickListener{
            binding.apply {
                launch {
                    updateUserData(
                        firstName.text.toString(),
                        lastName.text.toString(),
                        phoneNumber.text.toString(),
                        dateOfBirth.text.toString(),
                        email.text.toString())
                }
            }
            parentActivity.replaceFragment(SettingsFragment())
            }
        binding.backButton.setOnClickListener{
            parentActivity.replaceFragment(SettingsFragment())
        }
    }



    @SuppressLint("ClickableViewAccessibility")
    private fun setProperties(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        dateOfBirth: String,
        email: String
    ){
        setEditableOnClickDrawableEnd(binding.firstName, firstName)
        setEditableOnClickDrawableEnd(binding.lastName, lastName)
        setEditableOnClickDrawableEnd(binding.phoneNumber, phoneNumber)
        setEditableOnClickDrawableEnd(binding.dateOfBirth, dateOfBirth)
        setEditableOnClickDrawableEnd(binding.email, email)

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setEditableOnClickDrawableEnd(textInputEditText: TextInputEditText, initialText: String) {
        textInputEditText.apply {
            setText(initialText)
            isClickable = true
            isFocusable = false
            // Set onTouchListener to detect click on the drawable end
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    val drawableEndWidth = (compoundDrawablesRelative[2]?.bounds?.width() ?: 0)
                    // Check if the click is on the drawable end
                    if (event.rawX >= right - drawableEndWidth - paddingLeft) {
                        // Enable editing
                        isFocusable = true
                        isClickable = true
                        isFocusableInTouchMode = true
                    }
                }
                // Return false to allow the event to continue to propagate
                return@setOnTouchListener false
            }
        }
    }

    private fun fetchUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { currentUserIndex ->
            val userId = currentUserIndex.uid
            val preferencesRef = databaseReference.child("users").child(userId)

            preferencesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val user = dataSnapshot.getValue(User::class.java)
                        user?.let { userIndex ->
                            val firstName = userIndex.firstName
                            val lastName = userIndex.lastName
                            val phoneNumber = userIndex.phoneNumber
                            val dateOfBirth = userIndex.dateOfBirth
                            val email = userIndex.email
                            setProperties(firstName, lastName, phoneNumber, dateOfBirth, email)
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Error handling
                }
            })
        } ?: run {
            // User not signed in, handle this case
        }
    }

    private fun updateUserData(firstName: String, lastName: String, phoneNumber: String, dateOfBirth: String, email: String) {
        val currentUser = auth.currentUser
        currentUser?.let { currentUserIndex ->
            val userId = currentUserIndex.uid
            val userRef = databaseReference.child("users").child(userId)

            // Create a map of updated user data
            val userData = mapOf(
                "firstName" to firstName,
                "lastName" to lastName,
                "phoneNumber" to phoneNumber,
                "dateOfBirth" to dateOfBirth,
                "email" to email
            )

            currentUserIndex.updateEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        println("Email successfully changed")
                    } else {
                        // Email address update initiation failed
                        // You can handle the failure, e.g., display an error message
                    }
                }

            // Update user data in the database
            userRef.updateChildren(userData)
                .addOnSuccessListener {
                    // Data updated successfully
                }
                .addOnFailureListener { exception ->
                    // Handle failure
                }
        }
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