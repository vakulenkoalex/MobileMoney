package com.mobilemoney.data.repository

import android.content.Context

class ClipboardPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("clipboard_prefs", Context.MODE_PRIVATE)

    var clipboardParsingEnabled: Boolean
        get() = prefs.getBoolean("clipboard_parsing_enabled", false)
        set(value) = prefs.edit().putBoolean("clipboard_parsing_enabled", value).apply()

    var debugModeEnabled: Boolean
        get() = prefs.getBoolean("debug_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("debug_mode_enabled", value).apply()
}
