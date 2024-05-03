package com.example.financeapp.activity.menu_child.navigation.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.financeapp.R
import com.example.financeapp.activity.menu_child.ChildMenuActivity
import com.example.financeapp.databinding.FragmentChildProfileSettingsBinding
import com.example.financeapp.model.Child
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

class ChildProfileSettingsFragment : Fragment(), CoroutineScope {

    private lateinit var binding: FragmentChildProfileSettingsBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var parentActivity: ChildMenuActivity

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChildProfileSettingsBinding.inflate(inflater, container, false)
        val view = binding.root

        parentActivity = activity as ChildMenuActivity
        val childId = parentActivity.childId


        databaseReference = FirebaseDatabase.getInstance().reference.child("child")
        auth = FirebaseAuth.getInstance()

        launch{
            fetchChildData(childId)
        }
        setClickListeners(childId)
        whenBackPressed()
        return view
    }

    private fun setClickListeners(childId: String){
        binding.btnSave.setOnClickListener{
            binding.apply {
                launch {
                    updateUserData(
                        firstName.text.toString(),
                        lastName.text.toString(),
                        dateOfBirth.text.toString(),
                        childId)
                }
            }
            parentActivity.replaceFragment(ChildSettingsFragment())
        }
        binding.backButton.setOnClickListener{
            parentActivity.replaceFragment(ChildSettingsFragment())
        }
    }



    @SuppressLint("ClickableViewAccessibility")
    private fun setProperties(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
    ){
        setEditableOnClickDrawableEnd(binding.firstName, firstName)
        setEditableOnClickDrawableEnd(binding.lastName, lastName)
        setEditableOnClickDrawableEnd(binding.dateOfBirth, dateOfBirth)
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

    private fun fetchChildData(childId:String) {
        childId.let { uid ->
            val childRef = databaseReference.child(uid)
            childRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val childData = dataSnapshot.getValue(Child::class.java)
                        if (childData != null) {
                            val firstName = childData.firstName
                            val lastName = childData.lastName
                            val dateOfBirth = childData.dateOfBirth
                            setProperties(firstName, lastName, dateOfBirth)
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

    private fun updateUserData(firstName: String, lastName: String, dateOfBirth: String, childId: String) {
        childId.let { uid ->
            val childRef = databaseReference.child(uid)

            // Create a map of updated user data
            val userData = mapOf(
                "firstName" to firstName,
                "lastName" to lastName,
                "dateOfBirth" to dateOfBirth,
            )
            // Update user data in the database
            childRef.updateChildren(userData)
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
                switchToFragment((ChildSettingsFragment()))
            }
        })
    }
}