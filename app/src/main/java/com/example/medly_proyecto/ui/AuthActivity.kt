package com.example.medly_proyecto.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
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
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthActivity : AppCompatActivity() {

    private lateinit var authToggleGroup: MaterialButtonToggleGroup
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var llConfirmPasswordContainer: LinearLayout
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var rlLoginOptions: RelativeLayout
    private lateinit var btnAction: MaterialButton
    private lateinit var googleButton: MaterialButton
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvHeaderSubtitle: TextView

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    private var isLoginMode = true

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { idToken ->
                    firebaseAuthWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                mostrarAlerta("Error Google (Código ${e.statusCode}): ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MEDLY_PROYECTO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)

        // Inicialización de vistas - Solo las que existen en el XML
        authToggleGroup = findViewById(R.id.authToggleGroup)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        llConfirmPasswordContainer = findViewById(R.id.llConfirmPasswordContainer)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        rlLoginOptions = findViewById(R.id.rlLoginOptions)
        btnAction = findViewById(R.id.btnAction)
        googleButton = findViewById(R.id.googleButton)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle)

        configurarGoogleSignIn()
        configurarToggle()
        configurarBotones()
        observarViewModel()

        viewModel.verificarSesionActual()
    }

    private fun configurarGoogleSignIn() {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
    }

    private fun configurarToggle() {
        authToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnSelectLogin -> setLoginMode(true)
                    R.id.btnSelectRegister -> setLoginMode(false)
                }
            }
        }
    }

    private fun setLoginMode(login: Boolean) {
        isLoginMode = login
        if (login) {
            tvHeaderTitle.text = "¡Sigue adelante y completa tu cuenta!"
            tvHeaderSubtitle.text = "Crea tu cuenta y simplifica tu salud al instante."
            llConfirmPasswordContainer.visibility = View.GONE
            rlLoginOptions.visibility = View.VISIBLE
            btnAction.text = getString(R.string.ingresar)
        } else {
            tvHeaderTitle.text = "Crea tu cuenta ahora"
            tvHeaderSubtitle.text = "Únete a Medly para gestionar tu salud de forma inteligente."
            llConfirmPasswordContainer.visibility = View.VISIBLE
            rlLoginOptions.visibility = View.GONE
            btnAction.text = getString(R.string.registrar)
        }
    }

    private fun configurarBotones() {
        btnAction.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (isLoginMode) {
                viewModel.validarEIniciarSesion(email, password)
            } else {
                val confirmPass = confirmPasswordEditText.text.toString().trim()
                // Registro simplificado: Solo correo y claves
                viewModel.validarYRegistrar(email, password, confirmPass)
            }
        }

        googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun observarViewModel() {
        viewModel.authState.observe(this) { (exitoso, mensaje) ->
            if (exitoso) {
                if (!isLoginMode && mensaje?.contains("exitoso") == true) {
                    Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                    authToggleGroup.check(R.id.btnSelectLogin)
                } else {
                    viewModel.verificarUsuario()
                }
            } else if (mensaje != null) {
                mostrarAlerta(mensaje)
            }
        }

        viewModel.userVerificationState.observe(this) { userType ->
            when (userType) {
                AuthViewModel.UserType.EXISTING -> irAhome()
                AuthViewModel.UserType.NEW, AuthViewModel.UserType.INCOMPLETE -> irARecolector()
                AuthViewModel.UserType.ERROR -> mostrarAlerta("Error al procesar el perfil")
                else -> {}
            }
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

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        viewModel.autenticarConGoogle(credential)
    }
}
