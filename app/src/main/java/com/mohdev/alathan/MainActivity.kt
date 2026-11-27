// Copyright (c) 2026 Moh-F. All rights reserved. Proprietary source-available license, see LICENSE.
package com.mohdev.alathan

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mohdev.alathan.ui.AlathanRoot
import com.mohdev.alathan.ui.AppState

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
        setContent { AlathanRoot() }
    }

    /** OK / Menu on the remote opens the settings panel when no focused control consumed the key. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val open = keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A
        if (open && !AppState.settingsOpen && (event?.repeatCount ?: 0) == 0) {
            AppState.settingsOpen = true
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
