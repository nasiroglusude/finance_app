package com.example.financeapp.activity.menu.navigation.more

import BillsAdapter
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.financeapp.R
import com.example.financeapp.activity.menu.MenuActivity
import com.example.financeapp.activity.menu.navigation.HomeFragment
import com.example.financeapp.adapter.CurrencyAdapter
import com.example.financeapp.databinding.DialogAddBillBinding
import com.example.financeapp.databinding.FragmentBillsBinding
import com.example.financeapp.model.User
import com.example.financeapp.enums.Currency
import com.example.financeapp.model.Bill
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

class BillsFragment : Fragment(), BillsAdapter.OnDeleteBillClickListener, CoroutineScope {

    private lateinit var binding: FragmentBillsBinding
    private lateinit var dialogBinding: DialogAddBillBinding
    private lateinit var adapter: BillsAdapter
    private lateinit var currencyAdapter: CurrencyAdapter
    private lateinit var databaseReference: DatabaseReference
    private var selectedCurrencyPosition: Int = 0
    private lateinit var auth: FirebaseAuth
    private var billList: MutableList<Bill> = mutableListOf()
    private lateinit var currencySymbol: String

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBillsBinding.inflate(inflater, container, false)
        dialogBinding = DialogAddBillBinding.inflate(inflater, container, false)
        val view = binding.root


        // Firebase bileşenlerini başlat
        databaseReference = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // RecyclerView'ı başlat
        adapter = BillsAdapter(billList, this)
        binding.billsList.layoutManager = LinearLayoutManager(requireContext())
        binding.billsList.adapter = adapter

        setSpinner()
        // Firebase'den çocuk verilerini al
        if (billList.isEmpty()) {
            println("Databaseden çekti")
            launch {
                fetchBillData()
            }
        }

        // "Çocuk Ekle" düğmesi için tıklama dinleyicisini ayarla
        binding.btnAddBill.setOnClickListener {
            showAddBillDialog(dialogBinding)
        }

        currencySymbol = ""

        launch {
            fetchBillCurrency()
        }
        whenBackPressed()

        return view
    }

    private fun showDatePicker(dialogBinding: DialogAddBillBinding) {
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

    private fun fetchBillCurrency() {
        val currentUser = auth.currentUser?.uid
        currentUser?.let { userId ->
            val preferencesRef = databaseReference.child("users").child(userId)

            preferencesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val user = dataSnapshot.getValue(User::class.java)
                        user?.let {
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
    private fun showAddBillDialog(dialogBinding: DialogAddBillBinding) {
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
                val owner = dialogBinding.editTextOwnerName.text.toString()
                val amount = dialogBinding.editTextAmount.text.toString()
                val currency = dialogBinding.materialSpinner.text.toString()
                val lastDate = dialogBinding.editTextLastDate.text.toString()

                if (owner.isNotEmpty() && amount.isNotEmpty() && currency.isNotEmpty() && lastDate.isNotEmpty()) {
                    // Bir Çocuk nesnesi oluştur
                    val newBill = Bill(
                        UUID.randomUUID().toString(),
                        owner,
                        amount,
                        currency,
                        lastDate,
                        status = "unpaid"
                    )
                    saveBillToDatabase(newBill)

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
        if (fragment is BillsFragment) {
            // Detach the fragment
            parentFragmentManager.beginTransaction().detach(fragment).commitNow()

            // Reattach the fragment
            parentFragmentManager.beginTransaction().attach(fragment).commitNow()
        }
    }


    private fun saveBillToDatabase(newBill:Bill){
        val currentUser = auth.currentUser?.uid
        currentUser?.let { uid ->
            // Yeni çocuğu Firebase Gerçek Zamanlı Veritabanına ekle
            databaseReference.child("users").child(uid).child("bills").child(newBill.id)
                .setValue(newBill)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Çocuk başarıyla "child" düğümüne eklendi
                        fetchBillData() // Arayüzü yenile
                    } else {

                        switchToFragment(BillsFragment())

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
    private fun fetchBillData() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        currentUserId?.let { uid ->
            val query: Query =
                databaseReference.child("users").child(uid).child("bills")
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val billsList: MutableList<Bill> = mutableListOf()
                    for (childSnapshot in snapshot.children) {
                        val bill: Bill? = childSnapshot.getValue(Bill::class.java)
                        bill?.let { billsList.add(it) }
                    }
                    adapter.updateData(billsList)
                }

                override fun onCancelled(error: DatabaseError) {
                    switchToFragment(BillsFragment())
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
    private fun showDeleteBillConfirmationDialog(billId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Bill")
            .setMessage("Are you sure you want to delete this bill?")
            .setPositiveButton("OK") { dialog, _ ->
                // Call a function to delete the child from Firebase
                deleteBill(billId)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                switchToFragment(HomeFragment())
                dialog.dismiss()
            }
            .show()
    }

    // Function to delete the child from Firebase
    private fun deleteBill(billId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        currentUserId?.let {id ->
            val childRef = databaseReference.child("users").child(id).child("bills").child(billId)
            childRef.removeValue()
                .addOnSuccessListener {
                    // Child successfully deleted
                    fetchBillData() // Refresh the UI after deletion
                }
                .addOnFailureListener { exception ->
                    // Failed to delete child
                    Log.e(TAG, "Failed to delete child: $exception")
                    // Handle the failure here
                }
        }
    }

    // Update the onDeleteChildClick function
    override fun onDeleteBillClick(position: Int) {
        val billId = billList[position].id
        showDeleteBillConfirmationDialog(billId)
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



