package edu.bluejack24_2.domojo.viewmodels

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.bluejack24_2.domojo.utils.CloudinaryClient
import edu.bluejack24_2.domojo.views.ui.CreateChallengeActivity
import edu.bluejack24_2.domojo.views.ui.RegisterActivity
import java.io.File

class CreateChallengeViewModel : ViewModel() {
    val challengeTitle = MutableLiveData<String>()
    val challengeCategory = MutableLiveData<String>()
    val challengeDescription = MutableLiveData<String>()

    val challengeTitleError = MutableLiveData<String?>()
    val challengeCategoryError = MutableLiveData<String?>()
    val challengeDescriptionError = MutableLiveData<String?>()
    private lateinit var firestore: FirebaseFirestore

    private lateinit var activity: CreateChallengeActivity

    fun setActivity(activity: CreateChallengeActivity) {
        this.activity = activity
    }

    fun onCreateClicked(icon: File, banner: File){
        firestore = FirebaseFirestore.getInstance()

        val challengeTitleValue = challengeTitle.value
        val challengeCategoryValue = challengeCategory.value
        val challengeDescriptionValue = challengeDescription.value

        challengeTitleError.value = null
        challengeCategoryError.value = null
        challengeDescriptionError.value = null

        if (challengeTitleValue.isNullOrBlank()) {
            challengeTitleError.value = "Title is required!"
            return
        }

        if (challengeCategoryValue.isNullOrBlank()) {
            challengeCategoryError.value = "Category is required!"
            return
        }

        if(challengeDescriptionValue.isNullOrBlank()) {
            challengeDescriptionError.value = "Description is required!"
            return
        }

        CloudinaryClient.uploadImage(
            context = activity,
            Uri.fromFile(icon),
            onSuccess = { resultIcon ->
                CloudinaryClient.uploadImage(
                    context = activity,
                    Uri.fromFile(banner),
                    onSuccess = { resultBanner ->
                        val challenge = hashMapOf(
                            "title" to challengeTitleValue,
                            "category" to challengeCategoryValue,
                            "description" to challengeDescriptionValue,
                            "iconUrl" to resultIcon,
                            "bannerUrl" to resultBanner,
                        )

                        firestore.collection("challenges")
                            .document()
                            .set(challenge)
                            .addOnSuccessListener {
                                Log.d("FIRESTORE_SUCCESS", "Challenge data added successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.w("FIRESTORE_ERROR", "Error add challenge data", e)
                            }
                    },
                    onError = { message ->
                        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                    })
                },
                onError = { message ->
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
