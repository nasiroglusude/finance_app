package com.example.financeapp.activity.enterance
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.financeapp.activity.menu.MenuActivity
import com.example.financeapp.databinding.ActivityProfilePhotoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

import java.io.IOException


class ProfilePhotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilePhotoBinding


    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI: $uri")
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val drawable = BitmapDrawable(resources, bitmap)
                    binding.addRecipePhoto.setImageDrawable(drawable)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilePhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        launchPhotoPicker()
        binding.pickImageButton.setOnClickListener {
            launchPhotoPicker()
        }

        binding.btnContinue.setOnClickListener {
            // Check if an image has been selected
            if (binding.addRecipePhoto.drawable != null) {
                // If an image is selected, get its URI and upload it to Firebase
                val imageUri = (binding.addRecipePhoto.drawable as BitmapDrawable).bitmap
                uploadImageToFirebase(imageUri)
                navigateToActivity(MenuActivity())
            } else {
                Toast.makeText(this, "Please select a profile photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchPhotoPicker() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun uploadImageToFirebase(bitmap: Bitmap) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val profilePhotoRef = FirebaseDatabase.getInstance().reference
                .child("users").child(userId).child("profilePhoto")

            // Convert Bitmap to URI
            val uri = getImageUriFromBitmap(bitmap)

            profilePhotoRef.setValue(uri.toString())
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile photo uploaded successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to upload profile photo: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }

    private fun navigateToActivity(activity: Activity) {
        val intent = Intent(this, activity::class.java)
        startActivity(intent)
    }
}
