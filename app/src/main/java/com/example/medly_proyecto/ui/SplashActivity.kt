package com.example.medly_proyecto.ui

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.cloudinary.android.MediaManager
import com.example.medly_proyecto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        inicializarCloudinary()

        Handler(Looper.getMainLooper()).postDelayed({
            checkSession()
        }, 3000)
    }

    private fun inicializarCloudinary() {
        try {
            // Para la opción más segura (Subidas NO firmadas), solo necesitamos el cloud_name.
            // Esto evita exponer el api_secret en el código de la aplicación.
            val config = mutableMapOf<String, String>()
            config["cloud_name"] = "dd9py23dp"
            
            MediaManager.init(this, config)
            Log.d("CloudinaryInit", "Cloudinary configurado exitosamente para subidas no firmadas")
        } catch (e: Exception) {
            Log.w("CloudinaryInit", "Aviso: Cloudinary ya inicializado o error: ${e.message}")
        }
    }

    private fun checkSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("usuarios").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val nombres = document.getString("nombres") ?: ""
                        if (nombres.isNotEmpty()) {
                            irAHome()
                        } else {
                            irARecolector()
                        }
                    } else {
                        irARecolector()
                    }
                }
                .addOnFailureListener {
                    irAHome()
                }
        } else {
            irAAuth()
        }
    }

    private fun irAHome() {
        val intent = Intent(this, HomeActivity::class.java)
        val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
        startActivity(intent, options.toBundle())
        finish()
    }

    private fun irARecolector() {
        val intent = Intent(this, RecolectorActivity::class.java)
        val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
        startActivity(intent, options.toBundle())
        finish()
    }

    private fun irAAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
        startActivity(intent, options.toBundle())
        finish()
    }
}
