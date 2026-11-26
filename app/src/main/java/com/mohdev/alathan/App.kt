package com.mohdev.alathan

import android.app.Application
import com.mohdev.alathan.core.AppLog
import com.mohdev.alathan.core.Settings

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        Settings.init(this)
        Settings.syncFromSystem()
    }
}
