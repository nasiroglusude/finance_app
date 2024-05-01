package com.example.financeapp.activity.enterance

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.financeapp.activity.menu.MenuActivity
import com.example.financeapp.databinding.ActivityNoConnectionBinding
import com.example.financeapp.util.ConnectivityObserver
import com.example.financeapp.util.NetworkConnectivityObserver
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class NoConnectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoConnectionBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var conObserver: ConnectivityObserver


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = Firebase.auth
        conObserver = NetworkConnectivityObserver(applicationContext)

        observeConnection(conObserver)
    }

    private fun observeConnection(connectivityObserver: ConnectivityObserver) {
        lifecycleScope.launch {
            connectivityObserver.observe().collect { status ->
                when (status) {
                    ConnectivityObserver.Status.Available -> {
                        if (firebaseAuth.currentUser?.uid != null) {
                            startActivity(Intent(this@NoConnectionActivity, MenuActivity::class.java))
                            finish()
                        }
                        if (firebaseAuth.currentUser?.uid == null) {
                            startActivity(
                                Intent(
                                    this@NoConnectionActivity,
                                    IntroActivity::class.java
                                )
                            )
                            finish()
                        }
                    }

                    ConnectivityObserver.Status.Unavailable,
                    ConnectivityObserver.Status.Losing,
                    ConnectivityObserver.Status.Lost ->{
                        //Do nothing
                    }
                }
            }
        }
    }

}