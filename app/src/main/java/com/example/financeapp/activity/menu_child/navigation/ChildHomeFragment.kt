package com.example.financeapp.activity.menu_child.navigation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil.bind
import androidx.databinding.DataBindingUtil.setContentView
import androidx.fragment.app.Fragment
import com.example.financeapp.R
import com.example.financeapp.activity.menu_child.ChildNewBudgetActivity
import com.example.financeapp.data.Budget
import com.example.financeapp.databinding.ActivityControlledChildBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChildHomeFragment : Fragment() {

    private lateinit var binding: ActivityControlledChildBinding
    private lateinit var auth: FirebaseAuth
    private var childId: String? = null
    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // ViewBinding'i başlat
        binding = ActivityControlledChildBinding.inflate(layoutInflater)
        val view = binding.root

        databaseReference = FirebaseDatabase.getInstance().reference.child("child")

        childId = arguments?.getString("childId")


        // Firebase'den bütçeleri al
        fetchBudgetsFromFirebase()
        // Butonları animasyonla
        animateButton(binding.btnIncome, true)
        animateButton(binding.btnOutcome, false)

        //Çocuğun parentMail fieldi ile eşelşen maile sahip userın verilerini çekmek
        childId?.let { id ->
            fetchChildAttributeById(id, "balance") { balance ->
                balance?.let { availableBalance ->
                    binding.availableBalance.text = availableBalance
                }

            }
        }

        /*
        fetchParentAttributes(childId, "firstName") { fullName ->
            fullName?.let {
                println("Ebeveyn Adı: $it")
            } ?: println("Çocuk için ebeveyn adı bulunamadı.")
        }*/

        return view
    }


    private fun fetchParentAttributes(
        childId: String,
        attributeName: String,
        onAttributesFetched: (attributes: String?) -> Unit
    ) {
        fetchChildAttributeById(childId, "parentMail") { parentMail ->
            parentMail?.let { mail ->
                fetchUserByParentEmail(mail, attributeName) { attribute ->
                    onAttributesFetched(attribute)
                }
            } ?: onAttributesFetched(null)
        }
    }


    private fun fetchChildAttributeById(
        childId: String,
        attribute: String,
        callback: (String?) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance().reference
        val childRef = database.child("child").child(childId)

        childRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val attributeValue = dataSnapshot.child(attribute).getValue(String::class.java)
                    callback(attributeValue)
                } else {
                    // Veri bulunamadığında null değer döndür
                    callback(null)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Hata durumunda yapılacak işlemler
                callback(null)
            }
        })
    }


    private fun fetchUserByParentEmail(
        parentEmail: String,
        attribute: String,
        callback: (String?) -> Unit
    ) {
        val database = FirebaseDatabase.getInstance().reference
        val usersRef = database.child("users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    val userEmail = userSnapshot.child("email").getValue(String::class.java)
                    if (userEmail == parentEmail) {
                        // Belirli bir parent email adresine sahip kullanıcıyı bulduk
                        val attributeValue =
                            userSnapshot.child(attribute).getValue(String::class.java)
                        callback(attributeValue)
                        return
                    }
                }
                // Belirli bir parent email adresine sahip kullanıcı bulunamadı
                callback(null)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Hata durumunda yapılacak işlemler
                callback(null)
            }
        })
    }

    //Budgetleri firebase çeken fonksiyon
    private fun fetchBudgetsFromFirebase() {
        childId?.let { uid ->
            databaseReference.child(uid).child("budgets")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val incomeLabels = mutableListOf<String>()
                        val incomes = mutableListOf<Double>()
                        val outcomeLabels = mutableListOf<String>()
                        val outcomes = mutableListOf<Double>()
                        val incomeColors = mutableListOf<String>()
                        val outcomeColors = mutableListOf<String>()


                        // Bütün bütçeleri döngüye al
                        for (budgetSnapshot in snapshot.children) {
                            // Budget nesnesini al
                            val budget = budgetSnapshot.getValue(Budget::class.java)
                            budget?.let {
                                if (it.type == "Income") {
                                    incomeLabels.add(it.title)
                                    incomes.add(it.amount.toDouble())
                                    incomeColors.add(it.color)

                                } else {
                                    outcomeLabels.add(it.title)
                                    outcomes.add(it.amount.toDouble())
                                    outcomeColors.add(it.color)

                                }
                            }
                        }

                        // Pasta grafiği oluştur
                        val pieChart = createPieChart()

                        // Verileri pasta grafiğinde göster
                        updatePieChartDataSet(pieChart, incomeLabels, incomes, incomeColors)
                        binding.chartContainer.addView(pieChart)

                        // Set up button click listeners to switch between income and expense data
                        binding.btnIncome.setOnClickListener {
                            animateButton(binding.btnIncome, true)
                            animateButton(binding.btnOutcome, false)
                            updatePieChartDataSet(pieChart, incomeLabels, incomes, incomeColors)
                        }

                        binding.btnOutcome.setOnClickListener {
                            animateButton(binding.btnIncome, false)
                            animateButton(binding.btnOutcome, true)
                            updatePieChartDataSet(pieChart, outcomeLabels, outcomes, outcomeColors)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Hata durumunu ele al
                    }
                })
        }
    }


    //PieChart'ı oluşturan fonksiyon
    private fun createPieChart(): PieChart {
        // Pasta grafiğini oluştur
        val pieChart = PieChart(requireContext())
        pieChart.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return pieChart
    }

    //Pie Chartı verilen label-value ve color değerlerine göre güncelleyen fonksiyon
    private fun updatePieChartDataSet(
        pieChart: PieChart,
        labels: List<String>,
        income: List<Double>,
        colors: List<String>
    ) {
        // Veri girişlerini hazırla
        val entries = mutableListOf<PieEntry>()
        for (i in labels.indices) {
            entries.add(PieEntry(income[i].toFloat(), labels[i]))
        }

        // Renk dizelerini renk tamsayılarına dönüştür
        val parsedColors = colors.map { Color.parseColor(it) }

        // Pasta veri kümesini ayarla
        val dataSet = PieDataSet(entries, "Bütçeler")
        dataSet.colors = parsedColors.toMutableList() // Renkleri Pasta veri kümesine ata

        dataSet.valueTextSize = 16f
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTypeface = Typeface.DEFAULT_BOLD // Yazı tipini ayarla

        // Pasta verisini oluştur ve Pasta grafiğine ayarla
        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate()
    }


    private fun animateButton(button: Button, isSelected: Boolean) {
        // Butonun seçili durumunu güncelle
        button.isSelected = isSelected

        // Butonun seçili durumuna göre arka plan drawable'ını ayarla
        button.background = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.btn_selector
        )

        // Butonu animasyonla
        val scaleValue = if (isSelected) 1.05f else 1.0f
        val scaleX = ObjectAnimator.ofFloat(button, View.SCALE_X, scaleValue)
        val scaleY = ObjectAnimator.ofFloat(button, View.SCALE_Y, scaleValue)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 300
        animatorSet.start()
    }

}
