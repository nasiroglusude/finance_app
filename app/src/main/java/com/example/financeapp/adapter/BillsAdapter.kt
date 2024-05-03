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
import com.example.financeapp.model.Bill
import com.example.financeapp.model.Debt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class BillsAdapter(
    private val list: MutableList<Bill>,
    private val listener: OnDeleteBillClickListener
) : RecyclerView.Adapter<BillsAdapter.ViewHolder>() {

    interface OnDeleteBillClickListener {
        fun onDeleteBillClick(position: Int)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // CardView içindeki bileşenleri tanımla
        val textViewName: TextView = itemView.findViewById(R.id.tvBillName)
        val textViewAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val textViewCurrency: TextView = itemView.findViewById(R.id.tvCurrency)
        val textViewLastDate: TextView = itemView.findViewById(R.id.tvLastDate)
        val btnDelete: AppCompatButton = itemView.findViewById(R.id.btnDeleteBill)
        val cbPay: CheckBox = itemView.findViewById(R.id.cbPay)
    }

    // ViewHolder oluşturulduğunda çağrılan metot
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):  ViewHolder {
        // ViewHolder'ın düzenini şişir
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.bill_item, parent, false)

        return ViewHolder(view)
    }

    // ViewHolder'a veri bağlandığında çağrılan metot
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bill = list[position]
        holder.textViewName.text = bill.ownerName
        holder.textViewAmount.text = bill.amount
        holder.textViewCurrency.text = bill.currency
        holder.textViewLastDate.text = bill.lastDate

        // Set checkbox state based on debt status
        holder.cbPay.isChecked = bill.status == "paid"

        // Set click listener for delete button
        holder.btnDelete.setOnClickListener {
            listener.onDeleteBillClick(position)
        }

        // Set checked change listener for checkbox
        holder.cbPay.setOnCheckedChangeListener { buttonView, isChecked ->
            val newStatus = if (isChecked) "paid" else "unpaid"
            updateBillStatus(holder.adapterPosition, newStatus)
        }
    }
    fun updateBillStatus(position: Int, newStatus: String) {
        if (position in 0 until list.size) {
            val bill = list[position]
            bill.status = newStatus

            // Get a reference to the debt in the Firebase Realtime Database
            val databaseReference = FirebaseDatabase.getInstance().reference
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.let { user ->
                val userId = user.uid
                val debtRef = databaseReference.child("users").child(userId).child("bills").child(bill.id)

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
    fun updateData(newList: List<Bill>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

}
