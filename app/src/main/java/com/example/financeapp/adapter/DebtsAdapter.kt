import android.content.ContentValues.TAG
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.example.financeapp.R
import com.example.financeapp.model.Debt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DebtsAdapter(
    private val list: MutableList<Debt>,
    private val listener: OnDeleteDebtClickListener
) : RecyclerView.Adapter<DebtsAdapter.ViewHolder>() {

    interface OnDeleteDebtClickListener {
        fun onDeleteDebtClick(position: Int)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // CardView içindeki bileşenleri tanımla
        val textViewName: TextView = itemView.findViewById(R.id.tvName)
        val textViewAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val textViewCurrency: TextView = itemView.findViewById(R.id.tvCurrency)
        val textViewLastDate: TextView = itemView.findViewById(R.id.tvLastDate)
        val btnDelete: AppCompatButton = itemView.findViewById(R.id.btnDeleteChild)
        val cbPay: CheckBox = itemView.findViewById(R.id.cbPay)
    }

    // ViewHolder oluşturulduğunda çağrılan metot
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):  ViewHolder {
        // ViewHolder'ın düzenini şişir
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.debt_item, parent, false)

        return ViewHolder(view)
    }

    // ViewHolder'a veri bağlandığında çağrılan metot
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val debt = list[position]
        holder.textViewName.text = debt.creditorName
        holder.textViewAmount.text = debt.amount
        holder.textViewCurrency.text = debt.currency
        holder.textViewLastDate.text = debt.lastDate

        // Set checkbox state based on debt status
        holder.cbPay.isChecked = debt.status == "paid"

        // Set click listener for delete button
        holder.btnDelete.setOnClickListener {
            listener.onDeleteDebtClick(position)
        }

        // Set checked change listener for checkbox
        holder.cbPay.setOnCheckedChangeListener { buttonView, isChecked ->
            val newStatus = if (isChecked) "paid" else "unpaid"
            updateDebtStatus(holder.adapterPosition, newStatus)
        }
    }
    fun updateDebtStatus(position: Int, newStatus: String) {
        if (position in 0 until list.size) {
            val debt = list[position]
            debt.status = newStatus

            // Get a reference to the debt in the Firebase Realtime Database
            val databaseReference = FirebaseDatabase.getInstance().reference
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.let { user ->
                val userId = user.uid
                val debtRef = databaseReference.child("users").child(userId).child("debts").child(debt.id)

                // Update the status field of the debt in the database
                debtRef.child("status").setValue(newStatus)
                    .addOnSuccessListener {
                        // If the update is successful, notify the adapter of the change
                        notifyItemChanged(position)
                    }
                    .addOnFailureListener { exception ->
                        // Handle any errors that occur during the update process
                        Log.e(TAG, "Failed to update debt status: $exception")
                        // Optionally, you can also revert the local change if the database update fails
                        // For example, you could reload the data from the database to ensure consistency
                    }
            }
        }
    }


    // Listede bulunan eleman sayısını döndüren metot
    override fun getItemCount(): Int {
        return list.size
    }

    // Yeni veri seti ile listeyi güncelleyen metot
    fun updateData(newList: List<Debt>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

}
