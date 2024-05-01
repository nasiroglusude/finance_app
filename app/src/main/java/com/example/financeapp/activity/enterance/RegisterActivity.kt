package com.example.financeapp.activity.enterance

import android.content.ContentValues.TAG
import android.os.Bundle
import android.view.View
import android.content.Intent
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.*
import androidx.appcompat.app.AppCompatActivity
import com.example.financeapp.R
import com.example.financeapp.model.User
import com.example.financeapp.databinding.ActivityRegisterBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import kotlin.coroutines.CoroutineContext

class RegisterActivity : AppCompatActivity(),CoroutineScope {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignup.alpha = 0.5f
        editTextListener()
        setupClickListener()
        setupClickableSpan()
        setupTextChangeListeners()
    }

    // E-posta ve Doğum Tarihi alanlarını dinleyen fonksiyonlar
    private fun editTextListener() {
        binding.email.setOnFocusChangeListener{_, focused->
            if(!focused){
                binding.emailContainer.helperText = validEmail()
            }
        }

        binding.password.setOnFocusChangeListener{_, focused->
            if(!focused){
                binding.passwordContainer.helperText = validPassword()
            }
        }
    }

    // E-posta alanı için geçerlilik kontrolü yapan fonksiyon
    private fun validEmail(): String? {
        val emailText = binding.email.text.toString()
        if(!Patterns.EMAIL_ADDRESS.matcher(emailText).matches())
        {
            return getString(R.string.invalid_mail)
        }
        return null
    }

    private fun validPassword(): String?{
        val password = binding.password.text.toString()
        val isPasswordValid = password.length >= 6// && password.any { it.isUpperCase() }
        if (!isPasswordValid) {
            return getString(R.string.invalid_password)
        }
        return null
    }

    // Buton tıklama olaylarını ayarlayan fonksiyon
    private fun setupClickListener() {
        binding.backButton.setOnClickListener {
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
        }

        binding.dateOfBirth.setOnClickListener {
            showDatePicker()
        }

        binding.btnSignup.setOnClickListener {
            launch {
                signupWithEmailAndPassword()
            }
        }
    }

    private fun signupWithEmailAndPassword() {
        val firstName = binding.firstName.text.toString()
        val lastName = binding.lastName.text.toString()
        val phoneNumber = binding.phoneNumber.text.toString()
        val dateOfBirth = binding.dateOfBirth.text.toString()
        val email = binding.email.text.toString()
        val password = binding.password.text.toString()
        val currentDateString = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())

        auth = Firebase.auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userProfileChangeRequest = UserProfileChangeRequest.Builder()
                        .setDisplayName("$firstName $lastName")
                        .build()
                    user?.updateProfile(userProfileChangeRequest)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                Log.d(TAG, "Kullanıcı profili güncellendi.")
                            } else {
                                Log.e(TAG, "Kullanıcı profilini güncelleme başarısız.", profileTask.exception)
                            }
                        }

                    val newUser = User(
                        user?.uid ?: "",
                        firstName,
                        lastName,
                        phoneNumber,
                        dateOfBirth,
                        email,
                        password,
                        balance = "",
                        currency = "",
                        currentDateString
                    )

                    val databaseReference = FirebaseDatabase.getInstance().reference.child("users").child(user?.uid ?: "")
                    databaseReference.setValue(newUser)
                        .addOnCompleteListener { databaseTask ->
                            if (databaseTask.isSuccessful) {
                                Log.d(TAG, "Kullanıcı bilgileri veritabanına kaydedildi.")
                            } else {
                                Log.e(TAG, "Kullanıcı bilgilerini veritabanına kaydetme başarısız.", databaseTask.exception)
                            }
                        }

                    updateUI(user)
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Kimlik doğrulama başarısız oldu.",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(null)
                }
            }
    }


    // Kullanıcı arayüzünü güncelleyen fonksiyon
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Kullanıcı oturum açtı, ana etkinliğe yönlendirme veya başka bir işlem gerçekleştirme
            val intent = Intent(this, UserPreferencesActivity::class.java)
            startActivity(intent)
            finish() // İsteğe bağlı: Kullanıcının geri dönmesini önlemek için geçerli etkinliği sonlandır
        } else {
            // Kullanıcı kaydı başarısız veya kullanıcı null, arayüzü buna göre işle
            // Örneğin, bir hata mesajı göster
            Toast.makeText(this, "Kullanıcı kaydı başarısız oldu", Toast.LENGTH_SHORT).show()
        }
    }

    // Tıklanabilir metni ayarlayan fonksiyon
    private fun setupClickableSpan() {
        val spannableString =
            SpannableString(getString(R.string.do_you_have_an_account) + " " + getString(R.string.Login))
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                navigateToLoginActivity()
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.color = resources.getColor(R.color.purple_500)
            }
        }
        spannableString.setSpan(
            clickableSpan,
            getString(R.string.Login).length + 1,
            spannableString.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.logInText.text = spannableString
        binding.logInText.movementMethod = LinkMovementMethod.getInstance()
        binding.logInText.highlightColor = resources.getColor(android.R.color.transparent)
    }

    // Tarih seçiciyi gösteren fonksiyon
    private fun showDatePicker() {
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val builder = MaterialDatePicker.Builder.datePicker()
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            // Seçilen tarihi biçimlendir
            val formattedDate = dateFormat.format(Date(selection))
            // Biçimlendirilmiş tarihi EditText'e ayarla
            binding.dateOfBirth.setText(formattedDate)
        }
        picker.show(supportFragmentManager, picker.toString())
    }

    // Giriş ekranına yönlendiren fonksiyon
    private fun navigateToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    // Metin değişikliği dinleyicilerini ayarlayan fonksiyon
    private fun setupTextChangeListeners() {
        binding.email.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateRegisterButtonState()
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
                updateRegisterButtonState()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Gerekli değil
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Gerekli değil
            }
        })


    }

    // Kayıt butonunun durumunu güncelleyen fonksiyon
    private fun updateRegisterButtonState() {
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()

        // Parola uzunluğu kontrolü
        val isPasswordValid = password.length >= 6// && password.any { it.isUpperCase() }

        val isValidInput = email.isNotEmpty() && password.isNotEmpty() && isPasswordValid
        binding.btnSignup.isEnabled = isValidInput
        binding.btnSignup.alpha = if (isValidInput) 1.0f else 0.5f
    }

}
