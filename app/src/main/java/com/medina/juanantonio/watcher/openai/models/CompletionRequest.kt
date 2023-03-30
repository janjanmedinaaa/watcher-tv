package com.medina.juanantonio.watcher.openai.models

data class CompletionRequest(
    val model: String,
    val prompt: String,
    val temperature: Float,
    val max_tokens: Int,
    val top_p: Float,
    val frequency_penalty: Float,
    val presence_penalty: Float
)