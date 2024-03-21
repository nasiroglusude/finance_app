package com.example.financeapp.activity.menu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.financeapp.R
import com.example.financeapp.databinding.ActivityControlledChildBinding
import com.google.firebase.auth.FirebaseAuth

class ControlledChildActivity : AppCompatActivity() {

    private lateinit var binding: ActivityControlledChildBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controlled_child)

        // ViewBinding'i başlat
        binding = ActivityControlledChildBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // FirebaseAuth örneğini başlat
        auth = FirebaseAuth.getInstance()

        // FirebaseAuth'ten geçerli kullanıcıyı al
        val currentUser = auth.currentUser

        // Geçerli kullanıcının null olmadığını kontrol et
        currentUser?.let { user ->
            // Kullanıcının tam adını al
            val fullName = "${user.displayName}"
            // Tam adı TextView'e ayarla
            binding.textViewFullName.text = fullName
        }
    }
}
