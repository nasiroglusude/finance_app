package com.example.financeapp.activity.menu

import com.example.financeapp.activity.menu.navigation.child.KidsFragment
import com.example.financeapp.activity.menu.navigation.NewBudgetFragment
import com.example.financeapp.activity.menu.navigation.settings.SettingsFragment
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.example.financeapp.R
import com.example.financeapp.activity.enterance.NoConnectionActivity
import com.example.financeapp.databinding.ActivityMenuBinding
import com.example.financeapp.activity.menu.navigation.ExchangeFragment
import com.example.financeapp.activity.menu.navigation.HomeFragment
import com.example.financeapp.model.User
import com.example.financeapp.util.ConnectivityObserver
import com.example.financeapp.util.NetworkConnectivityObserver
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

class MenuActivity: AppCompatActivity(), CoroutineScope {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMenuBinding
    private lateinit var userData: User
    private lateinit var conObserver: ConnectivityObserver

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        conObserver = NetworkConnectivityObserver(applicationContext)
        observeConnection(conObserver)

        replaceFragment(HomeFragment())
        navigationButtonListener()

        userData = User()
        databaseReference = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        launch {
            fetchUserData()
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
                R.id.add -> {
                    replaceFragment(NewBudgetFragment())
                    true
                }
                R.id.exchange -> {
                    replaceFragment(ExchangeFragment())
                    true
                }
                R.id.settings -> {
                    replaceFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun observeConnection(connectivityObserver: ConnectivityObserver){
        lifecycleScope.launch {
            connectivityObserver.observe().collect { status ->
                when (status) {
                    ConnectivityObserver.Status.Available -> {
                    }
                    ConnectivityObserver.Status.Unavailable,
                    ConnectivityObserver.Status.Losing,
                    ConnectivityObserver.Status.Lost -> {
                        // Handle when network is unavailable
                        startActivity(Intent(this@MenuActivity, NoConnectionActivity::class.java))
                    }
                }
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
    fun updateSelectedNavItem(itemId: Int) {
        binding.bottomNavigationView.selectedItemId = itemId
    }


}