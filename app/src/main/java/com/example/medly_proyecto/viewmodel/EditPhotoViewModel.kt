package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.medly_proyecto.repository.UsuarioRepository

class EditPhotoViewModel : ViewModel() {
    private val usuarioRepository = UsuarioRepository()

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> = _updateStatus

    fun saveImages(userId: String, profileBase64: String?, backgroundBase64: String?) {
        val updates = mutableMapOf<String, Any>()
        profileBase64?.let { updates["profileImageUrl"] = it }
        backgroundBase64?.let { updates["backgroundImageUrl"] = it }

        if (updates.isNotEmpty()) {
            usuarioRepository.actualizarCamposUsuario(userId, updates) { success ->
                _updateStatus.value = success
            }
        }
    }
}
