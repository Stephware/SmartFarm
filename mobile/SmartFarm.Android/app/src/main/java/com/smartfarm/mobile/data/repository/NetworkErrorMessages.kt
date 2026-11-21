package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import java.io.IOException
import retrofit2.HttpException

/**
 * Short, actionable messages for common network failures.
 * Never display raw server responses, URLs, or credentials.
 */
internal object NetworkErrorMessages {
    fun forRequest(error: Throwable, fallback: String): String = when (error) {
        is HttpException -> when (error.code()) {
            400, 422 -> uiText(R.string.ui_please_check_the_entered_details_8e67710, "Please check the entered details.")
            401 -> uiText(R.string.ui_your_session_expired_sign_out_and_sign_in_again_8275acc, "Your session expired. Sign out and sign in again.")
            403 -> uiText(R.string.ui_you_do_not_have_permission_to_do_that_5d6c52b, "You do not have permission to do that.")
            404 -> uiText(R.string.ui_this_record_is_no_longer_available_refresh_the_page_d61608d, "This record is no longer available. Refresh the page.")
            409, 412 -> uiText(R.string.ui_this_record_changed_refresh_before_trying_again_a0eb8f1, "This record changed. Refresh before trying again.")
            429 -> uiText(R.string.ui_too_many_requests_please_wait_and_try_again_02f43f1, "Too many requests. Please wait and try again.")
            in 500..599 -> uiText(R.string.ui_the_server_is_unavailable_try_again_later_4023e15, "The server is unavailable. Try again later.")
            else -> fallback
        }
        is IOException -> uiText(R.string.ui_could_not_connect_check_your_internet_connection_and_42632e0, "Could not connect. Check your internet connection and try again.")
        is IllegalStateException ->
            if (error.message?.startsWith("Session changed") == true ||
                error.message == "Please sign in.") {
                uiText(R.string.ui_your_session_changed_sign_in_again_a316954, "Your session changed. Sign in again.")
            } else fallback
        else -> fallback
    }

    // Non-idempotent writes may have succeeded even if their response was lost.
    fun forMutation(error: Throwable, fallback: String): String = when {
        error is IOException || (error is HttpException && error.code() >= 500) ->
            uiText(R.string.ui_could_not_confirm_the_change_refresh_the_records_bef_890bc91, "Could not confirm the change. Refresh the records before trying again.")
        else -> forRequest(error, fallback)
    }

    fun forLogin(error: Throwable): String = when ((error as? HttpException)?.code()) {
        400, 401 -> uiText(R.string.ui_incorrect_email_or_password_6f8e6be, "Incorrect email or password.")
        else -> forRequest(error, uiText(R.string.ui_sign_in_failed_check_your_credentials_and_connection_425ff86, "Sign in failed. Check your credentials and connection."))
    }

    fun forRegistration(error: Throwable): String = when ((error as? HttpException)?.code()) {
        400, 422 -> uiText(R.string.ui_check_your_account_details_and_password_requirements_06aaab9, "Check your account details and password requirements.")
        409 -> uiText(R.string.ui_this_email_may_already_be_registered_try_signing_in_14ac209, "This email may already be registered. Try signing in.")
        else -> forRequest(error, uiText(R.string.ui_registration_failed_check_your_details_and_connectio_5eb5fd5, "Registration failed. Check your details and connection."))
    }
}
