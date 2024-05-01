package com.example.financeapp.activity.enterance

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.financeapp.activity.menu_child.ChildMenuActivity
import com.example.financeapp.activity.menu_child.navigation.ChildHomeFragment
import com.example.financeapp.databinding.ActivityChildLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class ChildLoginActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityChildLoginBinding
    private lateinit var databaseReference: DatabaseReference

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        databaseReference = FirebaseDatabase.getInstance().reference
        supportActionBar?.hide()

        // Firebase authentication instance'ını başlat
        auth = Firebase.auth

        // Giriş butonunun alfasını ayarla
        binding.btnLogin.alpha = 0.5f

        // Metin değişikliği dinleyicilerini ayarla
        setupTextChangeListeners()

        // Tıklanabilir metni ayarla
        setupClickListener()

    }

    // Metin değişikliği dinleyicilerini ayarlayan fonksiyon
    private fun setupTextChangeListeners() {
        binding.email.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateLoginButtonState()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Gerekli değil
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Gerekli değil
            }
        })

        binding.password.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateLoginButtonState()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Gerekli değil
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Gerekli değil
            }
        })
    }

    // Giriş butonunun durumunu güncelleyen fonksiyon
    private fun updateLoginButtonState() {
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()

        val isValidInput = email.isNotEmpty() && password.isNotEmpty()
        binding.btnLogin.isEnabled = isValidInput
        binding.btnLogin.alpha = if (isValidInput) 1.0f else 0.5f

    }

    // Tıklama olaylarını ayarlayan fonksiyon
    private fun setupClickListener() {
        // Geri düğmesine tıklanınca MainActivity'e yönlendir
        binding.backButton.setOnClickListener {
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
        }

        // Giriş butonuna tıklanınca
        binding.btnLogin.setOnClickListener {
            launch {
                loginWithParentEmailAndPassword()
            }
        }
    }

    //Child'ın parent emaili ve şifresi ile girişi
    private fun loginWithParentEmailAndPassword() {
        val parentEmail = binding.email.text.toString()
        val parentPassword = binding.password.text.toString()

        if (parentEmail.isEmpty() || parentPassword.isEmpty()) {
            // Eğer e-posta veya şifre boşsa, bir mesaj göster ve çık
            Toast.makeText(
                applicationContext,
                "Lütfen hem e-posta hem de şifreyi girin.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Veritabanından çocuk kullanıcıları al
        val childUsersRef = databaseReference.child("child")
        childUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Her bir çocuk kullanıcısını döngüye al
                for (childSnapshot in snapshot.children) {
                    val email = childSnapshot.child("parentMail").getValue(String::class.java)
                    val password = childSnapshot.child("password").getValue(String::class.java)
                    // E-posta ve şifrenin eşleşip eşleşmediğini kontrol et
                    if (email == parentEmail && password == parentPassword) {
                        val userId = childSnapshot.key // Kullanıcı ID'sini al
                        navigateToControlledChildActivity(userId)
                        println("Child ID: "+userId)
                        return
                    }

                }
                // Eşleşen kullanıcı bulunamazsa, bir hata mesajı göster
                Toast.makeText(
                    applicationContext,
                    "Kimlik doğrulama başarısız oldu. Geçersiz e-posta veya şifre.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onCancelled(error: DatabaseError) {
                // onCancelled durumunu işle
                Toast.makeText(
                    applicationContext,
                    "Çocuk kullanıcıları alınamadı.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    //Giriş başarılı olduğunda çocuğa özel olan sayfaya (ControlledChildActivity) yönlendir
    private fun navigateToControlledChildActivity(userId: String?) {
        val intent = Intent(this, ChildMenuActivity::class.java)
        intent.putExtra("childId", userId) // Kullanıcı ID'sini intent'e ekle
        startActivity(intent)
    }

}
