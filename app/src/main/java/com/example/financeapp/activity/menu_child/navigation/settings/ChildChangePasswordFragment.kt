package com.example.financeapp.activity.menu_child.navigation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.financeapp.R
import com.example.financeapp.activity.menu_child.ChildMenuActivity
import com.example.financeapp.databinding.FragmentChildChangePasswordBinding
import com.example.financeapp.model.Child
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class ChildChangePasswordFragment : Fragment(), CoroutineScope {

    private lateinit var binding: FragmentChildChangePasswordBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var childData: Child
    private lateinit var parentActivity: ChildMenuActivity

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChildChangePasswordBinding.inflate(inflater, container, false)
        val view = binding.root

        databaseReference = FirebaseDatabase.getInstance().reference.child("child")

        parentActivity = activity as ChildMenuActivity
        val childId = parentActivity.childId


        println(childId)

        setClickListeners(childId)
        whenBackPressed()

        return view
    }

    private fun setClickListeners(childId:String) {
        binding.btnChange.setOnClickListener {
            val currentPassword = getChildPasswordData()
            val oldPassword = binding.currentPassword.text.toString()
            val newPasswordFirst = binding.newPasswordFirst.text.toString()
            val newPasswordSecond = binding.newPasswordSecond.text.toString()
            if (currentPassword != null && oldPassword.isNotEmpty() && newPasswordFirst.isNotEmpty() && newPasswordSecond.isNotEmpty()) {
                changeChildPassword(
                    currentPassword,
                    oldPassword,
                    newPasswordFirst,
                    newPasswordSecond,
                    childId
                )
            }

            switchToFragment(ChildSettingsFragment())

        }
    }


        private fun changeChildPassword(
            currentPassword: String,
            oldPassword: String,
            newPasswordFirst: String,
            newPasswordSecond: String,
            childId: String
        ) {
            if ((newPasswordFirst == newPasswordSecond) && currentPassword == oldPassword) {
                val childRef = databaseReference.child(childId)
                childRef.child("password").setValue(newPasswordFirst)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Şifre başarıyla güncellendi
                        } else {
                            // Hata durumunu ele al
                        }
                    }
            }


        }


        private fun getChildPasswordData(): String? {
            var currentPassword: String? = null
            if (activity is ChildMenuActivity) {
                val childData = (activity as ChildMenuActivity).getChildData()
                currentPassword = childData.password
            }
            return currentPassword
        }


        private fun switchToFragment(fragment: Fragment) {
            // Reload or refresh the profile fragment
            val targetFragment = fragment
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, targetFragment)
                .commit()
        }


        private fun whenBackPressed() {
            requireActivity().onBackPressedDispatcher.addCallback(
                requireActivity(),
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        switchToFragment((ChildSettingsFragment()))
                    }
                })
        }
    }