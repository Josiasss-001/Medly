package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.medly_proyecto.repository.UsuarioRepository

class EditPhotoViewModel : ViewModel() {
    private val usuarioRepository = UsuarioRepository()

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> = _updateStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun saveImages(userId: String, profileBase64: String?, backgroundBase64: String?) {
        val updates = mutableMapOf<String, Any>()
        profileBase64?.let { updates["profileImageUrl"] = it }
        backgroundBase64?.let { updates["backgroundImageUrl"] = it }

        if (updates.isNotEmpty()) {
            _isLoading.value = true
            usuarioRepository.actualizarPerfilImagenes(userId, updates) { success ->
                _isLoading.value = false
                _updateStatus.value = success
            }
        }
    }
}
