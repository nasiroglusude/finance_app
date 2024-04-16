package com.example.financeapp.activity.menu

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.financeapp.adapter.CategoryAdapter
import com.example.financeapp.databinding.ActivityNewBudgetBinding
import com.example.financeapp.enums.Currency
import com.example.financeapp.data.Budget
import com.example.financeapp.data.Category
import com.example.financeapp.databinding.ActivityMenuBinding
import com.example.financeapp.databinding.DialogAddCategoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

class NewBudgetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewBudgetBinding
    private lateinit var dialogBinding: DialogAddCategoryBinding
    private lateinit var menuBinding: ActivityMenuBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var categoryAdapter: CategoryAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuBinding = ActivityMenuBinding.inflate(layoutInflater)

        binding = ActivityNewBudgetBinding.inflate(layoutInflater)
        dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)

        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.btnAddNewCategory.setOnClickListener {
            showAddCategoryDialog()
        }
        binding.saveButton.setOnClickListener {
            saveBudgetToFirebase()
        }
        binding.backButton.setOnClickListener{

        }


        // Kategori spinner adaptörünü başlat
        categoryAdapter = CategoryAdapter(this, mutableListOf()) // Boş bir listeyle başlat
        binding.categorySpinner.adapter = categoryAdapter // Adaptörü ayarla
        populateCategorySpinner()

        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categoryAdapter.getItem(position)
                val categoryColor = selectedCategory?.color
                categoryColor?.let {
                    binding.colorPreview.setBackgroundColor(Color.parseColor(it))
                    val backgroundColor = (binding.colorPreview.background as? ColorDrawable)?.color
                    println("Renk:" + backgroundColor)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Bir şey seçilmediğinde işle
            }
        }


        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            Currency.entries.map { it.displayName }
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.currencySpinner.adapter = spinnerAdapter





    }

    //Spinnerin içeriğini firebasein categories tablosundan gelen kategorilerin title attributlerini atama
    private fun populateCategorySpinner() {
        val userId = firebaseAuth.currentUser?.uid
        userId?.let { uid ->
            FirebaseDatabase.getInstance().reference.child("users")
                .child("categories").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val categories = mutableListOf<Category>()
                        for (categorySnapshot in snapshot.children) {
                            val category = categorySnapshot.getValue(Category::class.java)
                            category?.let {
                                categories.add(it)
                            }
                        }
                        println("NewBudgetActivity"+" Kategoriler: $categories")

                        // Kategorileri alındı, adaptörü güncelle
                        categoryAdapter.clear()
                        categoryAdapter.addAll(categories)
                        categoryAdapter.notifyDataSetChanged()

                        // Spinner'a adaptörü ayarla
                        binding.categorySpinner.adapter = categoryAdapter
                    }

                    override fun onCancelled(error: DatabaseError) {
                        println("NewBudgetActivity"+ " Veritabanı hatası: ${error.message}")
                        // Hata durumunu ele al
                    }
                })
        }
    }

    //Yeni kategori oluşturma dialoğunu kuran fonksiyon
    private fun showAddCategoryDialog() {
        val dialog = Dialog(this)
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
            saveCategoryToFirebase(categoryName, selectedColor)
            populateCategorySpinner()
            dialog.dismiss()
        }

        dialog.show()
    }

    //Kategoriyi firebase'e kaydeden fonksiyon
    private fun saveCategoryToFirebase(categoryName: String, color: Int) {
        val userId = firebaseAuth.currentUser?.uid
        userId?.let { uid ->
            val categoryId = FirebaseDatabase.getInstance().reference.child("users")
                .child("categories").push().key

            val category = Category(
                categoryId ?: "",
                categoryName,
                "#" + Integer.toHexString(color)
            )

            FirebaseDatabase.getInstance().reference.child("users")
                .child("categories").child(categoryId ?: "").setValue(category)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Kategori başarıyla kaydedildi
                    } else {
                        // Hata durumunu ele al
                    }
                }
        }
    }

    //Bütçeyi firebase'e kaydeden fonksiyon
    private fun saveBudgetToFirebase() {
        val userId = firebaseAuth.currentUser?.uid
        userId?.let { uid ->
            val budgetId = FirebaseDatabase.getInstance().reference.child("users").child(uid)
                .child("budgets").push().key
            val title = binding.title.text.toString()
            val amount = binding.amount.text.toString()
            val color = "#" + Integer.toHexString((binding.colorPreview.background as? ColorDrawable)?.color ?: Color.BLACK)

            val currency = Currency.entries[binding.currencySpinner.selectedItemPosition].code
            val radioButtonId = binding.budgetTypeRadioGroup.checkedRadioButtonId
            val type = if (radioButtonId == binding.incomeRadioButton.id) "Gelir" else "Gider"
            val selectedCategory = binding.categorySpinner.selectedItem as? Category
            val categoryName = selectedCategory?.title ?: ""

            val budget = Budget(
                budgetId ?: "",
                title,
                amount,
                color,
                currency,
                type,
                categoryName
            )

            FirebaseDatabase.getInstance().reference.child("users").child(uid)
                .child("budgets").child(budgetId ?: "").setValue(budget)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Bütçe başarıyla kaydedildi
                        finish()
                    } else {
                        // Hata durumunu ele al
                    }
                }
        }
    }

}
