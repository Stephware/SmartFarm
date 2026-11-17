package com.smartfarm.mobile.ui.components.onboarding

import android.content.Context
import android.content.SharedPreferences

/**
 * Small, device-wide first-run flag for the instructional AniGo tour.
 *
 * Does not contain account data or farm records. It's intentionally device-scoped
 * so switching accounts or signing out never forces the same introduction again.
 * Clearing the app's storage resets the flag, as expected for a new installation.
 */
class AniGoOnboardingStore private constructor(
    private val preferences: SharedPreferences
) {
    fun isCompleted(): Boolean = preferences.getBoolean(COMPLETED_KEY, false)

    /**
     * Persist before navigating away from first-run onboarding. This tiny,
     * infrequent synchronous write avoids a lost flag if Android terminates
     * the process immediately after navigation.
     */
    fun markCompleted(): Boolean =
        preferences.edit().putBoolean(COMPLETED_KEY, true).commit()

    companion object {
        private const val DEFAULT_PREFS = "anigo_onboarding_preferences"
        private const val COMPLETED_KEY = "completed_v1"

        fun create(context: Context, fileName: String = DEFAULT_PREFS): AniGoOnboardingStore =
            AniGoOnboardingStore(
                context.applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)
            )
    }
}

/** Pure decision to keep first-run route selection simple and testable. */
internal fun anigoInitialRoute(completed: Boolean): String =
    if (completed) "welcome" else "onboarding"
