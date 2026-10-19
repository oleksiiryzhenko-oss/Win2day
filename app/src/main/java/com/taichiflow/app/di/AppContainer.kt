package com.taichiflow.app.di

import android.content.Context
import com.taichiflow.app.data.local.AppDatabase
import com.taichiflow.app.data.prefs.UserPreferences
import com.taichiflow.app.data.repository.TaiChiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency container: one database, one preferences store, one repository. */
class AppContainer(context: Context) {
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database: AppDatabase = AppDatabase.build(context)
    val preferences: UserPreferences = UserPreferences(context)
    val repository: TaiChiRepository = TaiChiRepository(database, preferences, appScope)
}
