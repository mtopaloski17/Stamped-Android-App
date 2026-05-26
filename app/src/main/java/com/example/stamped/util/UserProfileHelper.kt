package com.example.stamped.util

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

object UserProfileHelper {

    private const val PREFS = "stamped_settings"
    private const val KEY_DISPLAY_NAME = "user_display_name"
    private const val KEY_TITLE_ACHIEVEMENT_ID = "user_title_achievement_id"

    private fun currentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "local"
    }

    private fun displayNameKey(): String = "${KEY_DISPLAY_NAME}_${currentUserId()}"
    private fun titleKey(): String = "${KEY_TITLE_ACHIEVEMENT_ID}_${currentUserId()}"

    fun getDisplayName(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(displayNameKey(), "") ?: ""
    }

    fun setDisplayName(context: Context, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(displayNameKey(), name).apply()
    }

    fun getTitleAchievementId(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(titleKey(), null)
    }

    fun setTitleAchievementId(context: Context, id: String?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (id == null) prefs.remove(titleKey())
        else prefs.putString(titleKey(), id)
        prefs.apply()
    }

    fun deriveFromEmail(email: String): String {
        return email.substringBefore("@")
            .replace(".", " ")
            .replace("_", " ")
            .replace("-", " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            }
    }
}
