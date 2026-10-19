package com.taichiflow.app

import android.app.Application
import com.taichiflow.app.di.AppContainer
import kotlinx.coroutines.launch

class TaiChiFlowApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        com.taichiflow.app.launch.AppsFlyerHandoff.initialize(this)
        container = AppContainer(this)
        container.appScope.launch { container.repository.ensureSeeded() }
    }
}
