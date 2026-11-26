package com.elixradio.app

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object SupabaseApi {
    private const val BASE = "https://sumqnvpvtqpbbbytmbvk.supabase.co/rest/v1/"
    private const val KEY = "sb_publishable_-H-6950SAms-43NMmbM0xQ_rTz-lGl0"

    fun get(tableQuery: String, callback: (JSONArray) -> Unit) {
        thread {
            var result = JSONArray()
            try {
                val connection = URL(BASE + tableQuery).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.setRequestProperty("apikey", KEY)
                connection.setRequestProperty("Authorization", "Bearer " + KEY)
                connection.setRequestProperty("Accept", "application/json")
                if (connection.responseCode in 200..299) {
                    result = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
                }
                connection.disconnect()
            } catch (_: Exception) { }
            callback(result)
        }
    }
}
