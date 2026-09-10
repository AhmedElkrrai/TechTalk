package com.elkrrai.techtalk.presentation.component

import androidx.compose.runtime.Composable

/** No-op on iOS — there is no system back gesture equivalent to intercept here; screens
 * that need a close affordance render their own back/close button. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Intentionally empty.
}
