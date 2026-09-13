package com.tangai.memento.domain.model

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class UsernameTest {
    @Test fun searchNormalizationIgnoresWhitespaceAndDeviceLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("alice", normalizeUsername(" A L\tI\nCE\u2003"))
            assertEquals(normalizeUsername("Alice"), User("uid", "Alice").usernameNormalized)
        } finally {
            Locale.setDefault(previous)
        }
    }
}
