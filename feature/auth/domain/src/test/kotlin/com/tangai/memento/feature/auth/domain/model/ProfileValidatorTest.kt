package com.tangai.memento.feature.auth.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileValidatorTest {
    @Test
    fun acceptsMvpProfileLimits() {
        assertTrue(ProfileValidator.validate("Alice", "Alice_01", "A short bio").isValid)
        assertTrue(ProfileValidator.validate("A", "ab", "").isValid)
    }

    @Test
    fun rejectsInvalidFields() {
        assertFalse(ProfileValidator.validate(" ", "ab", "").isValid)
        assertFalse(ProfileValidator.validate("Alice", "a b", "").isValid)
        assertFalse(ProfileValidator.validate("Alice", "-alice", "").isValid)
        assertFalse(ProfileValidator.validate("Alice", "alice", "x".repeat(501)).isValid)
    }
}
