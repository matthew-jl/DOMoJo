package edu.bluejack24_2.domojo.repositories

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import edu.bluejack24_2.domojo.models.Challenge
import edu.bluejack24_2.domojo.utils.CloudinaryClient
import java.io.File

class ChallengeRepository() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

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
}