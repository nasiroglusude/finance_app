package com.example.financeapp.activity.menu.navigation

import android.app.Dialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.financeapp.R
import com.example.financeapp.activity.menu.MenuActivity
import com.example.financeapp.adapter.CategoryAdapter
import com.example.financeapp.databinding.DialogAddCategoryBinding
import com.example.financeapp.databinding.FragmentNewBudgetBinding
import com.example.financeapp.enums.Currency
import com.example.financeapp.enums.Repetition
import com.example.financeapp.model.Budget
import com.example.financeapp.model.Category
import com.example.financeapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.CoroutineContext

class NewBudgetFragment : Fragment(), CoroutineScope {
    private lateinit var binding: FragmentNewBudgetBinding
    private lateinit var dialogBinding: DialogAddCategoryBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var currencySymbol: String

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewBudgetBinding.inflate(inflater, container, false)
        dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        currencySymbol = ""


        // Kategori spinner adaptörünü başlat
        categoryAdapter = CategoryAdapter(requireContext(), mutableListOf()) // Boş bir listeyle başlat
        binding.categorySpinner.adapter = categoryAdapter // Adaptörü ayarla

        fetchUserPreferences()
        launch {
            updateCategorySpinner()
        }
        setRepetitionSpinner()
        setCurrencySpinner()
        setListeners()
    }

    private fun setListeners() {
        binding.btnAddNewCategory.setOnClickListener {
            showAddCategoryDialog()
        }
        binding.saveButton.setOnClickListener {
            launch {
                saveBudgetToFirebase()
            }
        }
        binding.categorySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedCategory = categoryAdapter.getItem(position)
                    // Get the Drawable from the View
                    val categoryColor = selectedCategory?.color
                    categoryColor?.let {
                        val circleDrawable = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.circle_background
                        )?.mutate()
                        circleDrawable?.let { drawable ->
                            // Change the color of the drawable
                            val selectedColor =
                                selectedCategory.color ?: "#000000" // Default color if null
                            drawable.setColorFilter(
                                Color.parseColor(selectedColor),
                                PorterDuff.Mode.SRC_IN
                            )

                            // Set the modified drawable as the background of the View
                            binding.colorPreview.background = drawable
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Bir şey seçilmediğinde işle
                }
            }
    }

    private fun setCurrencySpinner() {
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            Currency.entries.map { it.code +" "+ it.symbol }
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.currencySpinner.adapter = spinnerAdapter
    }
    private fun setRepetitionSpinner() {
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            Repetition.entries.map { it.displayName}
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.repetitionSpinner.adapter = spinnerAdapter
    }

    //Spinnerin içeriğini firebasein categories tablosundan gelen kategorilerin title attributlerini atama
    private fun updateCategorySpinner() {
        val userId = firebaseAuth.currentUser?.uid
        userId?.let { uid ->
            FirebaseDatabase.getInstance().reference.child("users").child(uid).child("categories")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val categories = mutableListOf<Category>()
                        for (categorySnapshot in snapshot.children) {
                            val category = categorySnapshot.getValue(Category::class.java)
                            category?.let {
                                categories.add(it)
                            }
                        }
                        println("NewBudgetActivity" + " Kategoriler: $categories")

                        // Kategorileri alındı, adaptörü güncelle
                        categoryAdapter.clear()
                        categoryAdapter.addAll(categories)
                        categoryAdapter.notifyDataSetChanged()

                        // Spinner'a adaptörü ayarla
                        binding.categorySpinner.adapter = categoryAdapter
                    }

                    override fun onCancelled(error: DatabaseError) {
                        println("NewBudgetActivity" + " Veritabanı hatası: ${error.message}")
                        // Hata durumunu ele al
                    }
                })
        }
    }

    private fun fetchUserPreferences() {
        val currentUser = firebaseAuth.currentUser?.uid
        currentUser?.let { uid ->
            val preferencesRef = FirebaseDatabase.getInstance().reference.child("users").child(uid)
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


    //Yeni kategori oluşturma dialoğunu kuran fonksiyon
    private fun showAddCategoryDialog() {
        val dialog = Dialog(requireContext())
        dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val colorPickerView = dialogBinding.colorPickerView
        val setButton = dialogBinding.setButton
        val categoryEditText = dialogBinding.categoryName
        colorPickerView.setColorListener(ColorEnvelopeListener { envelope, _ ->
            val selectedColor = envelope.color
            dialogBinding.colorPreview.setBackgroundColor(selectedColor)
        })

        setButton.setOnClickListener {
            val categoryName = categoryEditText.text.toString()
            val selectedColor = dialogBinding.colorPickerView.color

            // Kategoriyi Firebase'e kaydet
            launch {
                saveCategoryToFirebase(categoryName, selectedColor)
            }
            updateCategorySpinner()
            dialog.dismiss()
        }

        dialog.show()
    }

    //Kategoriyi firebase'e kaydeden fonksiyon
    private fun saveCategoryToFirebase(categoryName: String, color: Int) {
        val userId = firebaseAuth.currentUser?.uid
        userId?.let { uid ->
            val categoryId = FirebaseDatabase.getInstance().reference.child("users").child(uid)
                .child("categories").push().key

            val category = Category(
                categoryId ?: "",
                categoryName,
                "#" + Integer.toHexString(color)
            )

            FirebaseDatabase.getInstance().reference.child("users").child(uid).child("categories")
                .child(categoryId ?: "").setValue(category)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Kategori başarıyla kaydedildi
                    } else {
                        // Hata durumunu ele al
                    }
                }
        }
    }

    private fun fetchCategoryFromFirebase(
        categoryId: String,
        onSuccess: (Category) -> Unit,
        onFailure: () -> Unit
    ) {
        val userId = firebaseAuth.currentUser?.uid
        userId?.let { uid ->
            val categoryRef = FirebaseDatabase.getInstance().reference
                .child("users")
                .child(uid)
                .child("categories")
                .child(categoryId)

            categoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val category = dataSnapshot.getValue(Category::class.java)
                    if (category != null) {
                        onSuccess(category)
                    } else {
                        onFailure()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    onFailure()
                }
            })
        }
    }


    //Bütçeyi firebase'e kaydeden fonksiyon
    private fun saveBudgetToFirebase() {
        val userId = firebaseAuth.currentUser?.uid
        // Capture the context
        userId?.let { uid ->
            val budgetId = FirebaseDatabase.getInstance().reference.child("users").child(uid)
                .child("budgets").push().key
            val title = binding.title.text.toString()
            val amount = binding.amount.text.toString()
            //val currency = Currency.entries[binding.currencySpinner.selectedItemPosition].code
            val repetition = Repetition.entries[binding.repetitionSpinner.selectedItemPosition].code
            val typeRadioButtonId = binding.budgetTypeRadioGroup.checkedRadioButtonId
            val type = if (typeRadioButtonId == binding.incomeRadioButton.id) "Income" else "Expense"
            val selectedCategory = binding.categorySpinner.selectedItem as? Category
            val categoryName = selectedCategory?.title ?: ""
            val currentDateString = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
            if (selectedCategory != null) {
                launch {
                    fetchCategoryFromFirebase(selectedCategory.id,
                        onSuccess = { category ->
                            val colorHexString = category.color // Move this inside the coroutine

                            val budget = Budget(
                                budgetId ?: "",
                                title,
                                amount,
                                colorHexString,
                                currencySymbol,
                                type,
                                repetition,
                                categoryName,
                                currentDateString,
                                currentDateString,
                                false
                            )

                            // Now use the context captured earlier
                            FirebaseDatabase.getInstance().reference.child("users").child(uid)
                                .child("budgets").child(budgetId ?: "").setValue(budget)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        switchToHomeFragment()
                                    } else {
                                        // Hata durumunu ele al
                                    }
                                }
                        },
                        onFailure = {
                            // Failed to fetch category color
                            // Handle the failure case
                        }
                    )
                }
            }
        }
    }

    private fun switchToHomeFragment() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, HomeFragment())
            .commit()
        (requireActivity() as MenuActivity).updateSelectedNavItem(R.id.home)
    }

}
