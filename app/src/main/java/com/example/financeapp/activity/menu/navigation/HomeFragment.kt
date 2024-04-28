package com.example.financeapp.activity.menu.navigation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.example.financeapp.R
import com.example.financeapp.activity.enterance.IntroActivity
import com.example.financeapp.activity.enterance.UserPreferencesActivity
import com.example.financeapp.model.Budget
import com.example.financeapp.model.User
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
import com.example.financeapp.adapter.HorizontalCalendarAdapter
import com.example.financeapp.model.DateCalendar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(), HorizontalCalendarAdapter.OnItemClickListener {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var databaseReference: DatabaseReference
    private val currentDate = Calendar.getInstance(Locale.ENGLISH)
    private lateinit var auth: FirebaseAuth


    private val sdf = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
    private val cal = Calendar.getInstance(Locale.ENGLISH)
    private val dates = ArrayList<Date>()
    private lateinit var adapter: HorizontalCalendarAdapter
    private val calendarList2 = ArrayList<DateCalendar>()

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
        fetchUserPreferencesFromFirebase()

        // Butonları animasyonla
        animateButton(binding.btnIncome, true)
        animateButton(binding.btnOutcome, false)

        val tvMonth = setUpCalendarAdapter(binding.recyclerView, this@HomeFragment)
        binding.tvDateMonth.text = tvMonth
        fetchBudgetsFromFirebase(tvMonth)

        setUpCalendarPrevNextClickListener(binding.ivCalendarNext, binding.ivCalendarPrevious) { selectedMonthYear ->
            // Callback when month changes
            // Update UI with the selected month and year
            binding.tvDateMonth.text = selectedMonthYear
            println(selectedMonthYear)
            // Fetch budgets again based on the selected month and year
            fetchBudgetsFromFirebase(selectedMonthYear)
        }
        return view
    }

    override fun onItemClick(ddMmYy: String, dd: String, day: String) {
        val selectedDate = "$day $dd, $ddMmYy"
        Toast.makeText(requireContext(), "Selected Date: $selectedDate", Toast.LENGTH_SHORT).show()
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
                            if (currency.isNotEmpty() && balance.isNotEmpty())
                                updateUI(balance, currency)
                            else
                                showPreferenceInputDialog()
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
        binding.currency.text = currencySymbol
    }
    private fun showPreferenceInputDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setMessage("Devam etmek için başlangıç terchilerinizi girmelisiniz.")
            .setPositiveButton("Tamam") { _, _ ->
                // "Tamam" butonuna basıldığında UserPreferencesActivity'e yönlendirilir
                val intent = Intent(requireContext(), UserPreferencesActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("İptal") { _, _ ->
                // "İptal" butonuna basıldığında IntroActivity'e yönlendirilir
                val intent = Intent(requireContext(), IntroActivity::class.java)
                startActivity(intent)
            }
            .setCancelable(false) // Kullanıcı arka plana tıklamayı iptal edemez
            .create()

        dialog.show()
    }
    //Budgetleri firebase çeken fonksiyon
    private fun fetchBudgetsFromFirebase(selectedMonthYear: String) {
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
                                // Get the creation date
                                val creationDate = it.creationDate // String olarak alınan tarih
                                val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault()) // String'i Date'e dönüştürmek için format belirle
                                val date = dateFormat.parse(creationDate) ?: Date() // String tarihi Date'e dönüştür
                                val calendar = Calendar.getInstance()
                                calendar.time = date
                                val dateFormat2 = SimpleDateFormat("MMMM", Locale.getDefault())
                                val monthName = dateFormat2.format(calendar.time) // Ay ismini al
                                val year = calendar.get(Calendar.YEAR)
                                val budgetDate = "$monthName $year"

                                if (budgetDate == selectedMonthYear) {
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
                        }

                        // Eğer hiç veri yoksa, grafikleri temizle
                        if (incomeLabels.isEmpty() && outcomeLabels.isEmpty()) {
                            clearChart()
                            showNoDataText()
                        } else {
                            // Veriler mevcutsa, pasta grafiği oluştur ve göster
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
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Hata durumunu ele al
                    }
                })
        }
    }
    private fun showNoDataText() {
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
    private fun clearChart() {
        binding.chartContainer.removeAllViews()
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
        }

        ivCalendarPrevious.setOnClickListener {
            cal.add(Calendar.MONTH, -1)
            handleCalendarNavigation(onMonthChanged)
        }
    }
    private fun handleCalendarNavigation(onMonthChanged: (String) -> Unit) {
        val selectedMonthYear = sdf.format(cal.time)
        onMonthChanged.invoke(selectedMonthYear)
    }

    /*
     * Setting up adapter for recyclerview
     */
    private fun setUpCalendarAdapter(recyclerView: RecyclerView, listener : HorizontalCalendarAdapter.OnItemClickListener) : String {
        val snapHelper: SnapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        adapter = HorizontalCalendarAdapter { calendarDateModel: DateCalendar, position: Int ->
            calendarList2.forEachIndexed { index, calendarModel ->
                calendarModel.isSelected = index == position
            }
        }
        adapter.setData(calendarList2)
        adapter.setOnItemClickListener(listener)
        recyclerView.adapter = adapter

        return setUpCalendar(listener)
    }

    /*
     * Function to setup calendar for every month
     */
    private fun setUpCalendar(listener: HorizontalCalendarAdapter.OnItemClickListener) : String {
        val calendarList = ArrayList<DateCalendar>()
        val monthCalendar = cal.clone() as Calendar
        val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        dates.clear()
        monthCalendar.set(Calendar.DAY_OF_MONTH, 1)
        while (dates.size < maxDaysInMonth) {
            dates.add(monthCalendar.time)
            calendarList.add(DateCalendar(monthCalendar.time))
            monthCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        calendarList2.clear()
        calendarList2.addAll(calendarList)
        adapter.setOnItemClickListener(listener)
        adapter.setData(calendarList)
        return sdf.format(cal.time)
    }


}
