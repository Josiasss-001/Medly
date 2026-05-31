package com.example.medly_proyecto.model

data class ChatRequest(
    val model: String = "gpt-4o",
    val messages: List<Message>,
    val response_format: ResponseFormat? = null
)

data class ResponseFormat(
    val type: String = "json_object"
)

data class Message(
    val role: String,
    val content: Any // Can be String or List<ContentPart>
)

data class ContentPart(
    val type: String,
    val text: String? = null,
    val image_url: ImageUrl? = null
)

data class ImageUrl(
    val url: String // data:image/jpeg;base64,{base64_image}
)
