package edu.bluejack24_2.domojo.utils

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import edu.bluejack24_2.domojo.R

@BindingAdapter("imageUrl")
public fun loadImage(imageView: ImageView, url: String?){
    Glide.with(imageView.context)
        .load(url)
        .placeholder(android.R.drawable.ic_menu_gallery)
        .error(android.R.drawable.ic_menu_close_clear_cancel)
        .into(imageView)
}

@BindingAdapter("badgeId")
fun setBadgeImage(imageView: ImageView, badgeId: String?) {
    val badgeResId = when (badgeId) {
        "bronze" -> R.drawable.ic_badge_bronze
        "silver" -> R.drawable.ic_badge_silver
        "gold" -> R.drawable.ic_badge_gold
        "diamond" -> R.drawable.ic_badge_diamond
        "purple" -> R.drawable.ic_badge_purple
        else -> 0
    }

    if (badgeResId != 0) {
        imageView.setImageResource(badgeResId)
        imageView.visibility = ImageView.VISIBLE
    } else {
        imageView.visibility = ImageView.GONE
    }
}