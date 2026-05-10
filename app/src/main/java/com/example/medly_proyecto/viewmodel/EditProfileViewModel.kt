package com.example.medly_proyecto.viewmodel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.medly_proyecto.model.DatosMedicos
import com.example.medly_proyecto.model.Usuario
import com.example.medly_proyecto.repository.UsuarioRepository

class EditProfileViewModel : ViewModel() {
    private val usuarioRepository = UsuarioRepository()

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val _datosMedicos = MutableLiveData<DatosMedicos?>()
    val datosMedicos: LiveData<DatosMedicos?> = _datosMedicos

    private val _updateStatus = MutableLiveData<Pair<Boolean, String?>>()
    val updateStatus: LiveData<Pair<Boolean, String?>> = _updateStatus

    fun loadUserData(userId: String) {
        usuarioRepository.getUsuario(userId) { user ->
            _usuario.value = user
        }
        usuarioRepository.getDatosMedicos(userId) { medicalData ->
            _datosMedicos.value = medicalData
        }
    }

    fun validarYActualizar(
        userId: String,
        correo: String,
        nombres: String,
        apellidos: String,
        ciudad: String,
        fechaNac: String,
        edad: Int,
        peso: Double,
        estatura: Int,
        sexo: String,
        esCronico: Boolean,
        detalleEnf: String,
        fechaReg: Long
    ) {
        if (nombres.isEmpty()) {
            _updateStatus.value = Pair(false, "El nombre es obligatorio")
            return
        }

        val usuarioObj = Usuario(
            uid = userId,
            nombres = nombres,
            apellidos = apellidos,
            nombreCompleto = "$nombres $apellidos",
            correo = correo,
            ciudad = ciudad,
            fechaRegistro = fechaReg,
            perfilCompleto = true
        )

        val datosMedicosObj = DatosMedicos(
            uid = userId,
            ciudad = ciudad,
            fechaNacimiento = fechaNac,
            edad = edad,
            peso = peso,
            estatura = estatura,
            sexo = sexo,
            enfermedadCronica = esCronico,
            detalleEnfermedad = detalleEnf
        )

        usuarioRepository.actualizarPerfilCompleto(usuarioObj, datosMedicosObj) { success, error ->

            if (success) {
                _updateStatus.value = Pair(true, "Datos actualizados con éxito")
            } else {
                _updateStatus.value = Pair(false, error ?: "Error al conectar con el servidor")
            }
        }
    }
}