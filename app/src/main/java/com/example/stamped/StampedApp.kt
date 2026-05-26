package com.example.stamped

import android.app.Application
import com.example.stamped.util.DailyTipWorker
import com.example.stamped.util.ThemeHelper

class StampedApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ThemeHelper.applyFromPrefs(this)
        if (DailyTipWorker.isEnabled(this)) {
            DailyTipWorker.schedule(this)
        }
    }
}
