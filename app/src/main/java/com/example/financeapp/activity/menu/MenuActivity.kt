package com.example.financeapp.activity.menu

import KidsFragment
import ProfileFragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.financeapp.R
import com.example.financeapp.databinding.ActivityMenuBinding
import com.example.financeapp.activity.menu.navigation.ExchangeFragment
import com.example.financeapp.activity.menu.navigation.HomeFragment
import com.example.financeapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MenuActivity: AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMenuBinding
    private lateinit var userData: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        replaceFragment(HomeFragment())
        navigationButtonListener()

        userData = User()
        databaseReference = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
        fetchUserData()
        binding.btnAdd.setOnClickListener{
            navigateToActivity(this, NewBudgetActivity::class.java)
        }
    }

    private fun navigationButtonListener(){
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.kids -> {
                    replaceFragment(KidsFragment())
                    true
                }
                R.id.exchange -> {
                    replaceFragment(ExchangeFragment())
                    true
                }
                R.id.profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
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
                        userData = dataSnapshot.getValue(User::class.java) ?: User()
                    }
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    // Hata durumunu işle
                }
            })
        } ?: run {
            // Oturum açmış bir kullanıcı yoksa, bu durumu ele al
        }
    }

    // Diğer fragmentlere kullanıcı verilerini iletmek için bir yöntem
    fun getUserData(): User {
        return userData
    }


    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.frameLayout.id, fragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
    }

    fun navigateToActivity(context: Context, targetActivity: Class<*>) {
        val intent = Intent(context, targetActivity)
        context.startActivity(intent)
    }
}