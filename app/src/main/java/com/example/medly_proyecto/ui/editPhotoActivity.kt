package com.example.medly_proyecto.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.medly_proyecto.R
import com.example.medly_proyecto.viewmodel.EditPhotoViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream

class editPhotoActivity : AppCompatActivity() {

    private lateinit var editHeaderBackground: ImageView
    private lateinit var changeBackgroundButton: FloatingActionButton
    private lateinit var editProfileImage: ShapeableImageView
    private lateinit var changePhotoButton: FloatingActionButton
    private lateinit var savePhotosButton: MaterialButton
    private lateinit var cancelPhotosButton: MaterialButton

    private val viewModel: EditPhotoViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    private var base64ProfileImage: String? = null
    private var base64BackgroundImage: String? = null

    private val pickProfileImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            editProfileImage.setImageURI(it)
            base64ProfileImage = uriToBase64(it)
        }
    }

    private val pickBackgroundImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            editHeaderBackground.setImageURI(it)
            base64BackgroundImage = uriToBase64(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_photo)

        editHeaderBackground = findViewById(R.id.editHeaderBackground)
        changeBackgroundButton = findViewById(R.id.changeBackgroundButton)
        editProfileImage = findViewById(R.id.editProfileImage)
        changePhotoButton = findViewById(R.id.changePhotoButton)
        savePhotosButton = findViewById(R.id.savePhotosButton)
        cancelPhotosButton = findViewById(R.id.cancelPhotosButton)

        cargarFotosActuales()
        observarViewModel()

        changePhotoButton.setOnClickListener {
            pickProfileImageLauncher.launch("image/*")
        }

        changeBackgroundButton.setOnClickListener {
            pickBackgroundImageLauncher.launch("image/*")
        }

        savePhotosButton.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                if (base64ProfileImage == null && base64BackgroundImage == null) {
                    Toast.makeText(this, "No se han realizado cambios", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.saveImages(userId, base64ProfileImage, base64BackgroundImage)
                }
            }
        }

        cancelPhotosButton.setOnClickListener {
            finish()
        }
    }

    private fun observarViewModel() {
        viewModel.updateStatus.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Fotos actualizadas con éxito", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al actualizar las fotos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            // Comprimimos para no exceder el límite de Firestore (1MB por documento)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    private fun cargarFotosActuales() {
        val userId = auth.currentUser?.uid ?: return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        // Cargamos desde la colección correcta
        db.collection("perfil_imagenes").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    document.getString("profileImageUrl")?.let { base64 ->
                        if (base64.isNotEmpty()) editProfileImage.setImageBitmap(stringToBitmap(base64))
                    }
                    document.getString("backgroundImageUrl")?.let { base64 ->
                        if (base64.isNotEmpty()) editHeaderBackground.setImageBitmap(stringToBitmap(base64))
                    }
                }
            }
    }

    private fun stringToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
