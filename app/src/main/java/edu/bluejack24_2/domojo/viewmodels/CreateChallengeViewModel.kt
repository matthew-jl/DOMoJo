package edu.bluejack24_2.domojo.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CreateChallengeViewModel : ViewModel() {
    val challengeTitle = MutableLiveData<String>()
    val challengeCategory = MutableLiveData<String>()
    val challengeDescription = MutableLiveData<String>()

    val challengeTitleError = MutableLiveData<String?>()
    val challengeCategoryError = MutableLiveData<String?>()
    val challengeDescriptionError = MutableLiveData<String?>()
    val challengeIconError = MutableLiveData<String?>()
    val challengeBannerError = MutableLiveData<String?>()
}