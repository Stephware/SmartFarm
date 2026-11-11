package com.smartfarm.mobile

import com.smartfarm.mobile.features.auth.looksLikeEmail
import com.smartfarm.mobile.features.auth.meetsPasswordGuidance
import com.smartfarm.mobile.features.auth.registrationReady
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthPresentationTest {
    @Test fun validatesEmailWithWhitespaceTrim() {
        assertTrue(looksLikeEmail(" hello@example.com "))
        assertFalse(looksLikeEmail("someoneexample.com"))
        assertFalse(looksLikeEmail("one @example.com"))
        assertFalse(looksLikeEmail("x@invalid"))
    }

    @Test fun registrationNeedsMatchingAndWellFormedPasswords() {
        assertTrue(registrationReady("Farm Manager", "hello@example.com", "StrongPass123", "StrongPass123"))
        assertFalse(registrationReady("F", "hello@example.com", "StrongPass123", "StrongPass123"))
        assertFalse(registrationReady("Farm Manager", "hello@example.com", "strongpass123", "strongpass123"))
        assertFalse(registrationReady("Farm Manager", "hello@example.com", "StrongPass123", "StrongPass124"))
    }

    @Test fun passwordLengthAndCaseRequired() {
        assertFalse(meetsPasswordGuidance("Short1"))
        assertFalse(meetsPasswordGuidance("verylongpassword1"))
        assertTrue(meetsPasswordGuidance("FarmPassword123"))
    }
}
