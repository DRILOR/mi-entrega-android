package com.example.mientrega

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Credenciales locales: no son cuentas sincronizadas ni cifran la base Room. */
object AuthRules {
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.ROOT)

    fun registerError(name: String, email: String, password: String, confirm: String): String? = when {
        name.trim().length !in 2..80 -> "El nombre debe tener entre 2 y 80 caracteres."
        email.length > 254 || !emailPattern.matches(normalizeEmail(email)) -> "Escribe un correo válido."
        password.length !in 8..128 -> "La contraseña debe tener entre 8 y 128 caracteres."
        password != confirm -> "Las contraseñas no coinciden."
        else -> null
    }

    fun loginError(email: String, password: String): String? = when {
        email.length > 254 || !emailPattern.matches(normalizeEmail(email)) -> "Escribe un correo válido."
        password.isEmpty() -> "Escribe tu contraseña."
        else -> null
    }
}

data class PasswordDigest(val hash: String, val salt: String)

object PasswordHasher {
    private const val iterations = 210_000
    private const val bits = 256

    fun create(password: String): PasswordDigest {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val derived = derive(password, salt)
        return PasswordDigest(
            hash = Base64.getEncoder().encodeToString(derived),
            salt = Base64.getEncoder().encodeToString(salt)
        )
    }

    fun verify(password: String, digest: PasswordDigest): Boolean = runCatching {
        val salt = Base64.getDecoder().decode(digest.salt)
        val expected = Base64.getDecoder().decode(digest.hash)
        MessageDigest.isEqual(expected, derive(password, salt))
    }.getOrDefault(false)

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, bits)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
