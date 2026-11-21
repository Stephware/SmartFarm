package com.smartfarm.mobile
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.data.repository.NetworkErrorMessages
import com.smartfarm.mobile.localization.AppLanguage
import com.smartfarm.mobile.localization.systemMessage
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RetainedMessageLocaleTest {
    @Test fun retainedHttpLoginFailureResolvesInTheCurrentLanguage() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        fun select(language: String) = instrumentation.runOnMainSync { AppLanguage.select(context, language) }
        try {
            select("en")
            val failure = NetworkErrorMessages.forLogin(HttpException(Response.error<Unit>(401, "{}".toResponseBody())))
            assertEquals("Incorrect email or password.", failure)
            select("fil")
            assertEquals("Mali ang email o password.", systemMessage(failure))
            val translated = systemMessage(failure)
            select("en")
            assertEquals("Incorrect email or password.", systemMessage(translated))
        } finally { select("en") }
    }
}
