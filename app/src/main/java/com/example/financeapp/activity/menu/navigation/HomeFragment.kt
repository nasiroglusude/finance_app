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
import com.example.financeapp.R
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.financeapp.databinding.FragmentHomeBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class HomeFragment : Fragment(){

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
        auth = FirebaseAuth.getInstance()

        binding.availableBalance.text = "10.000"
        binding.currency.text = "$"

        animateButton(binding.btnIncome, true)
        animateButton(binding.btnOutcome, false)
        val incomeLabels = listOf("Interest", "Bitcoin", "Wage", "Gold")
        val incomes = listOf(16000.0, 20000.0, 30000.0, 42000.0)

        val outcomeLabels = listOf("Mortgage", "Rent", "School", "Invest")
        val outcomes = listOf(11000.0, 36000.0, 5000.0, 3000.0)
        //val newColors = listOf(Color.MAGENTA, Color.CYAN, Color.GRAY, Color.LTGRAY)

        val blueColor = ContextCompat.getColor(requireContext(), R.color.colorBlue)
        val redColor = ContextCompat.getColor(requireContext(), R.color.colorRed)
        val greenColor = ContextCompat.getColor(requireContext(), R.color.colorGreen)
        val yellowColor = ContextCompat.getColor(requireContext(), R.color.colorYellow)

        val newColors = listOf(blueColor, redColor, greenColor, yellowColor)


        val pieChart = createPieChart()
        updatePieChartDataSet(pieChart, incomeLabels, incomes, newColors)
        binding.chartContainer.addView(pieChart)

        binding.btnIncome.setOnClickListener{
            animateButton(binding.btnIncome, true)
            animateButton(binding.btnOutcome, false)
            updatePieChartDataSet(pieChart, incomeLabels, incomes, newColors)
        }

        binding.btnOutcome.setOnClickListener{
            animateButton(binding.btnIncome, false)
            animateButton(binding.btnOutcome, true)
            updatePieChartDataSet(pieChart, outcomeLabels, outcomes, newColors)
        }

        return view
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
        val dataSet = PieDataSet(entries, "Income by Month")
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



