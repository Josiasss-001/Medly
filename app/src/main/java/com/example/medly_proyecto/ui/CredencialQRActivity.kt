package com.example.medly_proyecto.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.medly_proyecto.R
import com.example.medly_proyecto.viewmodel.PerfilViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix

class CredencialQRActivity : AppCompatActivity() {

    private lateinit var ivCredencialFoto: ImageView
    private lateinit var tvCredencialNombre: TextView
    private lateinit var ivCredencialQR: ImageView
    private lateinit var btnCerrarCredencial: ImageButton

    private val viewModel: PerfilViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_credencial_qr)

        ivCredencialFoto = findViewById(R.id.ivCredencialFoto)
        tvCredencialNombre = findViewById(R.id.tvCredencialNombre)
        ivCredencialQR = findViewById(R.id.ivCredencialQR)
        btnCerrarCredencial = findViewById(R.id.btnCerrarCredencial)

        btnCerrarCredencial.setOnClickListener {
            finish()
        }

        observarViewModel()
        
        auth.currentUser?.uid?.let { uid ->
            viewModel.loadProfile(uid)
            generateQRCode(uid)
        }
    }

    private fun observarViewModel() {
        viewModel.usuario.observe(this) { usuario ->
            usuario?.let {
                tvCredencialNombre.text = it.nombreCompleto.uppercase()
            }
        }

        viewModel.perfilImagenes.observe(this) { imagenes ->
            imagenes?.let {
                if (it.profileImageUrl.isNotEmpty()) {
                    ivCredencialFoto.setImageBitmap(base64ToBitmap(it.profileImageUrl))
                }
            }
        }
    }

    private fun generateQRCode(text: String) {
        val width = 500
        val height = 500
        val writer = MultiFormatWriter()
        try {
            val bitMatrix: BitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            ivCredencialQR.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    private fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
