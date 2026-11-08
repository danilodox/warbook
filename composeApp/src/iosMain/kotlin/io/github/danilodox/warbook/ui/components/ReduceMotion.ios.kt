package io.github.danilodox.warbook.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

// Nunca compilado: o iOS precisa de um Mac (ver handoff).

@Composable
actual fun rememberReduceMotion(): Boolean = remember { UIAccessibilityIsReduceMotionEnabled() }
