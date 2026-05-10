package com.example.medly_proyecto.model

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)
