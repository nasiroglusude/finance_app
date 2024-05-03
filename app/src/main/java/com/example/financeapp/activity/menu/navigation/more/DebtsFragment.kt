package com.example.financeapp.activity.menu.navigation.more

import DebtsAdapter
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.financeapp.R
import com.example.financeapp.activity.menu.MenuActivity
import com.example.financeapp.activity.menu.navigation.HomeFragment
import com.example.financeapp.activity.menu_child.ChildMenuActivity
import com.example.financeapp.activity.menu_child.navigation.ChildHomeFragment
import com.example.financeapp.adapter.CurrencyAdapter
import com.example.financeapp.databinding.DialogAddDebtBinding
import com.example.financeapp.databinding.FragmentDebtsBinding
import com.example.financeapp.model.User
import com.example.financeapp.enums.Currency
import com.example.financeapp.model.Debt
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

class DebtsFragment : Fragment(), DebtsAdapter.OnDeleteDebtClickListener, CoroutineScope {

    private lateinit var binding: FragmentDebtsBinding
    private lateinit var dialogBinding: DialogAddDebtBinding
    private lateinit var adapter: DebtsAdapter
    private lateinit var currencyAdapter: CurrencyAdapter
    private lateinit var databaseReference: DatabaseReference
    private var selectedCurrencyPosition: Int = 0
    private lateinit var auth: FirebaseAuth
    private var debtsList: MutableList<Debt> = mutableListOf()
    private lateinit var currencySymbol: String

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDebtsBinding.inflate(inflater, container, false)
        dialogBinding = DialogAddDebtBinding.inflate(inflater, container, false)
        val view = binding.root


        // Firebase bileşenlerini başlat
        databaseReference = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // RecyclerView'ı başlat
        adapter = DebtsAdapter(debtsList, this)
        binding.debtsList.layoutManager = LinearLayoutManager(requireContext())
        binding.debtsList.adapter = adapter

        setSpinner()
        // Firebase'den çocuk verilerini al
        if (debtsList.isEmpty()) {
            println("Databaseden çekti")
            launch {
                fetchDebtData()
            }
        }

        // "Çocuk Ekle" düğmesi için tıklama dinleyicisini ayarla
        binding.btnAddDebt.setOnClickListener {
            showAddDebtDialog(dialogBinding)
        }

        currencySymbol = ""

        launch {
            fetchDebtCurrency()
        }
        whenBackPressed()

        return view
    }

    private fun showDatePicker(dialogBinding: DialogAddDebtBinding) {
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val builder = MaterialDatePicker.Builder.datePicker()
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            // Seçilen tarihi biçimlendir
            val formattedDate = dateFormat.format(Date(selection))
            // Biçimlendirilmiş tarihi EditText'e ayarla
            dialogBinding.editTextLastDate.setText(formattedDate)
        }
        picker.show(childFragmentManager, picker.toString())
    }

    private fun fetchDebtCurrency() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val preferencesRef = databaseReference.child("users").child(userId)

            preferencesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val user1 = dataSnapshot.getValue(User::class.java)
                        user1?.let {
                            currencySymbol = Currency.valueOf(it.currency).code
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setSpinner(){
        val currencies = Currency.entries.toList()
        // Create the adapter with your custom layout and the Currency array
        currencyAdapter = CurrencyAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, currencies)

        dialogBinding.materialSpinner.setAdapter(currencyAdapter)
        // Set the initial selection to the first item in the list
        dialogBinding.materialSpinner.setText(currencyAdapter.getItem(0).toString(), false)

        var isDropdownVisible = false

        dialogBinding.materialSpinner.setOnClickListener {
            if (isDropdownVisible) {
                // If dropdown is visible, dismiss it
                dialogBinding.materialSpinner.dismissDropDown()
            } else {
                // If dropdown is not visible, show it
                dialogBinding.materialSpinner.showDropDown()
            }
            // Toggle the dropdown visibility
            isDropdownVisible = !isDropdownVisible
        }

        // Optional: Set a click listener to get the selected currency
        dialogBinding.materialSpinner.setOnItemClickListener { _, _, position, _ ->
            selectedCurrencyPosition = position
            // Do something with the selected currency, such as displaying it
        }

    }


    // Çocuk ekleme iletişim kutusunu gösteren fonksiyon
    private fun showAddDebtDialog(dialogBinding: DialogAddDebtBinding) {
        val dialogView = dialogBinding.root

        // Check if the dialog view already has a parent
        val parent = dialogView.parent
        if (parent != null && parent is ViewGroup) {
            (parent).removeView(dialogView) // Remove the view from its parent
        }

        dialogBinding.editTextLastDate.setOnClickListener {
            showDatePicker(dialogBinding)
        }

        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val creditor = dialogBinding.editTextCreditorName.text.toString()
                val amount = dialogBinding.editTextAmount.text.toString()
                val currency = dialogBinding.materialSpinner.text.toString()
                val lastDate = dialogBinding.editTextLastDate.text.toString()

                if (creditor.isNotEmpty() && amount.isNotEmpty() && currency.isNotEmpty() && lastDate.isNotEmpty()) {
                    // Bir Çocuk nesnesi oluştur
                    val newDebt = Debt(
                        UUID.randomUUID().toString(),
                        creditor,
                        amount,
                        currency,
                        lastDate,
                        status = "unpaid"
                    )
                    saveDebtToDatabase(newDebt)

                } else {
                    refreshFragment()
                    Toast.makeText(
                        requireContext(),
                        "First name and date of birth are required",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialogBuilder.show()
    }
    private fun refreshFragment() {
        val fragment = parentFragmentManager.findFragmentById(R.id.frame_layout)
        if (fragment is DebtsFragment) {
            // Detach the fragment
            parentFragmentManager.beginTransaction().detach(fragment).commitNow()

            // Reattach the fragment
            parentFragmentManager.beginTransaction().attach(fragment).commitNow()
        }
    }


    private fun saveDebtToDatabase(newDebt:Debt){
        val currentUser = auth.currentUser?.uid
        currentUser?.let { uid ->
            // Yeni çocuğu Firebase Gerçek Zamanlı Veritabanına ekle
            databaseReference.child("users").child(uid).child("debts").child(newDebt.id)
                .setValue(newDebt)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Çocuk başarıyla "child" düğümüne eklendi
                        fetchDebtData() // Arayüzü yenile
                    } else {

                        switchToFragment(DebtsFragment())

                        // Çocuğun "child" düğümüne eklenmesi başarısız oldu
                        Log.e(
                            TAG,
                            "Failed to add child to the 'child' node: ${task.exception}"
                        )
                        // Hatası burada ele alabilirsiniz
                    }
                }
        }
    }

    // Çocuk verilerini Firebase'den almak için fonksiyon
    private fun fetchDebtData() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        currentUserId?.let { uid ->
            val query: Query =
                databaseReference.child("users").child(uid).child("debts")
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val debtList: MutableList<Debt> = mutableListOf()
                    for (childSnapshot in snapshot.children) {
                        val debt: Debt? = childSnapshot.getValue(Debt::class.java)
                        debt?.let { debtList.add(it) }
                    }
                    adapter.updateData(debtList)
                }

                override fun onCancelled(error: DatabaseError) {
                    switchToFragment(DebtsFragment())
                    // onCancelled'ı ele al
                    println("Alma işlemi iptal edildi: ${error.message}")
                }
            })
        } ?: run {
            // Eğer mevcut kullanıcı yoksa bir hata mesajı yazdır
            println("Mevcut kullanıcı bulunamadı.")
        }
    }



    // Function to show the material dialog for confirmation
    private fun showDeleteChildConfirmationDialog(debtId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Child")
            .setMessage("Are you sure you want to delete this child?")
            .setPositiveButton("OK") { dialog, _ ->
                // Call a function to delete the child from Firebase
                deleteDebt(debtId)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                switchToFragment(HomeFragment())
                dialog.dismiss()
            }
            .show()
    }

    // Function to delete the child from Firebase
    private fun deleteDebt(debtId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        currentUserId?.let {id ->
            val childRef = databaseReference.child("users").child(id).child("debts").child(debtId)
            childRef.removeValue()
                .addOnSuccessListener {
                    // Child successfully deleted
                    fetchDebtData() // Refresh the UI after deletion
                }
                .addOnFailureListener { exception ->
                    // Failed to delete child
                    Log.e(TAG, "Failed to delete child: $exception")
                    // Handle the failure here
                }
        }
    }

    // Update the onDeleteChildClick function
    override fun onDeleteDebtClick(position: Int) {
        val debtId = debtsList[position].id
        showDeleteChildConfirmationDialog(debtId)
    }

    private fun switchToFragment(fragment: Fragment) {
        if (isAdded) {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit()
            (requireActivity() as MenuActivity).updateSelectedNavItem(R.id.home)
        }
    }

    private fun whenBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                switchToFragment(HomeFragment())
            }
        })
    }
}



