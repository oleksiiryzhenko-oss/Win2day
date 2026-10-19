package com.taichiflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.taichiflow.app.ui.navigation.AppRoot
import com.taichiflow.app.ui.theme.TaiChiFlowTheme
import com.taichiflow.app.ui.util.LocalAppContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        val container = (application as TaiChiFlowApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                TaiChiFlowTheme {
                    AppRoot()
                }
            }
        }
    }
}
