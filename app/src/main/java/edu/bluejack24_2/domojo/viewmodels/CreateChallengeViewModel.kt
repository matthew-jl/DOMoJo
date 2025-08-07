package edu.bluejack24_2.domojo.viewmodels

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.bluejack24_2.domojo.models.Challenge
import edu.bluejack24_2.domojo.repositories.ChallengeRepository
import java.io.File

class CreateChallengeViewModel : ViewModel() {

    private val _navigateToChallenge = MutableLiveData<Boolean>()
    val navigateToChallenge: LiveData<Boolean> get() = _navigateToChallenge

    val challengeRepository: ChallengeRepository = ChallengeRepository()
    val challengeTitle = MutableLiveData<String>()
    val challengeCategory = MutableLiveData<String>()
    val challengeDescription = MutableLiveData<String>()

    val challengeTitleError = MutableLiveData<String?>()
    val challengeCategoryError = MutableLiveData<String?>()
    val challengeDescriptionError = MutableLiveData<String?>()

    val isLoading = MutableLiveData<Boolean>()

    fun onCreateClicked(context: Context, icon: File, banner: File) {

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

        if (challengeDescriptionValue.isNullOrBlank()) {
            challengeDescriptionError.value = "Description is required!"
            return
        }

        isLoading.value = true

        val challengeToCreate = Challenge(
            title = challengeTitleValue,
            category = challengeCategoryValue,
            description = challengeDescriptionValue,
            iconUrl = "",
            bannerUrl = "",
        )

        challengeRepository.createChallenge(
            context = context,
            challenge = challengeToCreate,
            iconFile = icon,
            bannerFile = banner,
            onSuccess = {
                isLoading.value = false
                _navigateToChallenge.value = true
            },
            onFailure = { errorMessage ->
                isLoading.value = false
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        )
    }
}
