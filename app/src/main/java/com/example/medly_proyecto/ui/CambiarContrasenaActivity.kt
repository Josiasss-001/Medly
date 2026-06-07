package com.example.medly_proyecto.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.medly_proyecto.databinding.ActivityCambiarContrasenaBinding
import com.example.medly_proyecto.databinding.DialogoConfirmarIdentidadBinding
import com.example.medly_proyecto.viewmodel.CambiarContrasenaViewModel

class CambiarContrasenaActivity : AppCompatActivity() {

    private lateinit var enlace: ActivityCambiarContrasenaBinding
    private val modeloVista: CambiarContrasenaViewModel by viewModels()
    private var estaAutenticado = false
    private var dialogoSeguridad: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enlace = ActivityCambiarContrasenaBinding.inflate(layoutInflater)
        setContentView(enlace.root)

        configurarVista()
        observarModelo()
        mostrarDialogoReautenticacion()
    }

    private fun configurarVista() {
        ViewCompat.setOnApplyWindowInsetsListener(enlace.main) { vista, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, barrasSistema.bottom)
            insets
        }

        enlace.btnBack.setOnClickListener {
            finish()
        }

        enlace.btnActualizar.setOnClickListener {
            if (estaAutenticado) {
                val nuevaPass = enlace.newPasswordEdit.text.toString()
                val confirmarPass = enlace.confirmPasswordEdit.text.toString()
                modeloVista.actualizarContrasena(nuevaPass, confirmarPass)
            } else {
                mostrarDialogoReautenticacion()
            }
        }
    }

    private fun observarModelo() {
        modeloVista.estaCargando.observe(this) { cargando ->
            enlace.progressBar.visibility = if (cargando) View.VISIBLE else View.GONE
            enlace.btnActualizar.isEnabled = !cargando
        }

        modeloVista.resultadoReautenticacion.observe(this) { resultado ->
            val (exito, mensaje) = resultado
            if (exito) {
                estaAutenticado = true
                dialogoSeguridad?.dismiss()
                Toast.makeText(this, "Identidad confirmada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, mensaje ?: "Error de autenticación", Toast.LENGTH_SHORT).show()
            }
        }

        modeloVista.resultadoCambio.observe(this) { resultado ->
            val (exito, mensaje) = resultado
            if (exito) {
                Toast.makeText(this, "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, mensaje ?: "Error al cambiar contraseña", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoReautenticacion() {
        val constructor = AlertDialog.Builder(this)
        val enlaceDialogo = DialogoConfirmarIdentidadBinding.inflate(LayoutInflater.from(this))
        constructor.setView(enlaceDialogo.root)
        constructor.setCancelable(false)

        dialogoSeguridad = constructor.create()
        dialogoSeguridad?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        enlaceDialogo.btnConfirmar.setOnClickListener {
            val correo = enlaceDialogo.etCorreo.text.toString()
            val pass = enlaceDialogo.etPass.text.toString()
            
            if (correo.isNotEmpty() && pass.isNotEmpty()) {
                modeloVista.reautenticarUsuario(correo, pass)
            } else {
                Toast.makeText(this, "Ingresa tus credenciales", Toast.LENGTH_SHORT).show()
            }
        }

        enlaceDialogo.btnCancelar.setOnClickListener {
            dialogoSeguridad?.dismiss()
            finish()
        }

        dialogoSeguridad?.show()
    }
}
