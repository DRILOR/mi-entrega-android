package com.example.mientrega

import org.junit.Assert.*
import org.junit.Test

class AuthSecurityTest {
    @Test fun normalizesEmail() {
        assertEquals("ana@example.com", AuthRules.normalizeEmail(" ANA@Example.com "))
    }

    @Test fun validatesRegistrationAndPasswordConfirmation() {
        assertNull(AuthRules.registerError("Ana Estudiante", "ana@example.com", "clave-fuerte-123", "clave-fuerte-123"))
        assertNotNull(AuthRules.registerError("", "ana@example.com", "clave-fuerte-123", "clave-fuerte-123"))
        assertNotNull(AuthRules.registerError("Ana", "no-es-correo", "clave-fuerte-123", "clave-fuerte-123"))
        assertNotNull(AuthRules.registerError("Ana", "ana@example.com", "1234567", "1234567"))
        assertNotNull(AuthRules.registerError("Ana", "ana@example.com", "clave-fuerte-123", "distinta-123"))
    }

    @Test fun hashesPasswordWithDifferentRandomSalts() {
        val first = PasswordHasher.create("clave-fuerte-123")
        val second = PasswordHasher.create("clave-fuerte-123")
        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.hash, second.hash)
        assertTrue(PasswordHasher.verify("clave-fuerte-123", first))
        assertFalse(PasswordHasher.verify("clave-incorrecta", first))
        assertFalse(PasswordHasher.verify("clave-fuerte-123", PasswordDigest("invalid", "invalid")))
    }
}
