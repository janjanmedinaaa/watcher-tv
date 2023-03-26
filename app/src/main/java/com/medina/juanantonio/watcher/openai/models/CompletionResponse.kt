package com.medina.juanantonio.watcher.openai.models

data class CompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage
) {

    inner class Choice(
        val text: String,
        val index: Int,
        val logprobs: String?,
        val finish_reason: String
    )

    inner class Usage(
        val prompt_tokens: Int,
        val completion_tokens: Int,
        val total_tokens: Int
    )
}