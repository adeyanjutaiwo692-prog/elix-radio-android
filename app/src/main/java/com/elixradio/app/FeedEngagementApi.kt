package com.elixradio.app

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object FeedEngagementApi {
    data class Signals(val categoryScores: Map<String, Double>, val itemScores: Map<String, Double>)

    fun loadSignals(context: Context, callback: (Signals) -> Unit) {
        if (!AuthApi.isSignedIn(context)) { callback(Signals(emptyMap(), emptyMap())); return }
        thread {
            val categories = mutableMapOf<String, Double>()
            val items = mutableMapOf<String, Double>()
            try {
                val token = AuthApi.accessToken(context)
                val url = BuildConfig.SUPABASE_URL.trimEnd('/') +
                    "/rest/v1/media_engagement_events?select=media_id,event_type,watch_seconds,media_items(kind)&order=created_at.desc&limit=1000"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/json")
                if (connection.responseCode in 200..299) {
                    val rows = org.json.JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
                    for (i in 0 until rows.length()) {
                        val event = rows.getJSONObject(i)
                        val id = event.optString("media_id")
                        val type = event.optString("event_type")
                        val seconds = event.optInt("watch_seconds", 0)
                        val weight = when (type) {
                            "like" -> 12.0
                            "save" -> 14.0
                            "share" -> 16.0
                            "skip" -> -5.0
                            "not_interested" -> -30.0
                            "watch" -> (seconds.coerceAtMost(300) / 20.0).coerceAtLeast(0.5)
                            else -> 0.0
                        }
                        items[id] = (items[id] ?: 0.0) + weight
                        val media = event.optJSONObject("media_items")
                        val kind = media?.optString("kind").orEmpty()
                        if (kind.isNotBlank()) categories[kind] = (categories[kind] ?: 0.0) + weight
                    }
                }
                connection.disconnect()
            } catch (_: Exception) { }
            callback(Signals(categories, items))
        }
    }

    fun record(context: Context, mediaId: String, eventType: String, watchSeconds: Int = 0) {
        if (!AuthApi.isSignedIn(context) || mediaId.isBlank()) return
        thread {
            try {
                val token = AuthApi.accessToken(context)
                val body = JSONObject().put("user_id", AuthApi.currentUserId(context))
                    .put("media_id", mediaId).put("event_type", eventType)
                    .put("watch_seconds", watchSeconds.coerceIn(0, 86400))
                val connection = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/media_engagement_events").openConnection() as HttpURLConnection)
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true
                connection.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Prefer", "return=minimal")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                connection.responseCode
                connection.disconnect()
            } catch (_: Exception) { }
        }
    }
}
