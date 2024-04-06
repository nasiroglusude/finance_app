package com.example.financeapp.activity.menu

import KidsFragment
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
import com.example.financeapp.activity.menu.navigation.ProfileFragment

class MenuActivity: AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        replaceFragment(HomeFragment())
        navigationButtonListener()
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
                    navigateToActivity(this, NewBudgetActivity::class.java)
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