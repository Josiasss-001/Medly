package com.example.medly_proyecto.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.medly_proyecto.R
import com.example.medly_proyecto.viewmodel.RegisterViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    private lateinit var botonVolver: MaterialButton
    private lateinit var botonRegistrar: MaterialButton
    private lateinit var correoEditText: TextInputEditText
    private lateinit var contrasenaEditText: TextInputEditText
    private lateinit var confirmarContrasenaEditText: TextInputEditText
    private lateinit var confirmarContrasenaInputLayout: TextInputLayout

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        botonVolver = findViewById(R.id.volverButton)
        botonRegistrar = findViewById(R.id.registerButton)
        correoEditText = findViewById(R.id.emailEditText)
        contrasenaEditText = findViewById(R.id.passwordEditText)
        confirmarContrasenaEditText = findViewById(R.id.confirmPasswordEditText)
        confirmarContrasenaInputLayout = findViewById(R.id.confirmPasswordInputLayout)

        val email = intent.getStringExtra("email") ?: ""
        correoEditText.setText(email)

        configurarListeners()
        observarViewModel()
    }

    private fun configurarListeners() {
        botonRegistrar.setOnClickListener {
            val correo = correoEditText.text.toString().trim()
            val contrasena = contrasenaEditText.text.toString().trim()
            val confirmarContrasena = confirmarContrasenaEditText.text.toString().trim()
            
            // Enviamos los datos al ViewModel para que él decida si son válidos
            viewModel.validarYRegistrar(correo, contrasena, confirmarContrasena)
        }

        botonVolver.setOnClickListener {
            finish()
        }
    }

    private fun observarViewModel() {
        viewModel.registerState.observe(this) { (exitoso, mensaje) ->
            if (exitoso) {
                Toast.makeText(this, "Usuario creado exitosamente", Toast.LENGTH_SHORT).show()
                irALogin()
                finish()
            } else {
                // Si el mensaje es sobre contraseñas, lo mostramos en el InputLayout
                if (mensaje?.contains("contraseña") == true) {
                    confirmarContrasenaInputLayout.error = mensaje
                } else {
                    Toast.makeText(this, mensaje ?: "Error desconocido", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun irALogin() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
    }
}
