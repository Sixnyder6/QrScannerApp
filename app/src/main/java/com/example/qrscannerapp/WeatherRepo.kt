package com.example.qrscannerapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class WeatherData(
    val tempC: Int,
    val description: String,
    val emoji: String
)

object WeatherRepo {

    private val client = OkHttpClient()

    // Координаты Санкт-Петербурга
    private const val LAT = 59.95
    private const val LON = 30.32
    private const val URL =
        "https://api.open-meteo.com/v1/forecast?latitude=$LAT&longitude=$LON&current_weather=true"

    suspend fun load(): WeatherData? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(URL).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val current = json.getJSONObject("current_weather")
            val temp = current.getDouble("temperature").toInt()
            val code = current.getInt("weathercode")
            WeatherData(
                tempC = temp,
                description = descriptionFor(code),
                emoji = emojiFor(code)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun descriptionFor(code: Int): String = when (code) {
        0 -> "Ясно"
        1, 2 -> "Малооблачно"
        3 -> "Пасмурно"
        45, 48 -> "Туман"
        51, 53, 55 -> "Морось"
        61, 63, 65 -> "Дождь"
        71, 73, 75 -> "Снег"
        77 -> "Снежная крупа"
        80, 81, 82 -> "Ливень"
        85, 86 -> "Снегопад"
        95 -> "Гроза"
        96, 99 -> "Гроза с градом"
        else -> "Неизвестно"
    }

    private fun emojiFor(code: Int): String = when (code) {
        0 -> "☀️"
        1, 2 -> "🌤️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55 -> "🌦️"
        61, 63, 65 -> "🌧️"
        71, 73, 75, 77 -> "❄️"
        80, 81, 82 -> "⛈️"
        85, 86 -> "🌨️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }
}