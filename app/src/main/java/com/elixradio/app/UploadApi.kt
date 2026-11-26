package com.elixradio.app

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.concurrent.thread

object UploadApi {
    data class Result(val ok: Boolean, val message: String, val filePath: String? = null)

    private const val MAX_FILE_BYTES = 5L * 1024L * 1024L * 1024L
    private const val CHUNK_BYTES = 6 * 1024 * 1024

    fun upload(context: Context, uri: Uri, title: String, selectedType: String, category: String, pageId: String? = null, callback: (Result) -> Unit) {
        thread {
            val result = try {
                val token = AuthApi.accessToken(context)
                val userId = AuthApi.currentUserId(context)
                require(token.isNotBlank() && userId.isNotBlank()) { "Please sign in before uploading." }
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val mediaType = when {
                    mime.startsWith("audio/") -> "audio"
                    mime.startsWith("video/") -> "video"
                    mime.startsWith("image/") -> "image"
                    mime == "application/pdf" -> "pdf"
                    else -> throw IllegalArgumentException("Choose an audio, video or image file.")
                }
                require(mediaType == selectedType) { "The selected file does not match the media type you chose." }
                val size = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
                    ?: throw IllegalStateException("Could not read the selected file size.")
                require(size > 0) { "The selected file is empty." }
                require(size <= MAX_FILE_BYTES) { "Maximum upload size is 5 GB." }

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
                val projectRef = BuildConfig.SUPABASE_URL.substringAfter("https://").substringBefore(".")
                val endpoint = "https://$projectRef.storage.supabase.co/storage/v1/upload/resumable"
                val metadata = listOf(
                    "bucketName" to "elix-media",
                    "objectName" to path,
                    "contentType" to mime,
                    "cacheControl" to "3600"
                ).joinToString(",") { (key, value) ->
                    "$key ${Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)}"
                }

                val create = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 30000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("Tus-Resumable", "1.0.0")
                    setRequestProperty("Upload-Length", size.toString())
                    setRequestProperty("Upload-Metadata", metadata)
                    setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $token")
                }
                val createCode = create.responseCode
                val uploadLocation = create.getHeaderField("Location")
                val createError = (if (createCode in 200..299) create.inputStream else create.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                create.disconnect()
                if (createCode !in 200..299 || uploadLocation.isNullOrBlank()) {
                    throw IllegalStateException("Could not start resumable upload: $createError")
                }
                val uploadUrl = URL(URL(endpoint), uploadLocation).toString()

                context.contentResolver.openInputStream(uri)?.use { input ->
                    var offset = 0L
                    val buffer = ByteArray(CHUNK_BYTES)
                    while (offset < size) {
                        val wanted = minOf(CHUNK_BYTES.toLong(), size - offset).toInt()
                        var count = 0
                        while (count < wanted) {
                            val n = input.read(buffer, count, wanted - count)
                            if (n < 0) throw IllegalStateException("File ended before the expected size.")
                            count += n
                        }

                        var sent = false
                        var lastError = "Upload interrupted."
                        repeat(3) {
                            if (!sent) {
                                try {
                                    val patch = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
                                        requestMethod = "PATCH"
                                        connectTimeout = 30000
                                        readTimeout = 120000
                                        doOutput = true
                                        setFixedLengthStreamingMode(count)
                                        setRequestProperty("Tus-Resumable", "1.0.0")
                                        setRequestProperty("Upload-Offset", offset.toString())
                                        setRequestProperty("Content-Type", "application/offset+octet-stream")
                                        setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                                        setRequestProperty("Authorization", "Bearer $token")
                                    }
                                    patch.outputStream.use { it.write(buffer, 0, count) }
                                    val code = patch.responseCode
                                    val body = (if (code in 200..299) patch.inputStream else patch.errorStream)
                                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                                    patch.disconnect()
                                    if (code == 204) {
                                        sent = true
                                    } else {
                                        lastError = "Chunk upload failed ($code): $body"
                                    }
                                } catch (e: Exception) {
                                    lastError = e.message ?: "Network error during upload."
                                }
                            }
                        }
                        if (!sent) throw IllegalStateException(lastError)
                        offset += count
                    }
                } ?: throw IllegalStateException("Could not open the selected file.")

                val row = JSONObject()
                    .put("uploaded_by", userId)
                    .put("title", title)
                    .put("kind", category)
                    .put("media_type", mediaType)
                    .put("file_path", path)
                    .put("rights_confirmed", true)
                    .apply { if (!pageId.isNullOrBlank()) put("ministry_id", pageId) }
                val record = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/media_items").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 20000
                    doOutput = true
                    setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Prefer", "return=representation")
                    outputStream.use { it.write(row.toString().toByteArray(Charsets.UTF_8)) }
                }
                val recordCode = record.responseCode
                val recordBody = try {
                    (if (recordCode in 200..299) record.inputStream else record.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                } catch (e: Exception) {
                    "Could not read server response: ${e.message ?: "unknown error"}"
                }
                val recordStatus = record.responseMessage ?: "Unknown response"
                record.disconnect()
                if (recordCode !in 200..299) {
                    val detail = recordBody.ifBlank { "The server returned an empty error body." }
                    throw IllegalStateException("File uploaded but catalogue publishing failed (HTTP $recordCode $recordStatus): $detail")
                }
                if (recordBody.isBlank()) {
                    throw IllegalStateException("Catalogue request returned HTTP $recordCode but no confirmation was received. Please refresh Library before retrying.")
                }
                Result(true, "Uploaded successfully. It is now available in ELIX Radio.", path)
            } catch (e: Exception) {
                Result(false, e.message ?: "Upload failed.")
            }
            callback(result)
        }
    }

    fun uploadProfileImage(context: Context, uri: Uri, userId: String, callback: (Pair<Boolean, String>) -> Unit) {
        thread {
            val result = try {
                val token = AuthApi.accessToken(context)
                require(token.isNotBlank() && userId.isNotBlank()) { "Please sign in first." }
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                require(mime in listOf("image/jpeg", "image/png", "image/webp")) { "Choose a JPG, PNG or WebP image." }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Could not read this image.")
                require(bytes.isNotEmpty() && bytes.size <= 10 * 1024 * 1024) { "Profile pictures must be under 10 MB." }
                val ext = when (mime) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
                val path = userId + "/profile-" + UUID.randomUUID() + "." + ext
                val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/elix-public/" + Uri.encode(path, "/")
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 60000
                    doOutput = true
                    setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer " + token)
                    setRequestProperty("Content-Type", mime)
                    setRequestProperty("x-upsert", "true")
                    setFixedLengthStreamingMode(bytes.size)
                    outputStream.use { it.write(bytes) }
                }
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                connection.disconnect()
                if (code !in 200..299) throw IllegalStateException("Storage HTTP " + code + ": " + body)
                Pair(true, BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/elix-public/" + Uri.encode(path, "/"))
            } catch (e: Exception) { Pair(false, e.message ?: "Image upload failed.") }
            callback(result)
        }
    }

    fun uploadThumbnailBytes(context: Context, bytes: ByteArray, userId: String, callback: (Pair<Boolean, String>) -> Unit) {
        thread {
            val result = try {
                val token = AuthApi.accessToken(context)
                require(token.isNotBlank() && bytes.isNotEmpty()) { "Could not prepare the thumbnail." }
                val path = userId + "/thumb-" + UUID.randomUUID() + ".jpg"
                val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/elix-public/" + Uri.encode(path, "/")
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 20000; readTimeout = 30000; doOutput = true
                    setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer " + token)
                    setRequestProperty("Content-Type", "image/jpeg")
                    setRequestProperty("x-upsert", "true")
                    setFixedLengthStreamingMode(bytes.size)
                    outputStream.use { it.write(bytes) }
                }
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                connection.disconnect()
                if (code !in 200..299) throw IllegalStateException("Thumbnail storage HTTP " + code + ": " + body)
                Pair(true, BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/elix-public/" + Uri.encode(path, "/"))
            } catch (e: Exception) { Pair(false, e.message ?: "Thumbnail upload failed.") }
            callback(result)
        }
    }

    fun deleteStoredFile(context: Context, path: String, callback: (String?) -> Unit) {
        thread {
            var error: String? = null
            try {
                val token = AuthApi.accessToken(context)
                require(token.isNotBlank()) { "Please sign in to delete this upload." }
                val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/elix-media/" + Uri.encode(path, "/")
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    connectTimeout = 20000
                    readTimeout = 20000
                    setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $token")
                }
                val code = connection.responseCode
                val response = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error = "Storage HTTP $code: $response"
                connection.disconnect()
            } catch (e: Exception) { error = e.message ?: "Could not delete stored file." }
            callback(error)
        }
    }

}
