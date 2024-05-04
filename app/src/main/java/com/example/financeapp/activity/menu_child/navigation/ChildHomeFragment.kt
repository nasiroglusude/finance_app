package com.example.financeapp.activity.menu_child.navigation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.example.financeapp.R
import com.example.financeapp.activity.menu_child.ChildMenuActivity
import com.example.financeapp.adapter.HorizontalCalendarAdapter
import com.example.financeapp.model.Budget
import com.example.financeapp.model.DateCalendar
import com.example.financeapp.databinding.ActivityControlledChildBinding
import com.example.financeapp.enums.Currency
import com.example.financeapp.model.Child
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.CoroutineContext

class ChildHomeFragment : Fragment(), HorizontalCalendarAdapter.OnItemClickListener,
    CoroutineScope {

    private lateinit var binding: ActivityControlledChildBinding
    private lateinit var auth: FirebaseAuth
    private var childId: String? = null
    private lateinit var databaseReference: DatabaseReference

    private val currentDate = Calendar.getInstance(Locale.ENGLISH)
    private val sdf = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
    private val cal = Calendar.getInstance(Locale.ENGLISH)

    private lateinit var adapter: HorizontalCalendarAdapter
    private val calendarList2 = ArrayList<DateCalendar>()
    private var isIncomeSelected = true

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivityControlledChildBinding.inflate(layoutInflater)
        val view = binding.root

        databaseReference = FirebaseDatabase.getInstance().reference.child("child")
        auth = Firebase.auth

        childId = arguments?.getString("childId")
        println("GELDİÜİİİ"+childId)

        //Çocuğun parentMail fieldi ile eşelşen maile sahip userın verilerini çekmek
        launch {
            fetchChildPreferencesFromFirebase()

        }
        // Butonları animasyonla
        animateButton(binding.btnIncome, true)
        animateButton(binding.btnOutcome, false)

        val calendar = setCalendar(binding.recyclerView, this@ChildHomeFragment)
        val monthIndex = calendar.get(Calendar.MONTH)
        val monthName = DateFormatSymbols().months[monthIndex].toString()
        val calendarYear = calendar.get(Calendar.YEAR)

        val currentMonthString = "$monthName $calendarYear"
        binding.tvDate.text = currentMonthString

        launch {
            fetchBudgetsFromFirebase(monthIndex.toString(), calendarYear.toString())
        }

        setUpCalendarPrevNextClickListener(binding.ivCalendarNext, binding.ivCalendarPrevious) { currentMonthString ->
            // Callback when month changes
            // Update UI with the selected month and year
            binding.tvDate.text = currentMonthString
            val calendar1 = setCalendar(binding.recyclerView, this@ChildHomeFragment)
            val monthIndex1 = calendar1.get(Calendar.MONTH)
            val calendarYear1 = calendar1.get(Calendar.YEAR)

            launch {
                fetchBudgetsFromFirebase(monthIndex1.toString(), calendarYear1.toString())
            }
        }
        whenBackPressed()

        return view
    }

    override fun onItemClick(ddMmYy: String, dd: String, day: String) {
        val selectedDate = "$day $dd, $ddMmYy"
        Toast.makeText(requireContext(), "Selected Date: $selectedDate", Toast.LENGTH_SHORT).show()
    }

    private fun setCalendar(recyclerView: RecyclerView, listener: HorizontalCalendarAdapter.OnItemClickListener): Calendar {
        // Check if a SnapHelper is already attached
        if (recyclerView.onFlingListener == null) {
            val snapHelper: SnapHelper = LinearSnapHelper()
            snapHelper.attachToRecyclerView(recyclerView)
        }

        adapter = HorizontalCalendarAdapter { calendarDateModel: DateCalendar, position: Int ->
            calendarList2.forEachIndexed { index, calendarModel ->
                calendarModel.isSelected = index == position
            }
        }
        adapter.setData(calendarList2)
        adapter.setOnItemClickListener(listener)
        recyclerView.adapter = adapter
        // Get the month from the calendar and return it
        return cal
    }



    private fun fetchChildPreferencesFromFirebase() {
        childId?.let { uid ->
            val childRef = databaseReference.child(uid)
            childRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val childData = dataSnapshot.getValue(Child::class.java)
                        if (childData != null) {
                            val currency = childData.currency
                            val balance = childData.balance
                            if (currency.isNotEmpty() && balance.isNotEmpty())
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
        binding.availableBalance.text = availableBalance.toString() // Convert to string if necessary
        val currencySymbol = Currency.valueOf(currency).symbol
        binding.currency.text = currencySymbol
    }

    //Budgetleri firebase çeken fonksiyon
    private fun fetchBudgetsFromFirebase(selectedMonth: String, selectedYear: String) {
        childId?.let { uid ->
            val budgetRef = databaseReference.child(uid).child("budgets")
            budgetRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val incomeLabels = mutableListOf<String>()
                    val incomes = mutableListOf<Double>()
                    val outcomeLabels = mutableListOf<String>()
                    val outcomes = mutableListOf<Double>()
                    val incomeColors = mutableListOf<String>()
                    val outcomeColors = mutableListOf<String>()

                    val currentCalendar = Calendar.getInstance()
                    val currentMonth = currentCalendar.get(Calendar.MONTH)
                    val currentYear = currentCalendar.get(Calendar.YEAR)

                    snapshot.children.forEach { budgetSnapshot ->
                        val budget = budgetSnapshot.getValue(Budget::class.java)
                        budget?.let {
                            val repetition = it.repetition

                            when (repetition) {
                                //Tek seferlik
                                "none" -> {
                                    updateBudgetForNoneRepetition(uid, it, incomeLabels, incomes, outcomeLabels, outcomes, incomeColors, outcomeColors)
                                }
                                "added" -> {
                                    updateBudgetForAddedRepetition(it,incomeLabels, incomes, outcomeLabels, outcomes, incomeColors, outcomeColors, selectedMonth, selectedYear)
                                }
                                //Aylık
                                "monthly" -> {
                                    updateBudgetForMonthlyRepetition(uid, it, selectedMonth, selectedYear, currentMonth, currentYear, incomeLabels, incomes, outcomeLabels, outcomes, incomeColors, outcomeColors)
                                }
                                //Yıllık
                                "annual" -> {
                                    updateBudgetForAnnualRepetition(uid, it, selectedMonth, selectedYear, currentMonth, currentYear, incomeLabels, incomes, outcomeLabels, outcomes, incomeColors, outcomeColors)
                                }
                            }
                        }
                    }

                    // Display or update the chart based on the collected data
                    updateChart(incomeLabels, incomes, outcomeLabels, outcomes, incomeColors, outcomeColors)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    private fun updateBudgetForNoneRepetition(
        uid: String,
        budget: Budget,
        incomeLabels: MutableList<String>,
        incomes: MutableList<Double>,
        outcomeLabels: MutableList<String>,
        outcomes: MutableList<Double>,
        incomeColors: MutableList<String>,
        outcomeColors: MutableList<String>
    )
    {
        val positiveBalance = budget.amount
        val negativeBalance = (positiveBalance.toInt()*-1).toString()
        updateBudgetRepetitionState(uid,budget ,"added")
        if (budget.type == "Income") {
            updateChildBalance(uid, positiveBalance)
            clearChart()
            incomeLabels.add(budget.title)
            incomes.add(budget.amount.toDouble())
            incomeColors.add(budget.color)
        } else {
            updateChildBalance(uid, negativeBalance)
            clearChart()
            outcomeLabels.add(budget.title)
            outcomes.add(budget.amount.toDouble())
            outcomeColors.add(budget.color)
        }
    }

    private fun updateBudgetForAddedRepetition(
        budget: Budget,
        incomeLabels: MutableList<String>,
        incomes: MutableList<Double>,
        outcomeLabels: MutableList<String>,
        outcomes: MutableList<Double>,
        incomeColors: MutableList<String>,
        outcomeColors: MutableList<String>,
        selectedMonth: String,
        selectedYear: String
    )
    {
        val creationMonth = getCreationMonth(budget)
        val creationYear = getCreationYear(budget)
        if (selectedMonth.toInt() == creationMonth && selectedYear.toInt() == creationYear)
        {
            if (budget.type == "Income") {
                clearChart()
                incomeLabels.add(budget.title)
                incomes.add(budget.amount.toDouble())
                incomeColors.add(budget.color)
            } else {
                clearChart()
                outcomeLabels.add(budget.title)
                outcomes.add(budget.amount.toDouble())
                outcomeColors.add(budget.color)
            }
        }
    }

    //Aylık
    private fun updateBudgetForMonthlyRepetition(
        uid: String,
        budget: Budget,
        selectedMonth: String,
        selectedYear: String,
        currentMonth: Int,
        currentYear: Int,
        incomeLabels: MutableList<String>,
        incomes: MutableList<Double>,
        outcomeLabels: MutableList<String>,
        outcomes: MutableList<Double>,
        incomeColors: MutableList<String>,
        outcomeColors: MutableList<String>
    ) {
        val creationMonth = getCreationMonth(budget)
        val creationYear = getCreationYear(budget)

        val lastUpdateMonth = getLastUpdateMonth(budget)
        val lastUpdateYear = getLastUpdateYear(budget)

        val isDueThisMonth = selectedMonth.toInt() == lastUpdateMonth && selectedYear.toInt() == lastUpdateYear

        val positiveBalance = budget.amount
        val negativeBalance = (positiveBalance.toInt()*-1).toString()

        val balanceChange = if (budget.type == "Income") positiveBalance else negativeBalance

        //İlk ekleme
        if (!budget.firstAddition && isDueThisMonth) {
            updateChildBalance(uid, balanceChange)
            updateBudgetAdditionState(uid, budget.id, true)
        }

        //Ay Geçerse
        val monthsPassed = (currentYear - lastUpdateYear) * 12 + (currentMonth - lastUpdateMonth)
        if (monthsPassed > 0 && (selectedMonth.toInt() >= creationMonth || selectedYear.toInt() > creationYear)) {
            val updatedAmount = budget.amount.toInt() * monthsPassed
            val updatedNegativeAmount = budget.amount.toInt() * monthsPassed *-1
            val updatedBalanceChange = if (budget.type == "Income") updatedAmount else updatedNegativeAmount
            updateChildBalance(uid, updatedBalanceChange.toString())
            updateBudgetLastUpdate(uid, budget.id)
        }

        //Sadece Çizdirme
        if (selectedMonth.toInt() >= creationMonth && selectedYear.toInt() == creationYear) {
            if (budget.type == "Income") {
                clearChart()
                incomeLabels.add(budget.title)
                incomes.add(budget.amount.toDouble())
                incomeColors.add(budget.color)
            } else {
                clearChart()
                outcomeLabels.add(budget.title)
                outcomes.add(budget.amount.toDouble())
                outcomeColors.add(budget.color)
            }
        }
        if (selectedYear.toInt() > creationYear) {
            if (budget.type == "Income") {
                clearChart()
                incomeLabels.add(budget.title)
                incomes.add(budget.amount.toDouble())
                incomeColors.add(budget.color)
            } else {
                clearChart()
                outcomeLabels.add(budget.title)
                outcomes.add(budget.amount.toDouble())
                outcomeColors.add(budget.color)
            }
        }
    }

    //Yıllık
    private fun updateBudgetForAnnualRepetition(uid: String, budget: Budget, selectedMonth: String, selectedYear: String, currentMonth: Int, currentYear: Int, incomeLabels: MutableList<String>, incomes: MutableList<Double>, outcomeLabels: MutableList<String>, outcomes: MutableList<Double>, incomeColors: MutableList<String>, outcomeColors: MutableList<String>) {
        val creationMonth = getCreationMonth(budget)
        val creationYear = getCreationYear(budget)

        val lastUpdateMonth = getLastUpdateMonth(budget)
        val lastUpdateYear = getLastUpdateYear(budget)

        val isDueThisYear = creationMonth == lastUpdateMonth && creationYear == lastUpdateYear

        val positiveBalance = budget.amount
        val negativeBalance = (positiveBalance.toInt()*-1).toString()

        val balanceChange = if (budget.type == "Income") positiveBalance else negativeBalance

        //First created
        if (!budget.firstAddition && isDueThisYear) {
            updateChildBalance(uid, balanceChange)
            updateBudgetAdditionState(uid, budget.id, true)
        }

        //When some years passed
        val yearsPassed = (currentYear-lastUpdateYear)
        if (yearsPassed > 0 && selectedYear.toInt() > creationYear) {
            val updatedAmount = budget.amount.toInt() * yearsPassed
            val updatedNegativeAmount = budget.amount.toInt() * yearsPassed * -1
            val updatedBalanceChange = if (budget.type == "Income") updatedAmount else updatedNegativeAmount

            updateChildBalance(uid, updatedBalanceChange.toString())
            updateBudgetLastUpdate(uid, budget.id)
        }

        //Just show all
        if (selectedMonth.toInt() == creationMonth && selectedYear.toInt() >= creationYear) {
            if (budget.type == "Income") {
                incomeLabels.add(budget.title)
                incomes.add(budget.amount.toDouble())
                incomeColors.add(budget.color)
            } else {
                outcomeLabels.add(budget.title)
                outcomes.add(budget.amount.toDouble())
                outcomeColors.add(budget.color)
            }
        }
    }
    /*
    private fun drawGraphIncome(budget: Budget, incomeLabels: MutableList<String>){
        clearChart()
        incomeLabels.add(budget.title)
        incomes.add(budget.amount.toDouble())
        incomeColors.add(budget.color)
    }
    */
    private fun getCreationMonth(budget: Budget): Int {
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val creationDate = dateFormat.parse(budget.creationDate) ?: Date()
        val calendar = Calendar.getInstance().apply { time = creationDate }
        return calendar.get(Calendar.MONTH)
    }

    private fun getCreationYear(budget: Budget): Int {
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val creationDate = dateFormat.parse(budget.creationDate) ?: Date()
        val calendar = Calendar.getInstance().apply { time = creationDate }
        return calendar.get(Calendar.YEAR)
    }

    private fun getLastUpdateMonth(budget: Budget): Int {
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val lastUpdateDate = budget.lastUpdate?.let { dateFormat.parse(it) } ?: Date()
        val calendar = Calendar.getInstance().apply { time = lastUpdateDate }
        return calendar.get(Calendar.MONTH)
    }

    private fun getLastUpdateYear(budget: Budget): Int {
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val lastUpdateDate = budget.lastUpdate?.let { dateFormat.parse(it) } ?: Date()
        val calendar = Calendar.getInstance().apply { time = lastUpdateDate }
        return calendar.get(Calendar.YEAR)
    }

    private fun updateChart(incomeLabels: MutableList<String>, incomes: MutableList<Double>, outcomeLabels: MutableList<String>, outcomes: MutableList<Double>, incomeColors: MutableList<String>, outcomeColors: MutableList<String>) {
        if (incomeLabels.isEmpty() && outcomeLabels.isEmpty()) {
            clearChart()
            showNoDataText()
        } else {
            val pieChart = createPieChart()
            if (pieChart != null) {
                updatePieChartDataSet(pieChart, incomeLabels, incomes, incomeColors)
                binding.chartContainer.addView(pieChart)

                binding.btnIncome.setOnClickListener {
                    isIncomeSelected = true
                    animateButton(binding.btnIncome, true)
                    animateButton(binding.btnOutcome, false)
                    updatePieChartDataSet(pieChart, incomeLabels, incomes, incomeColors)
                }

                binding.btnOutcome.setOnClickListener {
                    isIncomeSelected = false
                    animateButton(binding.btnIncome, false)
                    animateButton(binding.btnOutcome, true)
                    updatePieChartDataSet(pieChart, outcomeLabels, outcomes, outcomeColors)
                }
            }
        }
    }

    //PieChart'ı oluşturan fonksiyon
    private fun createPieChart(): PieChart? {
        // Check if the fragment is added
        if (!isAdded) {
            return null
        }

        // Fragment is added, create the PieChart
        val pieChart = PieChart(requireContext())
        pieChart.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Remove description label
        pieChart.description.isEnabled = false

        return pieChart
    }

    private fun clearChart() {
        binding.chartContainer.removeAllViews()
    }


    private fun showNoDataText() {
        // Check if the fragment is attached to a context
        if (isAdded) {
            // Create a TextView
            val noDataText = TextView(requireContext()).apply {
                text = "No data found."
                gravity = Gravity.CENTER // Center the text horizontally and vertically
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    // Set layout rules to center the TextView
                    addRule(RelativeLayout.CENTER_IN_PARENT)
                }
            }

            // Add TextView to the RelativeLayout
            binding.chartContainer.addView(noDataText)
        }
    }

    private fun updateBudgetRepetitionState(childId: String,budget: Budget ,repetitionState: String) {
        val budgetRef = databaseReference.child(childId).child("budgets").child(budget.id)
        budgetRef.child("repetition").setValue(repetitionState)
    }
    private fun updateBudgetAdditionState(childId: String, budgetKey: String, additionState: Boolean) {
        val budgetRef = databaseReference.child(childId).child("budgets").child(budgetKey)
        budgetRef.child("firstAddition").setValue(additionState)
    }
    private fun updateChildBalance(childId: String, amount: String) {
        val budgetRef = databaseReference.child(childId)
        budgetRef.child("balance").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val currentBalance = dataSnapshot.getValue(String::class.java)
                if (currentBalance != null){
                    val newBalance = currentBalance.toInt() + amount.toInt()
                    budgetRef.child("balance").setValue(newBalance.toString())
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }
    private fun updateBudgetLastUpdate(childId: String, budgetKey: String) {
        val budgetRef = databaseReference.child(childId).child("budgets").child(budgetKey)
        val currentDate = getCurrentDate()
        budgetRef.child("lastUpdate").setValue(currentDate)
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


    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val currentDate = Date()
        return dateFormat.format(currentDate)
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


    private fun setUpCalendarPrevNextClickListener(
        ivCalendarNext: Button,
        ivCalendarPrevious: Button,
        onMonthChanged: (String) -> Unit
    ) {
        ivCalendarNext.setOnClickListener {
            cal.add(Calendar.MONTH, 1)
            if (cal.after(currentDate)){
                cal.time = currentDate.time
            }
            handleCalendarNavigation(onMonthChanged)
            restoreButtonState()
        }

        ivCalendarPrevious.setOnClickListener {
            cal.add(Calendar.MONTH, -1)
            handleCalendarNavigation(onMonthChanged)
            restoreButtonState()
        }
    }

    private fun restoreButtonState() {
        if (isIncomeSelected) {
            animateButton(binding.btnIncome, true)
            animateButton(binding.btnOutcome, false)
        } else {
            animateButton(binding.btnIncome, false)
            animateButton(binding.btnOutcome, true)
        }
    }

    private fun handleCalendarNavigation(onMonthChanged: (String) -> Unit) {
        val selectedMonthYear = sdf.format(cal.time)
        onMonthChanged.invoke(selectedMonthYear)
    }
    private fun whenBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                ActivityCompat.finishAffinity(ChildMenuActivity())
            }
        })
    }
}
