package com.example.financeapp.activity.menu.navigation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.financeapp.R
import com.example.financeapp.data.Budget
import com.example.financeapp.data.User
import com.example.financeapp.databinding.FragmentHomeBinding
import com.example.financeapp.enums.Currency
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import java.util.prefs.Preferences

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // FragmentHomeBinding'i şişir
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        // Firebase bileşenlerini başlat
        databaseReference = FirebaseDatabase.getInstance().reference.child("users")
        auth = Firebase.auth

        // Firebase'den bütçeleri al
        fetchBudgetsFromFirebase()
        fetchUserPreferencesFromFirebase()

        // Butonları animasyonla
        animateButton(binding.btnIncome, true)
        animateButton(binding.btnOutcome, false)

        return view
    }

    private fun fetchUserPreferencesFromFirebase() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val userRef = databaseReference.child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val userData = dataSnapshot.getValue(User::class.java)
                        if (userData != null) {
                            val currency = userData.currency
                            val balance = userData.balance

                            updateUI(balance, currency)
                        } else {
                            Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle database error
                    Toast.makeText(requireContext(), "Failed to fetch user data", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun updateUI(availableBalance: String, currency:String) {
        // Set available balance with currency symbol
        binding.availableBalance.text = availableBalance
        val currencySymbol = Currency.valueOf(currency).symbol
        // Set currency symbol
        binding.currency.text = currencySymbol
    }

    //Budgetleri firebase çeken fonksiyon
    private fun fetchBudgetsFromFirebase() {
        // Kullanıcı kimliğini al
        val userId = auth.currentUser?.uid
        userId?.let { uid ->
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
