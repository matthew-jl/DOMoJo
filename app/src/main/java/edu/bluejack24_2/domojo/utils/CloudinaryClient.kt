package edu.bluejack24_2.domojo.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.callback.ErrorInfo

object CloudinaryClient {
    private var isCloudinaryInitialized = false

    fun uploadImage(
        context: Context,
        uri: Uri,
        onSuccess: (imageUrl: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val config: HashMap<String, String> = hashMapOf(
            "cloud_name" to "dr09lvoly",
            "api_key" to "381411341393715",
            "api_secret" to "sKXu1uATsyZlSxjfEXNtOATI4mQ"
        )

        try {
            if (!isCloudinaryInitialized) {
                MediaManager.init(context.applicationContext, config)
                isCloudinaryInitialized = true
                Log.d("CLOUDINARY", "MediaManager initialized.")
            } else {
                Log.d("CLOUDINARY", "MediaManager already initialized (skipped init).")
            }

            Log.d("CLOUDINARY", "Starting upload for URI: $uri")
            MediaManager.get().upload(uri)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d("CLOUDINARY", "Upload started")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        Log.d("CLOUDINARY", "Progress: $bytes / $totalBytes")
                    }

                    override fun onSuccess(requestId: String, resultData: MutableMap<Any?, Any?>) {
                        val imageUrl = resultData["secure_url"].toString()
                        Log.d("CLOUDINARY", "Upload success: $imageUrl")
                        onSuccess(imageUrl) // ✅ Return URL to caller
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("CLOUDINARY", "Upload error: ${error.description}")
                        onError(error.description) // ✅ Return error to caller
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w("CLOUDINARY", "Upload rescheduled: ${error.description}")
                        onError("Rescheduled: ${error.description}") // ✅ Also handle this as error
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            onError("Upload failed: ${e.message}")
        }
    }
}
