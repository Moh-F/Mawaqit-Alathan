// Copyright (c) 2026 Moh-F. All rights reserved. Proprietary source-available license, see LICENSE.
package com.mohdev.alathan.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mohdev.alathan.core.UpdateInfo
import com.mohdev.alathan.data.DayTimes
import com.mohdev.alathan.data.PrayerRepository

object AppState {
    var day by mutableStateOf<DayTimes?>(null)
    var update by mutableStateOf<UpdateInfo?>(null)
    var settingsOpen by mutableStateOf(false)
    var showLog by mutableStateOf(false)
    var loading by mutableStateOf(false)

    suspend fun refreshTimes(ctx: Context) {
        loading = true
        try {
            PrayerRepository.load(ctx)?.let { day = it }
        } finally {
            loading = false
        }
    }
}
