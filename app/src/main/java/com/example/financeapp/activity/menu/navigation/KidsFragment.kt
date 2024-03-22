import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.financeapp.data.Child
import com.example.financeapp.data.User
import com.example.financeapp.databinding.DialogAddChildBinding
import com.example.financeapp.databinding.FragmentKidsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class KidsFragment : Fragment(), KidsAdapter.OnDeleteChildClickListener {

    private lateinit var binding: FragmentKidsBinding
    private lateinit var adapter: KidsAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var kidsList: MutableList<User> = mutableListOf()

    // ChildAdapter sınıfındaki arayüz metodunu uygulama
    override fun onDeleteChildClick(position: Int) {
        // Silme işlemini burada ele al
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentKidsBinding.inflate(inflater, container, false)
        val view = binding.root

        // Firebase bileşenlerini başlat
        databaseReference = FirebaseDatabase.getInstance().reference.child("users")
        auth = FirebaseAuth.getInstance()

        // RecyclerView'ı başlat
        adapter = KidsAdapter(kidsList, this)
        binding.childrenList.layoutManager = LinearLayoutManager(requireContext())
        binding.childrenList.adapter = adapter

        // Firebase'den çocuk verilerini al
        if (kidsList.isEmpty()) {
            println("Databaseden çekti")
            fetchChildrenData()
        }

        // "Çocuk Ekle" düğmesi için tıklama dinleyicisini ayarla
        binding.btnAddChildren.setOnClickListener {
            showAddChildDialog()
        }

        return view
    }

    // Çocuk ekleme iletişim kutusunu gösteren fonksiyon
    private fun showAddChildDialog() {
        val dialogBinding = DialogAddChildBinding.inflate(layoutInflater)
        val dialogView = dialogBinding.root

        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val firstName = dialogBinding.editTextFirstName.text.toString()
                val lastName = dialogBinding.editTextLastName.text.toString()
                val email = auth.currentUser?.email ?: ""

                // Rastgele bir şifre oluştur
                val password = generateRandomPassword(8)

                // Geçerli tarihi al
                val currentDateString =
                    SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())

                // Bir Çocuk nesnesi oluştur
                val newChild = Child(
                    UUID.randomUUID().toString(), // Çocuk için benzersiz bir kimlik oluştur
                    firstName,
                    lastName,
                    "", // Doğum tarihi (gerekiyorsa)
                    email,
                    password,
                    currentDateString,
                    children = true
                )

                // Yeni çocuğu Firebase Gerçek Zamanlı Veritabanına ekle
                databaseReference.child("child").child(newChild.id).setValue(newChild)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Çocuk başarıyla "child" düğümüne eklendi
                            fetchChildrenData() // Arayüzü yenile
                        } else {
                            // Çocuğun "child" düğümüne eklenmesi başarısız oldu
                            Log.e(TAG, "Failed to add child to the 'child' node: ${task.exception}")
                            // Hatası burada ele alabilirsiniz
                        }
                    }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialogBuilder.show()
    }

    // Çocuk verilerini Firebase'den almak için fonksiyon
    private fun fetchChildrenData() {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

        currentUserEmail?.let { email ->
            val query: Query = databaseReference.child("child").orderByChild("parentMail").equalTo(email)
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val kidsList: MutableList<User> = mutableListOf()
                    for (childSnapshot in snapshot.children) {
                        val child: User? = childSnapshot.getValue(User::class.java)
                        child?.let { kidsList.add(it) }
                    }
                    adapter.updateData(kidsList)

                    // Alınan verileri yazdır
                    println("Alınan ${kidsList.size} kullanıcılar")
                    kidsList.forEachIndexed { index, user ->
                        println("Kullanıcı ${index + 1}: $user")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // onCancelled'ı ele al
                    println("Alma işlemi iptal edildi: ${error.message}")
                }
            })
        } ?: run {
            // Eğer mevcut kullanıcı yoksa bir hata mesajı yazdır
            println("Mevcut kullanıcı bulunamadı.")
        }
    }


    // Rastgele bir şifre oluşturmak için fonksiyon
    private fun generateRandomPassword(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

}



