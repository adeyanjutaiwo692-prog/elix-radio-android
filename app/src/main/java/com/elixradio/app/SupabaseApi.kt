package com.elixradio.app

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object SupabaseApi {
    private const val BASE = "https://sumqnvpvtqpbbbytmbvk.supabase.co/rest/v1/"
    private const val KEY = "sb_publishable_-H-6950SAms-43NMmbM0xQ_rTz-lGl0"

    fun getWithError(context: android.content.Context, tableQuery: String, callback: (JSONArray, String?) -> Unit) {
        thread {
            var rows = JSONArray()
            var error: String? = null
            try {
                val token = AuthApi.accessToken(context).ifBlank { KEY }
                val connection = URL(BASE + tableQuery).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("apikey", KEY)
                connection.setRequestProperty("Authorization", "Bearer " + token)
                connection.setRequestProperty("Accept", "application/json")
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code in 200..299) rows = JSONArray(body) else error = "HTTP $code: $body"
                connection.disconnect()
            } catch (e: Exception) { error = e.message ?: "Could not load catalogue." }
            callback(rows, error)
        }
    }

    fun postWithError(context: android.content.Context, table: String, body: org.json.JSONObject, callback: (String?) -> Unit) {
        thread {
            var error: String? = null
            try {
                val connection = URL(BASE + table).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doOutput = true
                connection.setRequestProperty("apikey", KEY)
                connection.setRequestProperty("Authorization", "Bearer " + AuthApi.accessToken(context))
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Prefer", "return=minimal")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val response = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error = "HTTP $code: $response"
                connection.disconnect()
            } catch (e: Exception) { error = e.message ?: "Request failed." }
            callback(error)
        }
    }

    fun patchWithError(context: android.content.Context, tableQuery: String, body: org.json.JSONObject, callback: (String?) -> Unit) {
        thread {
            var error: String? = null
            try {
                val connection = URL(BASE + tableQuery).openConnection() as HttpURLConnection
                connection.requestMethod = "PATCH"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doOutput = true
                connection.setRequestProperty("apikey", KEY)
                connection.setRequestProperty("Authorization", "Bearer " + AuthApi.accessToken(context))
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Prefer", "return=minimal")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val response = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error = "HTTP $code: $response"
                connection.disconnect()
            } catch (e: Exception) { error = e.message ?: "Request failed." }
            callback(error)
        }
    }

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
