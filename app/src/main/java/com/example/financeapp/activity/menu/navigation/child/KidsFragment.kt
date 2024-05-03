package com.example.financeapp.activity.menu.navigation.child

import KidsAdapter
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
import com.example.financeapp.databinding.DialogAddChildBinding
import com.example.financeapp.databinding.FragmentKidsBinding
import com.example.financeapp.model.Child
import com.example.financeapp.model.User
import com.example.financeapp.enums.Currency
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

class KidsFragment : Fragment(), KidsAdapter.OnDeleteChildClickListener, CoroutineScope {

    private lateinit var binding: FragmentKidsBinding
    private lateinit var dialogBinding: DialogAddChildBinding
    private lateinit var adapter: KidsAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var kidsList: MutableList<User> = mutableListOf()
    private lateinit var currencySymbol: String

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentKidsBinding.inflate(inflater, container, false)
        dialogBinding = DialogAddChildBinding.inflate(inflater, container, false)
        val view = binding.root


        // Firebase bileşenlerini başlat
        databaseReference = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // RecyclerView'ı başlat
        adapter = KidsAdapter(kidsList, this)
        binding.childrenList.layoutManager = LinearLayoutManager(requireContext())
        binding.childrenList.adapter = adapter

        // Firebase'den çocuk verilerini al
        if (kidsList.isEmpty()) {
            println("Databaseden çekti")
            launch {
                fetchChildrenData()
            }
        }

        // "Çocuk Ekle" düğmesi için tıklama dinleyicisini ayarla
        binding.btnAddChildren.setOnClickListener {
            showAddChildDialog(dialogBinding)
        }

        currencySymbol = ""
        launch {
            fetchUserPreferences()
        }
        whenBackPressed()

        return view
    }

    private fun showDatePicker(dialogBinding: DialogAddChildBinding) {
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val builder = MaterialDatePicker.Builder.datePicker()
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            // Seçilen tarihi biçimlendir
            val formattedDate = dateFormat.format(Date(selection))
            // Biçimlendirilmiş tarihi EditText'e ayarla
            dialogBinding.dateOfBirth.setText(formattedDate)
        }
        picker.show(childFragmentManager, picker.toString())
    }

    private fun fetchUserPreferences() {
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

    // Çocuk ekleme iletişim kutusunu gösteren fonksiyon
    private fun showAddChildDialog(dialogBinding: DialogAddChildBinding) {
        val dialogView = dialogBinding.root

        dialogBinding.dateOfBirth.setOnClickListener {
            showDatePicker(dialogBinding)
        }

        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val firstName = dialogBinding.editTextFirstName.text.toString()
                val lastName = dialogBinding.editTextLastName.text.toString()
                val dateOfBirth = dialogBinding.dateOfBirth.text.toString()

                if (firstName.isNotEmpty() && dateOfBirth.isNotEmpty()) {
                    val email = auth.currentUser?.email ?: ""

                    // Rastgele bir şifre oluştur
                    val password = generateRandomPassword(8)

                    // Geçerli tarihi al
                    val currentDateString =
                        SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
                    val currency = currencySymbol
                    val balance = "0"
                    // Bir Çocuk nesnesi oluştur
                    val newChild = Child(
                        UUID.randomUUID().toString(), // Çocuk için benzersiz bir kimlik oluştur
                        firstName,
                        lastName,
                        dateOfBirth,
                        email,
                        password,
                        currentDateString,
                        balance,
                        currency
                    )

                    // Yeni çocuğu Firebase Gerçek Zamanlı Veritabanına ekle
                    databaseReference.child("child").child(newChild.id).setValue(newChild)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Çocuk başarıyla "child" düğümüne eklendi
                                fetchChildrenData() // Arayüzü yenile
                            } else {
                                // Çocuğun "child" düğümüne eklenmesi başarısız oldu
                                Log.e(
                                    TAG,
                                    "Failed to add child to the 'child' node: ${task.exception}"
                                )
                                // Hatası burada ele alabilirsiniz
                            }
                        }
                } else {
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


    // Çocuk verilerini Firebase'den almak için fonksiyon
    private fun fetchChildrenData() {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

        currentUserEmail?.let { email ->
            val query: Query =
                databaseReference.child("child").orderByChild("parentMail").equalTo(email)
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val kidsList: MutableList<User> = mutableListOf()
                    for (childSnapshot in snapshot.children) {
                        val child: User? = childSnapshot.getValue(User::class.java)
                        child?.let { kidsList.add(it) }
                    }
                    adapter.updateData(kidsList)

                    // Alınan verileri yazdır
                    println("Alınan ${kidsList.size} kullanıcılar")
                    kidsList.forEachIndexed { index, user ->
                        println("Kullanıcı ${index + 1}: $user")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // onCancelled'ı ele al
                    println("Alma işlemi iptal edildi: ${error.message}")
                }
            })
        } ?: run {
            // Eğer mevcut kullanıcı yoksa bir hata mesajı yazdır
            println("Mevcut kullanıcı bulunamadı.")
        }
    }

    // Rastgele bir şifre oluşturmak için fonksiyon
    private fun generateRandomPassword(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    // Function to show the material dialog for confirmation
    private fun showDeleteChildConfirmationDialog(childId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Child")
            .setMessage("Are you sure you want to delete this child?")
            .setPositiveButton("OK") { dialog, _ ->
                // Call a function to delete the child from Firebase
                deleteChild(childId)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Function to delete the child from Firebase
    private fun deleteChild(childId: String) {
        val childRef = databaseReference.child("child").child(childId)
        childRef.removeValue()
            .addOnSuccessListener {
                // Child successfully deleted
                fetchChildrenData() // Refresh the UI after deletion
            }
            .addOnFailureListener { exception ->
                // Failed to delete child
                Log.e(TAG, "Failed to delete child: $exception")
                // Handle the failure here
            }
    }

    // Update the onDeleteChildClick function
    override fun onDeleteChildClick(position: Int) {
        val childId = kidsList[position].id
        showDeleteChildConfirmationDialog(childId)
    }

    private fun switchToHomeFragment() {
        if (isAdded) {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, HomeFragment())
                .commit()
            (requireActivity() as MenuActivity).updateSelectedNavItem(R.id.home)
        }
    }

    private fun whenBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                switchToHomeFragment()
            }
        })
    }
}



