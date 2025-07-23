package edu.bluejack24_2.domojo.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import edu.bluejack24_2.domojo.models.Challenge
import edu.bluejack24_2.domojo.utils.CloudinaryClient
import java.io.File

class ChallengeRepository() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val TAG = "ChallengeRepository"

    fun createChallenge(
        context: Context,
        challenge: Challenge,
        iconFile: File,
        bannerFile: File,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        CloudinaryClient.uploadImage(
            context = context,
            uri = Uri.fromFile(iconFile),
            onSuccess = { iconUrl ->
                CloudinaryClient.uploadImage(
                    context = context,
                    uri = Uri.fromFile(bannerFile),
                    onSuccess = { bannerUrl ->
                        val finalChallenge = challenge.copy(
                            iconUrl = iconUrl,
                            bannerUrl = bannerUrl
                        )

                        firestore.collection("challenges")
                            .document()
                            .set(finalChallenge)
                            .addOnSuccessListener {
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                onFailure(
                                    e.localizedMessage
                                        ?: "Failed to save challenge data to Firestore."
                                )
                            }
                    },
                    onError = { message ->
                        onFailure("Banner image upload failed: $message")
                    }
                )
            },
            onError = { message ->
                onFailure("Icon image upload failed: $message")
            }
        )
    }

    fun getAllChallenges(onSuccess: (List<Challenge>) -> Unit, onFailure: (String) -> Unit) {
        firestore.collection("challenges")
            .get()
            .addOnSuccessListener { documents ->
                val challengeList = mutableListOf<Challenge>()
                if (documents.isEmpty) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    val challenge = document.toObject<Challenge>()
                    if (challenge != null) {
                        challengeList.add(challenge)
                    }
                }
                onSuccess(challengeList)
            }
            .addOnFailureListener { exception ->
                onFailure(exception.localizedMessage ?: "Failed to load challenges.")
            }
    }

    /**
     * Fetches a single Challenge document by its ID.
     * Used for the Challenge Detail page.
     */
    fun getChallenge(challengeId: String, onSuccess: (Challenge?) -> Unit, onFailure: (String) -> Unit) {
        Log.d(TAG, "getChallenge: Attempting to fetch challenge with ID: $challengeId")
        firestore.collection("challenges").document(challengeId).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val challenge = documentSnapshot.toObject<Challenge>() // Use toObject<T>()
                    if (challenge != null) {
                        challenge.id = documentSnapshot.id // Ensure ID is set
                        Log.d(TAG, "getChallenge: Successfully fetched challenge: ${challenge.title} (ID: ${challenge.id})")
                        onSuccess(challenge)
                    } else {
                        Log.w(TAG, "getChallenge: Failed to parse document ${documentSnapshot.id} to Challenge object (returned null).")
                        onSuccess(null) // Document exists but couldn't be parsed
                    }
                } else {
                    Log.d(TAG, "getChallenge: Challenge with ID $challengeId does not exist.")
                    onSuccess(null) // Challenge does not exist
                }
            }
            .addOnFailureListener { exception ->
                val errorMessage = exception.localizedMessage ?: "Failed to load challenge details."
                Log.e(TAG, "getChallenge: Failed to fetch challenge $challengeId: $errorMessage", exception)
                onFailure(errorMessage)
            }
    }
}