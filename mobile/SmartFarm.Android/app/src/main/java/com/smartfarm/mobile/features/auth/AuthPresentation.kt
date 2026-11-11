package com.smartfarm.mobile.features.auth

/** Light client-side hints only. The server remains authoritative for credentials. */
internal fun looksLikeEmail(input: String): Boolean {
    val text = input.trim()
    val at = text.indexOf('@')
    if (at <= 0 || at == text.lastIndex || text.length > 256 ||
        text.any { it.isWhitespace() }) return false
    val domain = text.substring(at + 1)
    return domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.')
}

internal fun meetsPasswordGuidance(password: String): Boolean =
    password.length in 10..128 &&
        password.any { it.isUpperCase() } &&
        password.any { it.isLowerCase() } &&
        password.any { it.isDigit() }

internal fun registrationReady(name: String, email: String, password: String,
    confirmation: String): Boolean =
    name.trim().length in 2..120 && looksLikeEmail(email) &&
        meetsPasswordGuidance(password) && password == confirmation
