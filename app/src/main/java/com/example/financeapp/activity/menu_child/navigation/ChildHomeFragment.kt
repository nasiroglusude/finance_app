package com.example.financeapp.activity.menu_child.navigation

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
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.example.financeapp.R
import com.example.financeapp.adapter.HorizontalCalendarAdapter
import com.example.financeapp.model.Budget
import com.example.financeapp.model.DateCalendar
import com.example.financeapp.databinding.ActivityControlledChildBinding
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChildHomeFragment : Fragment(), HorizontalCalendarAdapter.OnItemClickListener {

    private lateinit var binding: ActivityControlledChildBinding
    private lateinit var auth: FirebaseAuth
    private var childId: String? = null
    private lateinit var databaseReference: DatabaseReference

    private val sdf = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
    private val cal = Calendar.getInstance(Locale.ENGLISH)
    private val dates = ArrayList<Date>()
    private lateinit var adapter: HorizontalCalendarAdapter
    private val calendarList2 = ArrayList<DateCalendar>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // ViewBinding'i başlat
        binding = ActivityControlledChildBinding.inflate(layoutInflater)
        val view = binding.root

        databaseReference = FirebaseDatabase.getInstance().reference.child("child")
        auth = Firebase.auth

        childId = arguments?.getString("childId")
        //Çocuğun parentMail fieldi ile eşelşen maile sahip userın verilerini çekmek
        childId?.let { id ->
            fetchChildAttributeById(id, "balance") { balance ->
                balance?.let { availableBalance ->
                    binding.availableBalance.text = availableBalance
                }

            }
        }

        animateButton(binding.btnIncome, true)
        animateButton(binding.btnOutcome, false)

        val tvMonth = setUpCalendarAdapter(binding.recyclerView, this@ChildHomeFragment)
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

    //Budgetleri firebase çeken fonksiyon
    private fun fetchBudgetsFromFirebase(selectedMonthYear: String) {
        // Kullanıcı kimliğini al
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
                                println(budgetDate)
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
