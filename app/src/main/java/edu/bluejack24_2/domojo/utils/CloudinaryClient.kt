package edu.bluejack24_2.domojo.utils

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

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
            } else {
            }

            MediaManager.get().upload(uri)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    }

                    override fun onSuccess(requestId: String, resultData: MutableMap<Any?, Any?>) {
                        val imageUrl = resultData["secure_url"].toString()
                        onSuccess(imageUrl)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        onError(error.description)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        onError("Rescheduled: ${error.description}")
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            onError("Upload failed: ${e.message}")
        }
    }
}
