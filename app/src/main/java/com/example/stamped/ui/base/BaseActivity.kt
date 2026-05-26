package com.example.stamped.ui.base

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.stamped.util.LocaleHelper

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }
}
