package io.github.danilodox.warbook.ui.components

import androidx.compose.runtime.Composable

/** O sistema pediu menos movimento (animações desligadas no Android, "Reduzir movimento" no iOS). */
@Composable
expect fun rememberReduceMotion(): Boolean
