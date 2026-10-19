package com.taichiflow.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object TC {
    val DeepRed = Color(0xFF8B0A1A)
    val Crimson = Color(0xFFDC143C)
    val Ember = Color(0xFFFF4D5E)
    val Blood = Color(0xFF3A0610)
    val Black = Color(0xFF0A0A0B)
    val Charcoal = Color(0xFF141416)
    val DarkGray = Color(0xFF1E1E22)
    val Gray = Color(0xFF2A2A30)
    val Line = Color(0xFF34343C)
    val Muted = Color(0xFF8A8A93)
    val Soft = Color(0xFFBDBDC4)
    val White = Color(0xFFF5F5F7)

    val redGradient: Brush get() = Brush.linearGradient(listOf(Crimson, DeepRed))
    val cardGradient: Brush get() = Brush.verticalGradient(listOf(DarkGray, Charcoal))
    val heroGradient: Brush get() = Brush.verticalGradient(listOf(Blood, Black))
}
