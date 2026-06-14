package com.example.medly_proyecto.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
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
    private lateinit var loadingOverlay: ConstraintLayout

    private val viewModel: PerfilViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private var generacionDisparada = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_credencial_qr)

        ivCredencialFoto = findViewById(R.id.ivCredencialFoto)
        tvCredencialNombre = findViewById(R.id.tvCredencialNombre)
        ivCredencialQR = findViewById(R.id.ivCredencialQR)
        btnCerrarCredencial = findViewById(R.id.btnCerrarCredencial)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        btnCerrarCredencial.setOnClickListener {
            if (loadingOverlay.visibility != View.VISIBLE) {
                finish()
            }
        }

        observarViewModel()
        
        val uid = auth.currentUser?.uid
        if (uid != null) {
            viewModel.loadProfile(uid)
            
            val debeGenerar = intent.getBooleanExtra("GENERAR_NUEVA", false)
            if (debeGenerar) {
                Log.d("CredencialQR", "Esperando Usuario y Datos médicos de Firebase...")
                
                // Observamos ambos para asegurar que el PDF no salga vacío
                viewModel.usuario.observe(this) { user ->
                    val datos = viewModel.datosMedicos.value
                    if (user != null && datos != null) {
                        iniciarProceso()
                    }
                }
                
                viewModel.datosMedicos.observe(this) { datos ->
                    val user = viewModel.usuario.value
                    if (user != null && datos != null) {
                        iniciarProceso()
                    }
                }
            }
        }
    }

    private fun iniciarProceso() {
        if (generacionDisparada) return
        generacionDisparada = true
        Log.d("CredencialQR", "Todo listo. Disparando generación de PDF y subida a Cloudinary.")
        viewModel.generarYSubirCredencial(this)
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

        viewModel.credencialUrl.observe(this) { url ->
            if (!url.isNullOrEmpty()) {
                Log.d("CredencialQR", "Nueva URL recibida: $url. Actualizando QR...")
                generateQRCode(url)
            }
        }

        viewModel.isUploading.observe(this) { isUploading ->
            if (isUploading) {
                loadingOverlay.visibility = View.VISIBLE
                btnCerrarCredencial.isEnabled = false
            } else {
                loadingOverlay.visibility = View.GONE
                btnCerrarCredencial.isEnabled = true
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
            Log.i("CredencialQR", "QR generado y mostrado con éxito.")
        } catch (e: WriterException) {
            Log.e("CredencialQR", "Error al generar QR: ${e.message}")
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

    override fun onBackPressed() {
        if (loadingOverlay.visibility != View.VISIBLE) {
            super.onBackPressed()
        }
    }
}
