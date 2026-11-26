package com.elixradio.app

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.concurrent.thread

object UploadApi {
    data class Result(val ok: Boolean, val message: String)

    fun upload(context: Context, uri: Uri, title: String, callback: (Result) -> Unit) {
        thread {
            var result: Result
            try {
                val token = AuthApi.accessToken(context)
                val userId = AuthApi.currentUserId(context)
                require(token.isNotBlank() && userId.isNotBlank()) { "Please sign in before uploading." }
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val mediaType = when {
                    mime.startsWith("audio/") -> "audio"
                    mime.startsWith("video/") -> "video"
                    mime == "application/pdf" -> "pdf"
                    mime.startsWith("image/") -> "image"
                    else -> throw IllegalArgumentException("Only audio, video, PDF and image files are supported.")
                }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Could not read the selected file.")
                require(bytes.isNotEmpty()) { "The selected file is empty." }
                require(bytes.size <= 100 * 1024 * 1024) { "Maximum upload size is 100 MB." }
                val ext = when (mime) {
                    "audio/mpeg" -> "mp3"
                    "audio/mp4" -> "m4a"
                    "audio/aac" -> "aac"
                    "audio/wav", "audio/x-wav" -> "wav"
                    "audio/ogg" -> "ogg"
                    "video/mp4" -> "mp4"
                    "video/quicktime" -> "mov"
                    "application/pdf" -> "pdf"
                    "image/jpeg" -> "jpg"
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "bin"
                }
                val path = "$userId/${UUID.randomUUID()}.$ext"
                val base = BuildConfig.SUPABASE_URL.trimEnd('/')
                val storage = (URL("$base/storage/v1/object/elix-media/$path").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 30000
                    readTimeout = 60000
                    doOutput = true
                    setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", mime)
                    setRequestProperty("x-upsert", "false")
                    outputStream.use { it.write(bytes) }
                }
                val storageCode = storage.responseCode
                val storageBody = (if (storageCode in 200..299) storage.inputStream else storage.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                storage.disconnect()
                if (storageCode !in 200..299) throw IllegalStateException("File upload failed: $storageBody")

                val row = JSONObject()
                    .put("uploaded_by", userId)
                    .put("title", title)
                    .put("kind", "other")
                    .put("media_type", mediaType)
                    .put("file_path", path)
                    .put("rights_confirmed", true)
                val record = (URL("$base/rest/v1/media_items").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 20000
                    doOutput = true
                    setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Prefer", "return=minimal")
                    outputStream.use { it.write(row.toString().toByteArray(Charsets.UTF_8)) }
                }
                val recordCode = record.responseCode
                val recordBody = (if (recordCode in 200..299) record.inputStream else record.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                record.disconnect()
                if (recordCode !in 200..299) throw IllegalStateException("File uploaded but catalogue publishing failed: $recordBody")
                result = Result(true, "Uploaded successfully. It is now available in ELIX Radio.")
            } catch (e: Exception) {
                result = Result(false, e.message ?: "Upload failed.")
            }
            callback(result)
        }
    }
}
