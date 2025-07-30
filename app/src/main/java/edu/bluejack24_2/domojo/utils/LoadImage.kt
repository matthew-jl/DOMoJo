package edu.bluejack24_2.domojo.utils

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

@BindingAdapter("imageUrl")
public fun loadImage(imageView: ImageView, url: String?){
    Glide.with(imageView.context)
        .load(url)
        .placeholder(android.R.drawable.ic_menu_gallery)
        .error(android.R.drawable.ic_menu_close_clear_cancel)
        .into(imageView)
}