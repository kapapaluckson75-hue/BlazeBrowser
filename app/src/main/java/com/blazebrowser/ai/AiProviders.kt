package com.blazebrowser.ai

data class AiProvider(
    val id: String,
    val displayName: String,
    val endpoint: String,
    val needsApiKey: Boolean = true,
    val defaultModel: String = ""
)

object AiProviders {
    val providers = listOf(
        AiProvider("anthropic", "Anthropic (Claude)", "https://api.anthropic.com/v1/chat/completions", true, "claude-3-sonnet-20240229"),
        AiProvider("openai", "OpenAI (GPT)", "https://api.openai.com/v1/chat/completions", true, "gpt-4o-mini"),
        AiProvider("google", "Google (Gemini)", "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent", true),
        AiProvider("nvidia", "NVIDIA NIM", "https://integrate.api.nvidia.com/v1/chat/completions", true, "meta/llama3-70b-instruct"),
        AiProvider("openrouter", "OpenRouter", "https://openrouter.ai/api/v1/chat/completions", true, "auto"),
        AiProvider("dashscope", "DashScope", "https://dashscope.aliyuncs.com/api/v1/services/text-generation/text-generation", true, "qwen-turbo"),
        AiProvider("custom", "Custom Endpoint", "", true)
    )
}
