package com.buka.mobile.data

import android.content.Context
import android.os.Build
import android.provider.Settings

class DeviceNameStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun currentName(): String =
        preferences.getString(KEY_NAME, null)?.trim()?.takeIf(String::isNotEmpty)
            ?: systemDefaultName()

    fun save(name: String?) {
        val clean = name?.trim()?.take(48).orEmpty()
        if (clean.isEmpty()) {
            preferences.edit().remove(KEY_NAME).apply()
        } else {
            preferences.edit().putString(KEY_NAME, clean).apply()
        }
    }

    fun systemDefaultName(): String {
        val configured = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            runCatching {
                Settings.Global.getString(appContext.contentResolver, Settings.Global.DEVICE_NAME)
            }.getOrNull()
        } else {
            null
        }
        return configured?.trim()?.takeIf(String::isNotEmpty)
            ?: Build.MODEL?.trim()?.takeIf(String::isNotEmpty)
            ?: "布卡流音"
    }

    private companion object {
        const val PREFS = "buka_mobile_device"
        const val KEY_NAME = "device_name"
    }
}
