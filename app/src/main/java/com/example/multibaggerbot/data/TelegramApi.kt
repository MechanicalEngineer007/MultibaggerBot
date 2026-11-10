package com.example.multibaggerbot.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class TelegramApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(botToken: String, chatId: String, messageText: String): Boolean = withContext(Dispatchers.IO) {
        if (botToken.isBlank() || chatId.isBlank()) return@withContext false

        val url = "https://api.telegram.org/bot$botToken/sendMessage"
        val payload = """
            {
                "chat_id": "$chatId",
                "text": ${escapeJsonString(messageText)},
                "parse_mode": "HTML"
            }
        """.trimIndent()

        val body = payload.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            val successful = response.isSuccessful
            response.close()
            successful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun escapeJsonString(text: String): String {
        return "\"" + text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\""
    }
}
