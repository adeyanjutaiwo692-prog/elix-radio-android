package com.elixradio.app

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object AuthApi {
    data class Result(val ok: Boolean, val message: String, val name: String = "", val role: String = "")

    private fun request(path: String, method: String, body: JSONObject?, token: String? = null): JSONObject {
        val url = URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path)
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
        }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val raw = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
        c.disconnect()
        val json = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
        if (code !in 200..299) {
            val msg = json.optString("msg").ifBlank {
                json.optString("message").ifBlank { json.optString("error_description").ifBlank { "Request failed ($code)" } }
            }
            throw IllegalStateException(msg)
        }
        return json
    }

    fun signUp(context: Context, name: String, email: String, password: String, callback: (Result) -> Unit) {
        thread {
            val result = try {
                val body = JSONObject().put("email", email).put("password", password)
                    .put("data", JSONObject().put("full_name", name).put("content_policy_accepted", true))
                val response = request("/auth/v1/signup", "POST", body)
                val token = response.optString("access_token")
                if (token.isNotBlank()) {
                    saveSession(context, token, response.optJSONObject("user")?.optString("id").orEmpty(), name)
                    Result(true, "Account created and signed in.")
                } else {
                    Result(true, "Account created. Check your email to confirm it, then sign in.")
                }
            } catch (e: Exception) {
                Result(false, e.message ?: "Could not create account.")
            }
            callback(result)
        }
    }

    fun signIn(context: Context, email: String, password: String, callback: (Result) -> Unit) {
        thread {
            val result = try {
                val body = JSONObject().put("email", email).put("password", password)
                val response = request("/auth/v1/token?grant_type=password", "POST", body)
                val user = response.optJSONObject("user") ?: JSONObject()
                val token = response.optString("access_token")
                val userId = user.optString("id")
                saveSession(context, token, userId, user.optJSONObject("user_metadata")?.optString("full_name").orEmpty())
                Result(true, "Signed in successfully.", currentName(context), "listener")
            } catch (e: Exception) {
                Result(false, e.message ?: "Could not sign in.")
            }
            callback(result)
        }
    }

    fun signOut(context: Context) {
        val prefs = context.getSharedPreferences("elix_auth", Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", null)
        if (!token.isNullOrBlank()) {
            thread { try { request("/auth/v1/logout", "POST", JSONObject(), token) } catch (_: Exception) { } }
        }
        prefs.edit().clear().apply()
    }

    fun accessToken(context: Context): String =
        context.getSharedPreferences("elix_auth", Context.MODE_PRIVATE).getString("access_token", "").orEmpty()

    fun currentUserId(context: Context): String =
        context.getSharedPreferences("elix_auth", Context.MODE_PRIVATE).getString("user_id", "").orEmpty()

    fun currentName(context: Context): String =
        context.getSharedPreferences("elix_auth", Context.MODE_PRIVATE).getString("display_name", "").orEmpty()

    fun isSignedIn(context: Context): Boolean =
        !context.getSharedPreferences("elix_auth", Context.MODE_PRIVATE).getString("access_token", null).isNullOrBlank()

    private fun saveSession(context: Context, token: String, userId: String, name: String) {
        context.getSharedPreferences("elix_auth", Context.MODE_PRIVATE).edit()
            .putString("access_token", token).putString("user_id", userId)
            .putString("display_name", name).apply()
    }
}
