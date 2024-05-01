package com.example.financeapp.activity.menu.navigation

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.financeapp.R
import com.example.financeapp.activity.menu.MenuActivity
import com.example.financeapp.databinding.ExchangeFromSpinnerBinding
import com.example.financeapp.databinding.ExchangeToSpinnerBinding
import com.example.financeapp.databinding.FragmentExchangeBinding
import com.example.financeapp.enums.Currency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.coroutines.CoroutineContext

class ExchangeFragment : Fragment(), CoroutineScope {

    private lateinit var binding: FragmentExchangeBinding
    private lateinit var bindingToDialog: ExchangeToSpinnerBinding
    private lateinit var bindingFromDialog: ExchangeFromSpinnerBinding
    private var convert_from_value: Currency? = null
    private var convert_to_value: Currency? = null
    private var conversion_value: String = ""

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExchangeBinding.inflate(inflater, container, false)
        val view = binding.root

        val arraylist: ArrayList<String> = ArrayList()
        Currency.entries.forEach { currency ->
            arraylist.add(currency.code)
        }
        setClickListeners()
        whenBackPressed()



        return view
    }

    private fun setClickListeners() {
        binding.convertFromDropdownMenu.setOnClickListener {
            bindingFromDialog =
                ExchangeFromSpinnerBinding.inflate(LayoutInflater.from(requireContext()))
            val fromDialog = Dialog(requireContext())
            fromDialog.setContentView(bindingFromDialog.root)
            fromDialog.window?.setLayout(1300, 1600)
            fromDialog.show()

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                Currency.entries
            )
            bindingFromDialog.listView.adapter = adapter

            bindingFromDialog.editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    adapter.filter.filter(s)
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            bindingFromDialog.listView.onItemClickListener =
                AdapterView.OnItemClickListener { parent, _, position, _ ->
                    val selectedCurrency = parent.getItemAtPosition(position) as Currency
                    binding.convertFromDropdownMenu.text = selectedCurrency.code
                    fromDialog.dismiss()
                    convert_from_value = selectedCurrency
                }
        }

        binding.convertToDropdownMenu.setOnClickListener {
            bindingToDialog =
                ExchangeToSpinnerBinding.inflate(LayoutInflater.from(requireContext()))
            val toDialog = Dialog(requireContext())
            toDialog.setContentView(bindingToDialog.root)
            toDialog.window?.setLayout(1300, 1600)
            toDialog.show()


            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                Currency.entries
            )
            bindingToDialog.listView.adapter = adapter

            bindingToDialog.editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    adapter.filter.filter(s)
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            bindingToDialog.listView.onItemClickListener =
                AdapterView.OnItemClickListener { parent, _, position, _ ->
                    val selectedCurrency = parent.getItemAtPosition(position) as Currency
                    binding.convertToDropdownMenu.text = selectedCurrency.code
                    toDialog.dismiss()
                    convert_to_value = selectedCurrency
                }
        }

        binding.btnConvert.setOnClickListener {
            try {
                val amountToConvert = binding.editAmountToConvertValue.text.toString().toDouble()
                convert_from_value?.let { from ->
                    convert_to_value?.let { to ->
                        launch {
                            getConversionRate(from, to, amountToConvert)
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle exception
            }
        }
    }

    private fun getConversionRate(
        convertFrom: Currency,
        convertTo: Currency,
        amountToConvert: Double
    ) {
        val requestQueue: RequestQueue = Volley.newRequestQueue(requireContext())
        val url =
            "https://free.currconv.com/api/v7/convert?q=${convertFrom.code}_${convertTo.code}&compact=ultra&apiKey=45854d1f50c6be34a749"
        println(url)
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    val conversionRateValue =
                        round(jsonObject.getDouble("${convertFrom.code}_${convertTo.code}"), 2)
                    conversion_value = "${round((conversionRateValue * amountToConvert), 2)}"
                    println(conversion_value)
                    binding.conversionRate.text = conversion_value
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            {
            }
        )

        requestQueue.add(stringRequest)
    }

    private fun round(value: Double, currency: Int): Double {
        if (currency < 0) throw IllegalArgumentException()
        val bd = BigDecimal.valueOf(value)
        return bd.setScale(currency, RoundingMode.HALF_UP).toDouble()
    }

    private fun switchToHomeFragment() {
        if (isAdded) {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, HomeFragment())
                .commit()
            (requireActivity() as MenuActivity).updateSelectedNavItem(R.id.home)
        }
    }

    private fun whenBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(
            requireActivity(),
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    switchToHomeFragment()
                }
            })
    }
}

