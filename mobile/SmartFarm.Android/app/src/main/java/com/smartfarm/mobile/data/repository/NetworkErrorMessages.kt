package com.smartfarm.mobile.data.repository

import java.io.IOException
import retrofit2.HttpException

/**
 * Short, actionable messages for common network failures.
 * Never display raw server responses, URLs, or credentials.
 */
internal object NetworkErrorMessages {
    fun forRequest(error: Throwable, fallback: String): String = when (error) {
        is HttpException -> when (error.code()) {
            400, 422 -> "Please check the entered details."
            401 -> "Your session expired. Sign out and sign in again."
            403 -> "You do not have permission to do that."
            404 -> "This record is no longer available. Refresh the page."
            409, 412 -> "This record changed. Refresh before trying again."
            429 -> "Too many requests. Please wait and try again."
            in 500..599 -> "The server is unavailable. Try again later."
            else -> fallback
        }
        is IOException -> "Could not connect. Check your internet connection and try again."
        is IllegalStateException ->
            if (error.message?.startsWith("Session changed") == true ||
                error.message == "Please sign in.") {
                "Your session changed. Sign in again."
            } else fallback
        else -> fallback
    }

    fun forLogin(error: Throwable): String = when ((error as? HttpException)?.code()) {
        400, 401 -> "Incorrect email or password."
        else -> forRequest(error, "Sign in failed. Check your credentials and connection.")
    }

    fun forRegistration(error: Throwable): String = when ((error as? HttpException)?.code()) {
        400, 422 -> "Check your account details and password requirements."
        409 -> "This email may already be registered. Try signing in."
        else -> forRequest(error, "Registration failed. Check your details and connection.")
    }
}
