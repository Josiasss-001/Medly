package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.medly_proyecto.model.PerfilImagenes
import com.example.medly_proyecto.model.Receta
import com.example.medly_proyecto.model.Usuario
import com.example.medly_proyecto.repository.RecetasRepository
import com.example.medly_proyecto.repository.UsuarioRepository

class RecetasViewModel : ViewModel() {
    private val repository = RecetasRepository()
    private val usuarioRepo = UsuarioRepository()

    private val _recetas = MutableLiveData<List<Receta>?>()
    val recetas: LiveData<List<Receta>?> = _recetas

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val _perfilImagenes = MutableLiveData<PerfilImagenes?>()
    val perfilImagenes: LiveData<PerfilImagenes?> = _perfilImagenes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _deleteStatus = MutableLiveData<Pair<Boolean, String?>>()
    val deleteStatus: LiveData<Pair<Boolean, String?>> = _deleteStatus

    fun cargarRecetas(userId: String) {
        _isLoading.value = true
        repository.getRecetasDesencriptadas(userId) { lista ->
            _recetas.postValue(lista)
            _isLoading.postValue(false)
        }
        
        // Cargar también datos de perfil para el Drawer
        usuarioRepo.getUsuario(userId) { user ->
            _usuario.postValue(user)
        }
        usuarioRepo.getPerfilImagenes(userId) { imagenes ->
            _perfilImagenes.postValue(imagenes)
        }
    }

    fun eliminarReceta(recetaId: String, userId: String) {
        _isLoading.value = true
        repository.eliminarReceta(recetaId) { success, message ->
            _deleteStatus.postValue(Pair(success, message))
            if (success) {
                cargarRecetas(userId)
            } else {
                _isLoading.postValue(false)
            }
        }
    }
}
