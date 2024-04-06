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
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        // Initialize Firebase components
        databaseReference = FirebaseDatabase.getInstance().reference.child("users")
        auth = Firebase.auth

        // Fetch budgets from Firebase
        fetchBudgetsFromFirebase()

        binding.availableBalance.text = "10.000"
        binding.currency.text = "$"

        animateButton(binding.btnIncome, true)
        animateButton(binding.btnOutcome, false)

        return view
    }

    private fun fetchBudgetsFromFirebase() {
        val userId = auth.currentUser?.uid
        userId?.let { uid ->
            databaseReference.child(uid).child("budgets").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val incomeLabels = mutableListOf<String>()
                    val incomes = mutableListOf<Double>()
                    val outcomeLabels = mutableListOf<String>()
                    val outcomes = mutableListOf<Double>()

                    for (budgetSnapshot in snapshot.children) {
                        val budget = budgetSnapshot.getValue(Budget::class.java)
                        budget?.let {
                            if (it.type == "Income") {
                                incomeLabels.add(it.title)
                                incomes.add(it.amount.toDouble())
                            } else {
                                outcomeLabels.add(it.title)
                                outcomes.add(it.amount.toDouble())
                            }
                        }
                    }

                    // Create pie chart
                    val pieChart = createPieChart()
                    val colors = listOf(
                        ContextCompat.getColor(requireContext(), R.color.colorBlue),
                        ContextCompat.getColor(requireContext(), R.color.colorRed),
                        ContextCompat.getColor(requireContext(), R.color.colorGreen),
                        ContextCompat.getColor(requireContext(), R.color.colorYellow)
                    )

                    // Display income data on the pie chart initially
                    updatePieChartDataSet(pieChart, incomeLabels, incomes, colors)
                    binding.chartContainer.addView(pieChart)

                    // Set up button click listeners to switch between income and expense data
                    binding.btnIncome.setOnClickListener {
                        animateButton(binding.btnIncome, true)
                        animateButton(binding.btnOutcome, false)
                        updatePieChartDataSet(pieChart, incomeLabels, incomes, colors)
                    }

                    binding.btnOutcome.setOnClickListener {
                        animateButton(binding.btnIncome, false)
                        animateButton(binding.btnOutcome, true)
                        updatePieChartDataSet(pieChart, outcomeLabels, outcomes, colors)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    private fun createPieChart(): PieChart {
        val pieChart = PieChart(requireContext())
        pieChart.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return pieChart
    }

    private fun updatePieChartDataSet(pieChart: PieChart, labels: List<String>, income: List<Double>, colors: List<Int>) {
        // Prepare data entries
        val entries = mutableListOf<PieEntry>()
        for (i in labels.indices) {
            entries.add(PieEntry(income[i].toFloat(), labels[i]))
        }

        // Set up PieDataSet
        val dataSet = PieDataSet(entries, "Budgets")
        dataSet.colors = colors
        dataSet.valueTextSize = 16f
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTypeface = Typeface.DEFAULT_BOLD // Set the typeface

        // Create PieData and set it to PieChart
        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.invalidate()
    }

    private fun animateButton(button: Button, isSelected: Boolean) {
        // Update the button's selected state
        button.isSelected = isSelected

        // Set the background drawable based on the button's selected state
        button.background = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.btn_selector
        )

        // Animate the button
        val scaleValue = if (isSelected) 1.05f else 1.0f
        val scaleX = ObjectAnimator.ofFloat(button, View.SCALE_X, scaleValue)
        val scaleY = ObjectAnimator.ofFloat(button, View.SCALE_Y, scaleValue)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 300
        animatorSet.start()
    }
}




