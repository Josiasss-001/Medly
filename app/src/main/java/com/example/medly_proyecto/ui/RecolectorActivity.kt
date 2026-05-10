package com.example.medly_proyecto.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.medly_proyecto.R
import com.example.medly_proyecto.viewmodel.RecolectorViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class RecolectorActivity : AppCompatActivity() {

    // Paso 1
    private lateinit var step1Container: LinearLayout
    private lateinit var nameEditText: TextInputEditText
    private lateinit var lastNameEditText: TextInputEditText
    private lateinit var ageEditText: TextInputEditText
    private lateinit var cityEditText: TextInputEditText

    // Paso 2
    private lateinit var step2Container: LinearLayout
    private lateinit var genderRadioGroup: RadioGroup
    private lateinit var weightEditText: TextInputEditText
    private lateinit var heightEditText: TextInputEditText
    private lateinit var birthDateEditText: TextInputEditText
    private lateinit var birthDateInputLayout: TextInputLayout

    // Paso 3
    private lateinit var step3Container: LinearLayout
    private lateinit var chronicDiseaseRadioGroup: RadioGroup
    private lateinit var chronicDiseaseInputLayout: TextInputLayout
    private lateinit var chronicDiseaseEditText: TextInputEditText

    // Navegación y UI
    private lateinit var backButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var stepIndicator: TextView
    private lateinit var progressBar: LinearProgressIndicator

    private var currentStep = 1
    private val viewModel: RecolectorViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recolector)

        // Inicializar vistas - Paso 1
        step1Container = findViewById(R.id.step1Container)
        nameEditText = findViewById(R.id.nameEditText)
        lastNameEditText = findViewById(R.id.lastNameEditText)
        ageEditText = findViewById(R.id.ageEditText)
        cityEditText = findViewById(R.id.cityEditText)

        // Inicializar vistas - Paso 2
        step2Container = findViewById(R.id.step2Container)
        genderRadioGroup = findViewById(R.id.genderRadioGroup)
        weightEditText = findViewById(R.id.weightEditText)
        heightEditText = findViewById(R.id.heightEditText)
        birthDateEditText = findViewById(R.id.birthDateEditText)
        birthDateInputLayout = findViewById(R.id.birthDateInputLayout)

        // Inicializar vistas - Paso 3
        step3Container = findViewById(R.id.step3Container)
        chronicDiseaseRadioGroup = findViewById(R.id.chronicDiseaseRadioGroup)
        chronicDiseaseInputLayout = findViewById(R.id.chronicDiseaseInputLayout)
        chronicDiseaseEditText = findViewById(R.id.chronicDiseaseEditText)

        // UI General
        backButton = findViewById(R.id.backButton)
        nextButton = findViewById(R.id.nextButton)
        saveButton = findViewById(R.id.saveButton)
        stepIndicator = findViewById(R.id.stepIndicatorTextView)
        progressBar = findViewById(R.id.recolectorProgressBar)

        configurarNavegacion()
        configurarSelectorFecha()
        configurarLogicaEnfermedadCronica()
        observarViewModel()
        actualizarUI()
    }

    private fun configurarNavegacion() {
        nextButton.setOnClickListener {
            if (validarPasoActual()) {
                currentStep++
                actualizarUI()
            }
        }

        backButton.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                actualizarUI()
            }
        }

        saveButton.setOnClickListener {
            if (validarPasoActual()) {
                ejecutarGuardado()
            }
        }
    }

    private fun actualizarUI() {
        // Visibilidad de contenedores
        step1Container.visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        step2Container.visibility = if (currentStep == 2) View.VISIBLE else View.GONE
        step3Container.visibility = if (currentStep == 3) View.VISIBLE else View.GONE

        // Visibilidad de botones
        backButton.visibility = if (currentStep == 1) View.GONE else View.VISIBLE
        nextButton.visibility = if (currentStep == 3) View.GONE else View.VISIBLE
        saveButton.visibility = if (currentStep == 3) View.VISIBLE else View.GONE

        // Indicador de texto y progreso de la barra celeste
        stepIndicator.text = getString(R.string.paso_formato, currentStep)
        val progress = (currentStep * 100) / 3
        progressBar.setProgress(progress, true)
    }

    private fun validarPasoActual(): Boolean {
        when (currentStep) {
            1 -> {
                if (nameEditText.text.isNullOrEmpty() || lastNameEditText.text.isNullOrEmpty() || 
                    ageEditText.text.isNullOrEmpty() || cityEditText.text.isNullOrEmpty()) {
                    Toast.makeText(this, getString(R.string.error_paso_1), Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            2 -> {
                if (weightEditText.text.isNullOrEmpty() || heightEditText.text.isNullOrEmpty() || 
                    birthDateEditText.text.isNullOrEmpty()) {
                    Toast.makeText(this, getString(R.string.error_paso_2), Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            3 -> {
                if (chronicDiseaseRadioGroup.checkedRadioButtonId == R.id.radioYes && 
                    chronicDiseaseEditText.text.isNullOrEmpty()) {
                    Toast.makeText(this, getString(R.string.error_paso_3), Toast.LENGTH_SHORT).show()
                    return false
                }
            }
        }
        return true
    }

    private fun ejecutarGuardado() {
        val nombres = nameEditText.text.toString().trim()
        val apellidos = lastNameEditText.text.toString().trim()
        val edad = ageEditText.text.toString().toIntOrNull() ?: 0
        val ciudad = cityEditText.text.toString().trim()
        
        val sexo = if (genderRadioGroup.checkedRadioButtonId == R.id.radioMale) "Hombre" else "Mujer"
        val peso = weightEditText.text.toString().toDoubleOrNull() ?: 0.0
        val estatura = heightEditText.text.toString().toIntOrNull() ?: 0
        val fechaNacimiento = birthDateEditText.text.toString()
        
        val tieneEnfermedad = chronicDiseaseRadioGroup.checkedRadioButtonId == R.id.radioYes
        val detalleEnfermedad = chronicDiseaseEditText.text.toString()

        val userId = auth.currentUser?.uid ?: return
        val correo = auth.currentUser?.email ?: ""

        viewModel.validarYGuardarPerfilCompleto(
            userId, correo, nombres, apellidos, ciudad, edad, sexo, peso, estatura, fechaNacimiento, tieneEnfermedad, detalleEnfermedad
        )
    }

    private fun observarViewModel() {
        viewModel.estadoGuardado.observe(this) { (success, message) ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            if (success) {
                val intent = Intent(this, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun configurarLogicaEnfermedadCronica() {
        chronicDiseaseRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            chronicDiseaseInputLayout.visibility = if (checkedId == R.id.radioYes) View.VISIBLE else View.GONE
        }
    }

    private fun configurarSelectorFecha() {
        val dateListener = View.OnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                birthDateEditText.setText("$d/${m + 1}/$y")
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
        birthDateEditText.setOnClickListener(dateListener)
        birthDateInputLayout.setEndIconOnClickListener(dateListener)
    }
}
