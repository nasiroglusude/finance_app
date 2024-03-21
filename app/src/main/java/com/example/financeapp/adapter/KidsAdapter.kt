import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.financeapp.R
import com.example.financeapp.data.User

class KidsAdapter(
    private val list: MutableList<User>,
    private val listener: OnDeleteChildClickListener
) : RecyclerView.Adapter<KidsAdapter.ViewHolder>() {

    interface OnDeleteChildClickListener {
        fun onDeleteChildClick(position: Int)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // CardView içindeki bileşenleri tanımla
        val textViewFullName: TextView = itemView.findViewById(R.id.tvFirstName)
        val textViewPassword: TextView = itemView.findViewById(R.id.tvPassword)
        val btnDelete: TextView = itemView.findViewById(R.id.btnDeleteChild)
    }

    // ViewHolder oluşturulduğunda çağrılan metot
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):  ViewHolder {
        // ViewHolder'ın düzenini şişir
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.kids_card_view, parent, false)

        return ViewHolder(view)
    }

    // ViewHolder'a veri bağlandığında çağrılan metot
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = list[position]
        holder.textViewFullName.text = "${user.firstName}"
        holder.textViewPassword.text = user.password // Şifreyi göster
        holder.textViewFullName.typeface = holder.textViewFullName.resources.getFont(R.font.kanit)

        // Silme düğmesine tıklandığında listener çağrılır
        holder.btnDelete.setOnClickListener {
            listener.onDeleteChildClick(position)
        }
    }

    // Listede bulunan eleman sayısını döndüren metot
    override fun getItemCount(): Int {
        return list.size
    }

    // Yeni veri seti ile listeyi güncelleyen metot
    fun updateData(newList: List<User>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
