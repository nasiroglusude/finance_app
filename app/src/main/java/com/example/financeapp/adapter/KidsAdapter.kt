import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.financeapp.R
import com.example.financeapp.activity.menu.navigation.child.ChildControlActivity
import com.example.financeapp.model.User
import com.google.android.material.button.MaterialButton

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
        val btnSeeDetails: MaterialButton = itemView.findViewById(R.id.btnDetails)
    }

    // ViewHolder oluşturulduğunda çağrılan metot
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):  ViewHolder {
        // ViewHolder'ın düzenini şişir
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.children_item, parent, false)

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

        holder.btnSeeDetails.setOnClickListener{
            val childId = user.id
            navigateToActivityWithExtra(holder.itemView.context, ChildControlActivity::class.java, "childId", childId)
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
    fun navigateToActivityWithExtra(context: Context, targetActivity: Class<*>, extraKey: String, extraValue: String) {
        val intent = Intent(context, targetActivity)
        intent.putExtra(extraKey, extraValue)
        context.startActivity(intent)
    }

}
