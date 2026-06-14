package com.example.medly_proyecto.ui

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.medly_proyecto.R
import com.example.medly_proyecto.repository.UsuarioRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth

/**
 * Clase base para centralizar la navegación y lógica común de escaneo.
 */
abstract class BaseActivity : AppCompatActivity() {

    protected val autenticacion: FirebaseAuth = FirebaseAuth.getInstance()
    protected var uriImagen: Uri? = null
    protected var tipoDeEscaneo: String = ""

    protected val lanzadorGaleria = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { alSeleccionarImagen(it) }
    }

    protected val lanzadorCamara = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val uriStr = resultado.data?.getStringExtra("SCANNED_IMAGE_URI")
            if (uriStr != null) {
                alSeleccionarImagen(Uri.parse(uriStr))
            }
        }
    }

    /**
     * Método que debe implementar cada actividad para manejar la imagen seleccionada/escaneada.
     */
    protected abstract fun alSeleccionarImagen(uri: Uri)

    /**
     * Configura los clics y el estado visual de la barra de navegación inferior.
     */
    protected fun configurarNavegacionCentralizada() {
        findViewById<View>(R.id.botonInicioNav)?.setOnClickListener {
            if (this !is HomeActivity) navegarA("HomeActivity")
        }
        findViewById<View>(R.id.botonDocsNav)?.setOnClickListener {
            if (this !is DocumentosActivity) navegarA("DocumentosActivity")
        }
        findViewById<View>(R.id.botonMapasNav)?.setOnClickListener {
            if (this !is MapaActivity) navegarA("MapaActivity")
        }
        findViewById<View>(R.id.botonPerfilNav)?.setOnClickListener {
            if (this !is perfilActivity) navegarA("perfilActivity")
        }
        findViewById<View>(R.id.contenedorEscaner)?.setOnClickListener {
            mostrarOpcionesDeEscaneo()
        }

        // Cargar foto de perfil real si existe
        cargarFotoPerfilNav()

        // Marcar el ícono de la pantalla actual
        val colorCeleste = ContextCompat.getColor(this, R.color.colorIconosNav)
        when (this) {
            is HomeActivity -> findViewById<ImageView>(R.id.botonInicioNav)?.setColorFilter(colorCeleste)
            is DocumentosActivity -> findViewById<ImageView>(R.id.botonDocsNav)?.setColorFilter(colorCeleste)
            is MapaActivity -> findViewById<ImageView>(R.id.botonMapasNav)?.setColorFilter(colorCeleste)
            is perfilActivity -> {
                // En perfil podemos resaltar el borde o dejar el tinte si no hay foto
            }
        }
    }

    private fun cargarFotoPerfilNav() {
        val uid = autenticacion.currentUser?.uid ?: return
        UsuarioRepository().getPerfilImagenes(uid) { imagenes ->
            imagenes?.profileImageUrl?.let { base64 ->
                if (base64.isNotEmpty()) {
                    val bitmap = base64ToBitmap(base64)
                    if (bitmap != null) {
                        runOnUiThread {
                            findViewById<ImageView>(R.id.botonPerfilNav)?.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        }
    }

    private fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) { null }
    }

    protected fun mostrarOpcionesDeEscaneo() {
        val dialogo = BottomSheetDialog(this)
        val vista = layoutInflater.inflate(R.layout.layout_bottom_sheet_scan, null)

        vista.findViewById<View>(R.id.btnScanReceta).setOnClickListener {
            dialogo.dismiss()
            mostrarDialogoDeOrigen("RECETA")
        }

        vista.findViewById<View>(R.id.btnScanHora).setOnClickListener {
            dialogo.dismiss()
            mostrarDialogoDeOrigen("HORA")
        }

        vista.findViewById<View>(R.id.btnScanDocumento).setOnClickListener {
            dialogo.dismiss()
            tipoDeEscaneo = "DOCUMENTO"
            lanzadorGaleria.launch(arrayOf("image/*", "application/pdf"))
        }

        dialogo.setContentView(vista)
        dialogo.show()
    }

    private fun mostrarDialogoDeOrigen(tipo: String) {
        tipoDeEscaneo = tipo
        val vista = layoutInflater.inflate(R.layout.layout_dialog_source_choice, null)
        val dialogo = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(vista)
            .create()

        dialogo.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        vista.findViewById<TextView>(R.id.tvDialogTitle).text = "Subir $tipo"

        vista.findViewById<View>(R.id.btnChoiceCamera).setOnClickListener {
            dialogo.dismiss()
            abrirCamara()
        }

        vista.findViewById<View>(R.id.btnChoiceGallery).setOnClickListener {
            dialogo.dismiss()
            lanzadorGaleria.launch(arrayOf("image/*", "application/pdf"))
        }

        vista.findViewById<View>(R.id.btnCancelChoice).setOnClickListener {
            dialogo.dismiss()
        }

        dialogo.show()
    }

    private fun abrirCamara() {
        val intent = Intent(this, ScannerCameraActivity::class.java)
        lanzadorCamara.launch(intent)
    }

    protected fun navegarA(nombreClase: String) {
        try {
            val intent = Intent(this, Class.forName("com.example.medly_proyecto.ui.$nombreClase"))
            val opciones = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(intent, opciones.toBundle())
            // Si navegamos desde Home a otra, no cerramos Home (para que sea la raíz)
            // Pero si navegamos desde otra a Home, sí podemos cerrar la actual.
            if (this !is HomeActivity) finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al navegar", Toast.LENGTH_SHORT).show()
        }
    }

    protected fun navegarAConFiltro(nombreClase: String, filtro: String) {
        try {
            val intent = Intent(this, Class.forName("com.example.medly_proyecto.ui.$nombreClase"))
            intent.putExtra("INITIAL_FILTER", filtro)
            val opciones = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(intent, opciones.toBundle())
            if (this !is HomeActivity) finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al navegar", Toast.LENGTH_SHORT).show()
        }
    }

    protected fun cerrarSesionUsuario() {
        autenticacion.signOut()
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
