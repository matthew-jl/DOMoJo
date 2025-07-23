package edu.bluejack24_2.domojo.views.ui

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import edu.bluejack24_2.domojo.utils.LocaleHelper
import edu.bluejack24_2.domojo.utils.ThemeHelper

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(ThemeHelper.getSavedTheme(this))
        super.onCreate(savedInstanceState)
    }
}