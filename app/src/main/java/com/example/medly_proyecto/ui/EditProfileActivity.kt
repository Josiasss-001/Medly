package com.example.medly_proyecto.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.medly_proyecto.R
import com.example.medly_proyecto.viewmodel.EditProfileViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class EditProfileActivity : AppCompatActivity() {

    private lateinit var editNameEditText: TextInputEditText
    private lateinit var editApellidoEditText: TextInputEditText
    private lateinit var editCityEditText: TextInputEditText
    private lateinit var editBirthDateEditText: TextInputEditText
    private lateinit var editAgeEditText: TextInputEditText
    private lateinit var editWeightEditText: TextInputEditText
    private lateinit var editHeightEditText: TextInputEditText
    
    private lateinit var editGenderRadioGroup: RadioGroup
    private lateinit var editChronicRadioGroup: RadioGroup
    private lateinit var editChronicDetailInputLayout: TextInputLayout
    private lateinit var editChronicDetailEditText: TextInputEditText
    
    private lateinit var saveChangesButton: MaterialButton
    private lateinit var cancelButton: MaterialButton
    private lateinit var backButton: ImageButton

    private val viewModel: EditProfileViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    private var fechaRegistroExistente: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)

        // Inicializar vistas
        editNameEditText = findViewById(R.id.editNameEditText)
        editApellidoEditText = findViewById(R.id.editApellidoEditText)
        editCityEditText = findViewById(R.id.editCityEditText)
        editBirthDateEditText = findViewById(R.id.editBirthDateEditText)
        editAgeEditText = findViewById(R.id.editAgeEditText)
        editWeightEditText = findViewById(R.id.editWeightEditText)
        editHeightEditText = findViewById(R.id.editHeightEditText)
        
        editGenderRadioGroup = findViewById(R.id.editGenderRadioGroup)
        editChronicRadioGroup = findViewById(R.id.editChronicRadioGroup)
        editChronicDetailInputLayout = findViewById(R.id.editChronicDetailInputLayout)
        editChronicDetailEditText = findViewById(R.id.editChronicDetailEditText)
        
        saveChangesButton = findViewById(R.id.saveChangesButton)
        cancelButton = findViewById(R.id.cancelButton)
        backButton = findViewById(R.id.backButton)

        observarViewModel()
        
        auth.currentUser?.uid?.let { viewModel.loadUserData(it) }

        configurarDatePicker()
        configurarLogicaEnfermedad()

        saveChangesButton.setOnClickListener {
            guardarCambios()
        }

        cancelButton.setOnClickListener {
            finish()
        }

        // Lógica del botón de retroceso
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun observarViewModel() {

        viewModel.usuario.observe(this) { usuario ->

            usuario?.let {

                // Si existen nombres y apellidos separados
                if (it.nombres.isNotEmpty() || it.apellidos.isNotEmpty()) {

                    editNameEditText.setText(it.nombres)
                    editApellidoEditText.setText(it.apellidos)

                } else {

                    // Compatibilidad con usuarios antiguos
                    val partes = it.nombreCompleto.split(" ")

                    if (partes.isNotEmpty()) {

                        editNameEditText.setText(partes[0])

                        if (partes.size > 2) {

                            editApellidoEditText.setText(
                                partes.subList(2, partes.size)
                                    .joinToString(" ")
                            )
                        }
                    }
                }

                fechaRegistroExistente = it.fechaRegistro
            }
        }

        viewModel.datosMedicos.observe(this) { datos ->

            datos?.let {

                editCityEditText.setText(it.ciudad)
                editBirthDateEditText.setText(it.fechaNacimiento)
                editAgeEditText.setText(it.edad.toString())
                editWeightEditText.setText(it.peso.toString())
                editHeightEditText.setText(it.estatura.toString())
                editChronicDetailEditText.setText(it.detalleEnfermedad)

                if (it.sexo == "Hombre") {

                    findViewById<RadioButton>(
                        R.id.editRadioMale
                    ).isChecked = true

                } else if (it.sexo == "Mujer") {

                    findViewById<RadioButton>(
                        R.id.editRadioFemale
                    ).isChecked = true
                }

                if (it.enfermedadCronica) {

                    findViewById<RadioButton>(
                        R.id.editRadioChronicYes
                    ).isChecked = true

                    editChronicDetailInputLayout.visibility = View.VISIBLE

                } else {

                    findViewById<RadioButton>(
                        R.id.editRadioChronicNo
                    ).isChecked = true

                    editChronicDetailInputLayout.visibility = View.GONE
                }
            }
        }

        viewModel.updateStatus.observe(this) { (success, message) ->

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

            if (success) {

                val intent = Intent(this, HomeActivity::class.java)

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)
                finish()
            }
        }
    }

    private fun configurarDatePicker() {
        editBirthDateEditText.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                editBirthDateEditText.setText("$d/${m + 1}/$y")
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun configurarLogicaEnfermedad() {
        editChronicRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            editChronicDetailInputLayout.visibility = if (checkedId == R.id.editRadioChronicYes) View.VISIBLE else View.GONE
        }
    }

    private fun guardarCambios() {
        val userId = auth.currentUser?.uid ?: return
        val correo = auth.currentUser?.email ?: ""
        
        val nombre = editNameEditText.text.toString().trim()
        val apellido = editApellidoEditText.text.toString().trim()
        val ciudad = editCityEditText.text.toString().trim()
        val fechaNac = editBirthDateEditText.text.toString().trim()
        val edad = editAgeEditText.text.toString().toIntOrNull() ?: 0
        val peso = editWeightEditText.text.toString().toDoubleOrNull() ?: 0.0
        val estatura = editHeightEditText.text.toString().toIntOrNull() ?: 0
        val sexo = if (editGenderRadioGroup.checkedRadioButtonId == R.id.editRadioMale) "Hombre" else "Mujer"
        val esCronico = editChronicRadioGroup.checkedRadioButtonId == R.id.editRadioChronicYes
        val detalleEnf = editChronicDetailEditText.text.toString().trim()

        viewModel.validarYActualizar(
            userId = userId,
            correo = correo,
            nombres = nombre,
            apellidos = apellido,
            ciudad = ciudad,
            fechaNac = fechaNac,
            edad = edad,
            peso = peso,
            estatura = estatura,
            sexo = sexo,
            esCronico = esCronico,
            detalleEnf = detalleEnf,
            fechaReg = fechaRegistroExistente
        )
    }
}
