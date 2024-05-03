package com.example.financeapp.activity.menu_child

import com.example.financeapp.activity.menu_child.navigation.settings.ChildSettingsFragment
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.example.financeapp.R
import com.example.financeapp.activity.enterance.NoConnectionActivity
import com.example.financeapp.activity.menu_child.navigation.ChildHomeFragment
import com.example.financeapp.activity.menu_child.navigation.ChildNewBudgetFragment
import com.example.financeapp.databinding.ActivityChildMenuBinding
import com.example.financeapp.model.Child
import com.example.financeapp.util.ConnectivityObserver
import com.example.financeapp.util.NetworkConnectivityObserver
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

class ChildMenuActivity: AppCompatActivity(), CoroutineScope  {

    private lateinit var binding: ActivityChildMenuBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var childData: Child
    private lateinit var conObserver: ConnectivityObserver
    lateinit var childId: String


    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        conObserver = NetworkConnectivityObserver(applicationContext)
        observeConnection(conObserver)

        childId = intent.getStringExtra("childId").toString()

        replaceFragment(ChildHomeFragment(), childId)
        navigationButtonListener()

        childData = Child()
        databaseReference = FirebaseDatabase.getInstance().reference
        launch {
            fetchChildData(childId)
        }

    }




    private fun navigationButtonListener(){
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    replaceFragment(ChildHomeFragment(), childId)

                    true
                }

                R.id.add -> {
                    replaceFragment(ChildNewBudgetFragment(), childId)
                    true
                }

                R.id.setting -> {
                    replaceFragment(ChildSettingsFragment())
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
                        startActivity(Intent(this@ChildMenuActivity, NoConnectionActivity::class.java))
                    }
                }
            }
        }
    }

    private fun fetchChildData(childId: String) {
        val childRef = databaseReference.child("child").child(childId)

        childRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    childData = dataSnapshot.getValue(Child::class.java) ?: Child()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error condition
            }
        })
    }

    // Diğer fragmentlere kullanıcı verilerini iletmek için bir yöntem
    fun getChildData(): Child {
        return childData
    }

    fun replaceFragment(fragment: Fragment, childId: String? = null) {
        val bundle = Bundle().apply {
            childId?.let { putString("childId", it) }
        }
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(binding.frameLayout.id, fragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()

        navigationButtonListener()
    }



    fun updateSelectedNavItem(itemId: Int) {
        binding.bottomNavigationView.selectedItemId = itemId
    }
}