package com.example.medly_proyecto.ui

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.example.medly_proyecto.BuildConfig
import com.example.medly_proyecto.R
import com.example.medly_proyecto.databinding.ActivityScannerCameraBinding
import com.example.medly_proyecto.viewmodel.ScannerCameraViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerCameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScannerCameraBinding
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService
    private var savedUri: Uri? = null
    private var isFlashOn = false
    private var isFromCamera = false

    private val viewModel: ScannerCameraViewModel by viewModels()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            deleteTempFile()
            isFromCamera = false
            savedUri = it
            cargarImagenEnPreview(it)
            binding.confirmationLayout.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.BLACK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0 
        }

        binding = ActivityScannerCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        binding.btnCapture.setOnClickListener { takePhoto() }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
        
        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }

        binding.btnRetake.setOnClickListener {
            deleteTempFile()
            binding.confirmationLayout.visibility = View.GONE
            binding.ivCapturedImage.setImageBitmap(null)
            savedUri = null
        }

        binding.btnConfirm.setOnClickListener {
            savedUri?.let { uri ->
                val b64 = convertUriToBase64(uri)
                if (b64 != null) {
                    viewModel.validarImagen(b64, BuildConfig.OPENAI_API_KEY)
                } else {
                    Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }

        observarViewModel()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun observarViewModel() {
        viewModel.isAnalyzing.observe(this) { loading ->
            binding.llValidating.visibility = if (loading) View.VISIBLE else View.GONE
            binding.llConfirmationButtons.visibility = if (loading) View.GONE else View.VISIBLE
        }

        viewModel.classificationResult.observe(this) { result ->
            result?.let {
                if (it.isValid) {
                    val finalUri = if (isFromCamera) saveImageToMediaStore(savedUri!!) else savedUri
                    deleteTempFile()

                    val resultIntent = Intent()
                    resultIntent.putExtra("SCANNED_IMAGE_URI", finalUri.toString())
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    mostrarAlertaError()
                }
                viewModel.resetResult()
            }
        }
    }

    private fun deleteTempFile() {
        if (isFromCamera) {
            savedUri?.let { uri ->
                try {
                    val file = File(uri.path ?: "")
                    if (file.exists()) file.delete()
                } catch (e: Exception) { }
            }
            savedUri = null
            isFromCamera = false
        }
    }

    private fun saveImageToMediaStore(uri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Medly-Scans")
                }
            }

            val destUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            destUri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destUri
        } catch (e: Exception) {
            null
        }
    }

    private fun cargarImagenEnPreview(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                val rotatedBitmap = rotateBitmapIfRequired(bitmap, uri)
                binding.ivCapturedImage.setImageBitmap(rotatedBitmap)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar la vista previa", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            inputStream.close()

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun mostrarAlertaError() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = layoutInflater.inflate(R.layout.layout_dialog_validation_error, null)
        dialog.setContentView(view)
        
        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        view.findViewById<View>(R.id.btnRescan).setOnClickListener {
            dialog.dismiss()
            deleteTempFile()
            binding.confirmationLayout.visibility = View.GONE
            binding.ivCapturedImage.setImageBitmap(null)
            savedUri = null
        }

        view.findViewById<View>(R.id.btnCancelError).setOnClickListener {
            dialog.dismiss()
            deleteTempFile()
            finish()
        }

        dialog.setCancelable(false)
        dialog.show()
    }

    private fun convertUriToBase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap == null) return null
            
            bitmap = rotateBitmapIfRequired(bitmap, uri)

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    private fun toggleFlash() {
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            isFlashOn = !isFlashOn
            camera?.cameraControl?.enableTorch(isFlashOn)
            
            if (isFlashOn) {
                binding.btnFlash.backgroundTintList = ContextCompat.getColorStateList(this, R.color.white)
                binding.btnFlash.imageTintList = ContextCompat.getColorStateList(this, R.color.black)
            } else {
                binding.btnFlash.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.transparent)
                binding.btnFlash.setBackgroundResource(R.drawable.badge_rounded_bg)
                binding.btnFlash.backgroundTintList = Color.parseColor("#44000000").let { android.content.res.ColorStateList.valueOf(it) }
                binding.btnFlash.imageTintList = ContextCompat.getColorStateList(this, R.color.white)
            }
        } else {
            Toast.makeText(this, "Flash no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(cacheDir, "temp_scan_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(baseContext, "Error al capturar imagen", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = Uri.fromFile(photoFile)
                    isFromCamera = true
                    savedUri = uri
                    cargarImagenEnPreview(uri)
                    binding.confirmationLayout.visibility = View.VISIBLE
                }
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                // Silenced logs as requested
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteTempFile()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permisos no concedidos.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}
