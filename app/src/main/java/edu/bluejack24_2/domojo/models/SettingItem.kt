package edu.bluejack24_2.domojo.models

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes

// Setting Item component for Profile Page
data class SettingItem(
    val icon: Drawable,
    val name: String,
    val description: String,
)
