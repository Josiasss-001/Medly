package com.example.medly_proyecto.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.medly_proyecto.model.DatosMedicos
import com.example.medly_proyecto.model.PerfilImagenes
import com.example.medly_proyecto.model.Usuario
import com.example.medly_proyecto.repository.UsuarioRepository
import com.example.medly_proyecto.util.ReporteMedicoManager
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import kotlin.concurrent.thread

/**
 * Auditoría Técnica y Corrección Definitiva:
 * 
 * 1. Eliminación de verificación HEAD: Innecesaria para Cloudinary y causa de falsos negativos.
 * 2. Forzado de resource_type: "raw" para garantizar entrega y evitar "Blocked for delivery".
 * 3. Prevención de Race Conditions: Control estricto de estados durante la carga y subida.
 * 4. Validación de URL: Filtro exhaustivo para evitar URLs temporales, malformadas o duplicadas.
 * 5. Diagnóstico: Logs detallados de la respuesta de Cloudinary.
 */
class PerfilViewModel : ViewModel() {
    private val usuarioRepository = UsuarioRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val _datosMedicos = MutableLiveData<DatosMedicos?>()
    val datosMedicos: LiveData<DatosMedicos?> = _datosMedicos

    private val _perfilImagenes = MutableLiveData<PerfilImagenes?>()
    val perfilImagenes: LiveData<PerfilImagenes?> = _perfilImagenes

    private val _credencialUrl = MutableLiveData<String?>()
    val credencialUrl: LiveData<String?> = _credencialUrl

    private val _isUploading = MutableLiveData<Boolean>(false)
    val isUploading: LiveData<Boolean> = _isUploading

    private val _errorFlow = MutableLiveData<String?>()
    val errorFlow: LiveData<String?> = _errorFlow

    private val _loggedOut = MutableLiveData<Boolean>()
    val loggedOut: LiveData<Boolean> = _loggedOut

    private val TAG_AUDIT = "AUDITORIA_FLOW"

    fun loadProfile(userId: String) {
        usuarioRepository.getUsuario(userId) { user ->
            _usuario.postValue(user)
            // Si hay un proceso de subida activo, NO sobreescribimos la URL con lo que venga de Firebase
            // para evitar race conditions donde la URL vieja llega después de iniciar el flujo.
            if (_isUploading.value == false) {
                val currentUrl = user?.credencialPdfUrl ?: ""
                if (validarUrlFinal(currentUrl)) {
                    _credencialUrl.postValue(currentUrl)
                } else {
                    Log.w(TAG_AUDIT, "URL en Firebase es inválida u omitida: $currentUrl")
                    _credencialUrl.postValue(null)
                }
            }
        }
        usuarioRepository.getDatosMedicos(userId) { medicalData ->
            _datosMedicos.postValue(medicalData)
        }
        usuarioRepository.getPerfilImagenes(userId) { imagenes ->
            _perfilImagenes.postValue(imagenes)
        }
    }

    fun generarYSubirCredencial(context: Context) {
        val user = _usuario.value
        val datos = _datosMedicos.value

        if (user == null || datos == null) {
            registrarError("Datos de perfil incompletos. Por favor, completa tu perfil primero.")
            return
        }

        // Evitar múltiples ejecuciones simultáneas
        if (_isUploading.value == true) {
            Log.w(TAG_AUDIT, "Ya hay una subida en curso. Ignorando solicitud duplicada.")
            return
        }

        _isUploading.postValue(true)
        Log.i(TAG_AUDIT, ">>> INICIANDO FLUJO DE CREDENCIAL MÉDICA (AUDITADO) <<<")

        thread {
            try {
                // 1. GENERACIÓN LOCAL
                val pdfFile = ReporteMedicoManager.generarReporteLocal(context, user, datos)
                
                // 2. VALIDACIÓN FÍSICA
                if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0) {
                    Log.d(TAG_AUDIT, "PDF Local validado. Tamaño: ${pdfFile.length()} bytes.")
                    subirACloudinary(pdfFile, user.uid)
                } else {
                    throw Exception("Error físico: El archivo PDF generado es nulo o está vacío.")
                }
            } catch (e: Exception) {
                registrarError("Fallo en generación de PDF: ${e.message}")
            }
        }
    }

    private fun subirACloudinary(file: File, userId: String) {
        // El public_id para RAW DEBE incluir la extensión para una construcción de URL limpia.
        val publicId = "credencial_$userId.pdf"
        
        Log.i(TAG_AUDIT, "Subiendo a Cloudinary. Public ID: $publicId | Resource Type: RAW")

        MediaManager.get().upload(file.absolutePath)
            .unsigned("or06mpum")
            .option("folder", "medly/credenciales")
            .option("public_id", publicId)
            .option("resource_type", "raw") // FORZADO: Evita /image/upload y errores 404/401
            .option("overwrite", true)
            .option("invalidate", true)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    Log.d(TAG_AUDIT, "Cloudinary Upload Started: $requestId")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    // DIAGNÓSTICO: Ver exactamente qué devuelve Cloudinary
                    Log.d("CLOUDINARY_DEBUG", "Resultado Completo: $resultData")

                    val secureUrl = resultData["secure_url"] as? String
                    val actualResourceType = resultData["resource_type"] as? String
                    
                    Log.i(TAG_AUDIT, "URL Recibida: $secureUrl | Type: $actualResourceType")

                    // Validamos la URL antes de cualquier acción posterior
                    if (validarUrlFinal(secureUrl)) {
                        actualizarFirebase(userId, secureUrl!!)
                    } else {
                        registrarError("URL rechazada por auditoría de seguridad/formato: $secureUrl")
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    registrarError("Cloudinary Error (${error.code}): ${error.description}")
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    _isUploading.postValue(false)
                }
            })
            .dispatch()
    }

    /**
     * Validación estricta:
     * - No nula ni vacía.
     * - Debe ser HTTPS.
     * - Debe contener /raw/upload/ (Crucial para evitar errores de entrega).
     * - Debe terminar en .pdf y NO contener duplicados .pdf.pdf.
     */
    private fun validarUrlFinal(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        
        val esHttps = url.startsWith("https://", ignoreCase = true)
        val esRaw = url.contains("/raw/upload/", ignoreCase = true)
        val lowerUrl = url.lowercase()
        val tieneUnSoloPdf = lowerUrl.endsWith(".pdf") && !lowerUrl.contains(".pdf.pdf")
        
        val esValida = esHttps && esRaw && tieneUnSoloPdf
        
        if (!esValida) {
            Log.w(TAG_AUDIT, "Validación FALLIDA para URL: $url (HTTPS: $esHttps, RAW: $esRaw, PDF_OK: $tieneUnSoloPdf)")
        }
        
        return esValida
    }

    private fun actualizarFirebase(userId: String, url: String) {
        Log.i(TAG_AUDIT, "Sincronizando URL validada con Firestore...")
        usuarioRepository.actualizarCredencialUrl(userId, url) { success ->
            if (success) {
                Log.i(TAG_AUDIT, "Firestore actualizado exitosamente. Emitiendo nueva URL para QR.")
                // IMPORTANTE: Solo emitimos a _credencialUrl después de que Firebase confirme el guardado.
                _credencialUrl.postValue(url)
            } else {
                registrarError("Error crítico: No se pudo persistir la URL final en la base de datos.")
            }
            // Finalizamos el estado de carga
            _isUploading.postValue(false)
        }
    }

    private fun registrarError(mensaje: String) {
        Log.e(TAG_AUDIT, "ERROR: $mensaje")
        _errorFlow.postValue(mensaje)
        _isUploading.postValue(false)
    }

    fun signOut() {
        auth.signOut()
        _loggedOut.value = true
    }

    fun eliminarCuenta(callback: (Boolean, String?) -> Unit) {
        usuarioRepository.eliminarCuenta { success, error ->
            callback(success, error)
        }
    }
}
