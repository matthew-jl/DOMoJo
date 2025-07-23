package edu.bluejack24_2.domojo.views.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import edu.bluejack24_2.domojo.utils.LocaleHelper

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
}