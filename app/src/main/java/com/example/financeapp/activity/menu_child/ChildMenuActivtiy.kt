package com.example.financeapp.activity.menu_child

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.financeapp.R
import com.example.financeapp.activity.menu_child.navigation.ChildHomeFragment
import com.example.financeapp.activity.menu_child.navigation.ChildProfileFragment
import com.example.financeapp.databinding.ActivityChildMenuBinding

class ChildMenuActivity: AppCompatActivity() {

    private lateinit var binding: ActivityChildMenuBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val childId = intent.getStringExtra("childId").toString()
        println("Child IDDDDDDDDDXD:"+childId)
        //HomeFragment'a git ve childId datasını da götür
        val bundle = Bundle()
        bundle.putString("childId", childId)
        replaceFragment(ChildHomeFragment(), bundle)
        navigationButtonListener()

        binding.btnAdd.setOnClickListener {
            val intent = Intent(this, ChildNewBudgetActivity::class.java)
            intent.putExtra("childId", childId) // Kullanıcı ID'sini intent'e ekle
            startActivity(intent)
        }
    }

    private fun navigationButtonListener(){
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    replaceFragment(ChildHomeFragment())
                    true
                }

                R.id.profile -> {
                    replaceFragment(ChildProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    fun replaceFragment(fragment: Fragment, data: Bundle? = null) {
        fragment.arguments = data // Veriyi fragment'e aktar
        supportFragmentManager.beginTransaction()
            .replace(binding.frameLayout.id, fragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
    }


}