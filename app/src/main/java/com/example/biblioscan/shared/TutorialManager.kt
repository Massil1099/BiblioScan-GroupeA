package com.example.biblioscan.shared

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class TutorialManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE)

    fun hasSeenTutorial(): Boolean {
        return prefs.getBoolean("seen_tutorial", false)
    }

    fun setTutorialSeen() {
        prefs.edit() { putBoolean("seen_tutorial", true) }
    }

    fun resetTutorial() {
        prefs.edit() { putBoolean("seen_tutorial", false) }
    }
}
