import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.financeapp.R
import com.example.financeapp.adapter.KidsAdapter
import com.example.financeapp.databinding.DialogAddChildBinding
import com.example.financeapp.databinding.FragmentKidsBinding
import com.example.financeapp.databinding.KidsCardViewBinding
import com.example.financeapp.viewmodel.KidsViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KidsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: KidsAdapter
    private lateinit var kidsCardViewBinding: KidsCardViewBinding

    private val viewModel: KidsViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentKidsBinding.inflate(inflater, container, false)
        val view = binding.root



        recyclerView = view.findViewById(R.id.recyclerView)
        adapter = KidsAdapter(mutableListOf())
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.btnAddCard.setOnClickListener {
            showAddChildDialog()
        }


        return view
    }

    private fun showDatePicker(dialogBinding: DialogAddChildBinding) {
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val builder = MaterialDatePicker.Builder.datePicker()
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            // Seçilen tarihi biçimlendir
            val formattedDate = dateFormat.format(Date(selection))

            // Diyalogun içerisindeki editText'e tarih ataması yapılmalı
            dialogBinding.dateOfBirth.setText(formattedDate)
        }
        picker.show(childFragmentManager, picker.toString())
    }


    private fun showAddChildDialog() {
        val dialogBinding = DialogAddChildBinding.inflate(layoutInflater)
        val autoCompleteTextViewSexuality = dialogBinding.autoCompleteTextView

        // Set up adapter for MaterialAutoCompleteTextView
        val sexualityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.sexuality_options)
        )
        autoCompleteTextViewSexuality.setAdapter(sexualityAdapter)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val firstName = dialogBinding.editTextFirstName.text.toString()
                val lastName = dialogBinding.editTextLastName.text.toString()
                val sexuality = autoCompleteTextViewSexuality.text.toString()
                val dateOfBirth = dialogBinding.dateOfBirth.text.toString()

                // Create a new item with retrieved data and add it to the RecyclerView
                val newItem = "$firstName"
                adapter.addItem(newItem)
            }
            .setNegativeButton("Cancel", null)
            .show()

        dialogBinding.dateOfBirth.setOnClickListener {
            showDatePicker(dialogBinding)
        }
    }


}
