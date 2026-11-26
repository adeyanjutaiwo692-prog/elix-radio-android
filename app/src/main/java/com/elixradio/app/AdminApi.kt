package com.elixradio.app

import android.content.Context
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object AdminApi {
    fun get(context: Context, query: String, callback: (JSONArray, String?) -> Unit) {
        thread {
            var rows = JSONArray()
            var error: String? = null
            try {
                val token = AuthApi.accessToken(context)
                require(token.isNotBlank()) { "Please sign in again." }
                val c = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/" + query)
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Accept", "application/json")
                }
                val code = c.responseCode
                val body = (if (code in 200..299) c.inputStream else c.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code in 200..299) rows = JSONArray(body)
                else error = "HTTP $code: $body"
                c.disconnect()
            } catch (e: Exception) {
                error = e.message ?: "Could not load dashboard data."
            }
            callback(rows, error)
        }
    }
}
