package com.example.medly_proyecto.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.medly_proyecto.R
import com.example.medly_proyecto.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthActivity : AppCompatActivity() {
    private lateinit var loginButton : MaterialButton
    private lateinit var signupButton : MaterialButton
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var googleButton : Button

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { idToken ->
                    firebaseAuthWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                // El código 10 suele ser error de SHA-1 o Web Client ID
                // El código 12500 suele ser falta de correo de soporte en Firebase
                mostrarAlerta("Error Google (Código ${e.statusCode}): ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MEDLY_PROYECTO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        signupButton = findViewById(R.id.signupButton)
        googleButton = findViewById(R.id.googleButton)

        configuracion()
        observarViewModel()

        viewModel.verificarSesionActual()
    }

    private fun observarViewModel() {
        viewModel.authState.observe(this) { (exitoso, error) ->
            if (exitoso) {
                viewModel.verificarUsuario()
            } else if (error != null) {
                mostrarAlerta(error)
            }
        }

        viewModel.userVerificationState.observe(this) { userType ->
            when (userType) {
                // Navegamos al Home en cualquier caso de éxito de autenticación
                AuthViewModel.UserType.EXISTING, AuthViewModel.UserType.NEW -> irAhome()
                AuthViewModel.UserType.ERROR -> {
                    // Si hubo error en Firestore pero Auth fue exitosa, igual intentamos entrar
                    irAhome()
                }
                else -> {}
            }
        }
    }

    private fun configuracion() {
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            viewModel.validarEIniciarSesion(email, password)
        }

        signupButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java).apply {
                putExtra("email", emailEditText.text.toString())
            }
            startActivity(intent)
        }

        googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun mostrarAlerta(mensaje: String?) {
        AlertDialog.Builder(this)
            .setTitle("Atención")
            .setMessage(mensaje ?: "Error desconocido")
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun irAhome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        viewModel.autenticarConGoogle(credential)
    }
}
