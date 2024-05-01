package com.example.financeapp.activity.enterance

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.financeapp.R
import com.example.financeapp.activity.menu.MenuActivity
import com.example.financeapp.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class LoginActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        // Firebase authentication instance'ını başlat
        auth = Firebase.auth

        println( auth.currentUser?.uid)
        // Giriş butonunun alfasını ayarla
        binding.btnLogin.alpha = 0.5f

        // Metin değişikliği dinleyicilerini ayarla
        setupTextChangeListeners()

        // Tıklanabilir metni ayarla
        setupClickListener()

        // Kayıt olma metnini ayarla
        setupSignUpText()
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
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                // Eğer e-posta veya şifre boşsa, bir mesaj göster ve çık
                Toast.makeText(
                    baseContext,
                    "Lütfen hem e-posta hem de şifreyi girin.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            launch{
                userSignIn(email, password)
            }
        }
    }

    private fun userSignIn(email:String, password:String){
        // E-posta ve şifreyle oturum açmayı dene
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Oturum açma başarılı, kullanıcının bilgileriyle arayüzü güncelle
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // Oturum açma başarısız olursa, kullanıcıya bir mesaj göster
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Kimlik doğrulama başarısız oldu.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    updateUI(null)
                }
            }
    }

    // Kullanıcı arayüzünü güncelleyen fonksiyon
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Kullanıcı oturum açtı, ana etkinliğe yönlendirme veya başka bir işlem gerçekleştirme
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            finish() // İsteğe bağlı: Kullanıcının geri dönmesini önlemek için geçerli etkinliği sonlandır
        } else {
            // Kullanıcı oturum açma başarısız veya kullanıcı null, arayüzü buna göre işle
            // Örneğin, bir hata mesajı göster
            Toast.makeText(this, "Kullanıcı oturum açma başarısız oldu", Toast.LENGTH_SHORT).show()
        }
    }

    // Kayıt olma metnini ayarlayan fonksiyon
    private fun setupSignUpText() {
        val signUpText = findViewById<TextView>(R.id.sign_up_text)

        val spannableString = createClickableSpan()

        signUpText.apply {
            text = spannableString
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = resources.getColor(android.R.color.transparent)
        }
    }

    // Tıklanabilir metni oluşturan fonksiyon
    private fun createClickableSpan(): SpannableString {
        val spannableString = SpannableString(getString(R.string.new_member) + " " + getString(R.string.click_here_to_sign_up))

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                navigateToRegisterActivity()
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.color = resources.getColor(R.color.purple_500)
            }
        }

        // Tıklanabilir metni ayarla
        spannableString.setSpan(clickableSpan, getString(R.string.new_member).length + 1, spannableString.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannableString
    }

    // Kayıt ekranına yönlendiren fonksiyon
    private fun navigateToRegisterActivity() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}
