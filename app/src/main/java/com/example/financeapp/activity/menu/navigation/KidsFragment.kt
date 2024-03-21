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
    override fun onDeleteChildClick(position: Int) {
        // Handle the delete action here
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentKidsBinding.inflate(inflater, container, false)
        val view = binding.root

        // Initialize Firebase components
        databaseReference = FirebaseDatabase.getInstance().reference.child("users")
        auth = FirebaseAuth.getInstance()

        // Initialize RecyclerView
        adapter = KidsAdapter(mutableListOf(), this)
        binding.childrenList.layoutManager = LinearLayoutManager(requireContext())
        binding.childrenList.adapter = adapter

        // Fetch children data from Firebase
        fetchChildrenData()

        // Set click listener for "Add Children" button
        binding.btnAddChildren.setOnClickListener {
            showAddChildDialog()
        }

        return view
    }

    private fun showAddChildDialog() {
        val dialogBinding = DialogAddChildBinding.inflate(layoutInflater)
        val dialogView = dialogBinding.root

        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val firstName = dialogBinding.editTextFirstName.text.toString()
                val lastName = dialogBinding.editTextLastName.text.toString()
                val email = auth.currentUser?.email ?: ""

                // Generate a random password
                val password = generateRandomPassword(8)

                // Get the current date
                val currentDateString =
                    SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())

                // Create a Child object
                val newChild = Child(
                    UUID.randomUUID().toString(), // Generate a unique ID for the child
                    firstName,
                    lastName,
                    "", // Date of birth (if needed)
                    email,
                    password,
                    currentDateString,
                    children = true
                )

                // Add the new child to the Firebase Realtime Database
                // Add the new child as a user to the Firebase Realtime Database
                // Add the new child to the "child" node in the Firebase Realtime Database
                databaseReference.child("child").child(newChild.id).setValue(newChild)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Child added successfully to the "child" node
                            fetchChildrenData() // Refresh the UI
                        } else {
                            // Failed to add child to the "child" node
                            Log.e(TAG, "Failed to add child to the 'child' node: ${task.exception}")
                            // You can handle the error here
                        }
                    }


            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialogBuilder.show()
    }

    private fun fetchChildrenData() {
        val query: Query = databaseReference.child("child").orderByChild("children").equalTo(true)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val kidsList: MutableList<User> = mutableListOf()
                for (childSnapshot in snapshot.children) {
                    val user: User? = childSnapshot.getValue(User::class.java)
                    user?.let { kidsList.add(it) }
                }
                adapter.updateData(kidsList)

                // Print fetched data
                println("Fetched ${kidsList.size} users")
                kidsList.forEachIndexed { index, user ->
                    println("User ${index + 1}: $user")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle onCancelled
                println("Fetch cancelled: ${error.message}")
            }
        })
    }
    private fun generateRandomPassword(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

}



