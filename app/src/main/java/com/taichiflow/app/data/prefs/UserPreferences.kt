package com.taichiflow.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.taichiflow.app.domain.model.Difficulty
import com.taichiflow.app.domain.model.Goal
import com.taichiflow.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taichiflow_prefs")

/** Profile, XP and settings stored with DataStore. */
class UserPreferences(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val NAME = stringPreferencesKey("display_name")
        val EXPERIENCE = stringPreferencesKey("experience")
        val GOAL = stringPreferencesKey("goal")
        val XP = longPreferencesKey("total_xp")
        val SEEDED = booleanPreferencesKey("seeded")
        val FOCUS_DEFAULT = booleanPreferencesKey("focus_mode_default")
        val HAPTICS = booleanPreferencesKey("haptics")
    }

    val profile: Flow<UserProfile> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            UserProfile(
                name = p[Keys.NAME] ?: DEFAULT_NAME,
                experience = Difficulty.values().firstOrNull { it.name == p[Keys.EXPERIENCE] } ?: Difficulty.BEGINNER,
                goal = Goal.values().firstOrNull { it.name == p[Keys.GOAL] } ?: Goal.FLOW,
                totalXp = p[Keys.XP] ?: 0L,
                seeded = p[Keys.SEEDED] ?: false,
                focusModeDefault = p[Keys.FOCUS_DEFAULT] ?: false,
                haptics = p[Keys.HAPTICS] ?: true
            )
        }

    suspend fun current(): UserProfile = profile.first()

    suspend fun updateProfile(name: String, experience: Difficulty, goal: Goal) {
        store.edit {
            it[Keys.NAME] = name.trim().ifBlank { DEFAULT_NAME }
            it[Keys.EXPERIENCE] = experience.name
            it[Keys.GOAL] = goal.name
        }
    }

    suspend fun addXp(amount: Long) {
        store.edit { it[Keys.XP] = (it[Keys.XP] ?: 0L) + amount }
    }

    suspend fun setXp(amount: Long) {
        store.edit { it[Keys.XP] = amount }
    }

    suspend fun setSeeded(value: Boolean) {
        store.edit { it[Keys.SEEDED] = value }
    }

    suspend fun setFocusModeDefault(value: Boolean) {
        store.edit { it[Keys.FOCUS_DEFAULT] = value }
    }

    suspend fun setHaptics(value: Boolean) {
        store.edit { it[Keys.HAPTICS] = value }
    }

    /** Clears profile, XP and settings but remembers that the first-launch demo was already offered. */
    suspend fun reset() {
        store.edit {
            it.clear()
            it[Keys.SEEDED] = true
        }
    }

    companion object {
        const val DEFAULT_NAME = "Practitioner"
    }
}
