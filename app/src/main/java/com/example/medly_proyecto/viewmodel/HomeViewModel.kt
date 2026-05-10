package com.example.medly_proyecto.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medly_proyecto.model.ChatRequest
import com.example.medly_proyecto.model.Message
import com.example.medly_proyecto.model.PerfilImagenes
import com.example.medly_proyecto.model.Usuario
import com.example.medly_proyecto.repository.OpenAIService
import com.example.medly_proyecto.repository.UsuarioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {
    private val usuarioRepo = UsuarioRepository()
    private val openAIService = OpenAIService.create()

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val _perfilImagenes = MutableLiveData<PerfilImagenes?>()
    val perfilImagenes: LiveData<PerfilImagenes?> = _perfilImagenes

    private val _fraseIA = MutableLiveData<String>()
    val fraseIA: LiveData<String> = _fraseIA

    fun cargarDatos(uid: String) {
        usuarioRepo.getUsuario(uid) { user ->
            _usuario.value = user
        }
        usuarioRepo.getPerfilImagenes(uid) { imagenes ->
            _perfilImagenes.value = imagenes
        }
    }

    fun iniciarFrases(apiKey: String) {
        viewModelScope.launch {
            val request = ChatRequest(
                messages = listOf(
                    Message(role = "system", content = "Eres un asistente motivacional para una app de salud. Da una frase corta en español."),
                    Message(role = "user", content = "Dame una frase motivacional para hoy.")
                )
            )
            while (isActive) {
                try {
                    val response = withContext(Dispatchers.IO) {
                        openAIService.getCompletion("Bearer $apiKey", request)
                    }
                    _fraseIA.postValue(response.choices.firstOrNull()?.message?.content?.trim()?.replace("\"", ""))
                } catch (e: Exception) {
                    _fraseIA.postValue("La salud es tu mayor riqueza.")
                }
                delay(10 * 60 * 1000)
            }
        }
    }
}
