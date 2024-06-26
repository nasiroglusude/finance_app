package com.example.financeapp.activity.enterance

import android.content.ContentValues.TAG
import android.os.Bundle
import android.view.View
import android.content.Intent
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.*
import androidx.appcompat.app.AppCompatActivity
import com.example.financeapp.R
import com.example.financeapp.activity.menu.MenuActivity
import com.example.financeapp.databinding.ActivityRegisterBinding
import com.example.financeapp.data.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        EditTextListener()
        setupClickListener()
        setupClickableSpan()
    }

    // E-posta ve Doğum Tarihi alanlarını dinleyen fonksiyonlar
    private fun EditTextListener() {
        binding.email.setOnFocusChangeListener{_, focused->
            if(!focused){
                binding.emailContainer.helperText = validEmail()
            }
        }

        binding.dateContainer.setOnFocusChangeListener{_, focused->
            if(!focused){
                binding.dateContainer.helperText = validDate()
            }
        }
    }

    // Doğum Tarihi alanı için geçerlilik kontrolü yapan fonksiyon
    private fun validDate(): String? {
        val dateText = binding.dateOfBirth.text.toString()
        val dateRegex = Regex("""^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/([0-9]{2})$""")
        if (!dateRegex.matches(dateText)) {
            return "Geçersiz Tarih"
        }
        return null
    }


    // E-posta alanı için geçerlilik kontrolü yapan fonksiyon
    private fun validEmail(): String? {
        val emailText = binding.email.text.toString()
        if(!Patterns.EMAIL_ADDRESS.matcher(emailText).matches())
        {
            return "Geçersiz E-posta Adresi"
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

        binding.btnSignup.setOnClickListener{
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
                        // Kullanıcı başarıyla oluşturuldu, kullanıcı profilini güncelle
                        val user = auth.currentUser
                        val userProfileChangeRequest = UserProfileChangeRequest.Builder()
                            .setDisplayName("$firstName $lastName")
                            .build()
                        user?.updateProfile(userProfileChangeRequest)
                            ?.addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    // Profil başarıyla güncellendi
                                    Log.d(TAG, "Kullanıcı profili güncellendi.")
                                } else {
                                    // Profil güncelleme başarısız
                                    Log.e(TAG, "Kullanıcı profilini güncelleme başarısız.", profileTask.exception)
                                }
                            }

                        // Mevcut tarihi oluşturma tarihi olarak ayarlayarak Yeni Kullanıcı nesnesini oluştur
                        val newUser = User(
                            user?.uid ?: "", // Kullanıcı null ise, id'yi boş dize olarak ayarla
                            firstName,
                            lastName,
                            phoneNumber,
                            dateOfBirth,
                            email,
                            password,
                            currentDateString // Mevcut tarihi oluşturma tarihi olarak ayarla
                        )

                        // Şimdi newUser nesnesini Firebase Realtime Database veya Firestore'a kaydedebilirsiniz
                        // Basitlik için, Firebase Realtime Database kullandığımızı varsayalım
                        val databaseReference = FirebaseDatabase.getInstance().reference.child("users").child(user?.uid ?: "")
                        databaseReference.setValue(newUser)
                            .addOnCompleteListener { databaseTask ->
                                if (databaseTask.isSuccessful) {
                                    Log.d(TAG, "Kullanıcı bilgileri veritabanına kaydedildi.")
                                } else {
                                    Log.e(TAG, "Kullanıcı bilgilerini veritabanına kaydetme başarısız.", databaseTask.exception)
                                }
                            }

                        // Başarılı kayıt sonrası arayüzü güncelle
                        updateUI(user)
                    } else {
                        // Kullanıcı oluşturma başarısız olursa, bir hata mesajı göster
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
    }

    // Kullanıcı arayüzünü güncelleyen fonksiyon
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Kullanıcı oturum açtı, ana etkinliğe yönlendirme veya başka bir işlem gerçekleştirme
            val intent = Intent(this, MenuActivity::class.java)
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
}
