package io.github.danilodox.warbook

import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationIdTest {

    @Test
    fun applicationIdNaoMuda() {
        // Depois de publicado na Play Store, o applicationId não pode mudar.
        assertEquals("io.github.danilodox.warbook", BuildConfig.APPLICATION_ID)
    }
}
