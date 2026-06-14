package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.medly_proyecto.model.DatosMedicos
import com.example.medly_proyecto.model.Usuario
import com.example.medly_proyecto.repository.UsuarioRepository

class RecolectorViewModel : ViewModel() {
    private val usuarioRepo = UsuarioRepository()

    private val _estadoGuardado = MutableLiveData<Pair<Boolean, String?>>()
    val estadoGuardado: LiveData<Pair<Boolean, String?>> = _estadoGuardado

    fun validarYGuardarPerfilCompleto(
        userId: String,
        correo: String,
        nombres: String,
        apellidos: String,
        ciudad: String,
        edad: Int,
        sexo: String,
        peso: Double,
        estatura: Int,
        fechaNacimiento: String,
        tieneEnfermedad: Boolean,
        detalleEnfermedad: String,
        institucionSalud: String
    ) {
        val usuario = Usuario(
            uid = userId,
            nombres = nombres,
            apellidos = apellidos,
            nombreCompleto = "$nombres $apellidos",
            correo = correo,
            ciudad = ciudad,
            perfilCompleto = true
        )

        val datosMedicos = DatosMedicos(
            uid = userId,
            fechaNacimiento = fechaNacimiento,
            edad = edad,
            peso = peso,
            estatura = estatura,
            sexo = sexo,
            enfermedadCronica = tieneEnfermedad,
            detalleEnfermedad = if (tieneEnfermedad) detalleEnfermedad else "",
            institucionSalud = institucionSalud
        )

        usuarioRepo.actualizarPerfilCompleto(usuario, datosMedicos) { exito, error ->
            if (exito) {
                _estadoGuardado.value = Pair(true, "¡Perfil completado con éxito!")
            } else {
                _estadoGuardado.value = Pair(false, error ?: "Hubo un error al guardar tus datos.")
            }
        }
    }
}
