package com.example.financeapp.activity.menu.navigation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.financeapp.R
import com.example.financeapp.data.Budget
import com.example.financeapp.databinding.FragmentHomeBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase

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

        // Kullanılabilir bakiyeyi ayarla
        binding.availableBalance.text = "10.000"
        // Para birimini ayarla
        binding.currency.text = "$"

        // Butonları animasyonla
        animateButton(binding.btnIncome, true)
        animateButton(binding.btnOutcome, false)

        return view
    }

    //Budgetleri firebase çeken fonksiyon
    private fun fetchBudgetsFromFirebase() {
        // Kullanıcı kimliğini al
        val userId = auth.currentUser?.uid
        userId?.let { uid ->
            databaseReference.child(uid).child("budgets")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val labels = mutableListOf<String>()
                        val amounts = mutableListOf<Double>()
                        val colors = mutableListOf<String>()

                        // Bütün bütçeleri döngüye al
                        for (budgetSnapshot in snapshot.children) {
                            // Budget nesnesini al
                            val budget = budgetSnapshot.getValue(Budget::class.java)
                            budget?.let {
                                // Etiketleri, miktarları ve renkleri listelere ekle
                                labels.add(it.title)
                                amounts.add(it.amount.toDouble())
                                // Geçersiz renk dizelerini ele al
                                println("RENK BURADA:"+it.color)
                                colors.add(it.color)
                            }
                        }

                        // Pasta grafiği oluştur
                        val pieChart = createPieChart()

                        // Verileri pasta grafiğinde göster
                        updatePieChartDataSet(pieChart, labels, amounts, colors)
                        binding.chartContainer.addView(pieChart)
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
    private fun updatePieChartDataSet(pieChart: PieChart, labels: List<String>, income: List<Double>, colors: List<String>) {
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
