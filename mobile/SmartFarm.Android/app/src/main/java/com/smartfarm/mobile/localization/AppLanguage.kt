package com.smartfarm.mobile.localization

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import java.util.Locale

object AppLanguage {
    @Volatile private var application: Context? = null
    private var cachedLanguage: String? = null
    private var cachedResources: Resources? = null
    fun initialize(context: Context) { application = context.applicationContext; cachedLanguage = null; Locale.setDefault(Locale.forLanguageTag(current(context))) }
    fun current(context: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (!locales.isEmpty) return if (locales[0].language == "fil") "fil" else "en"
        }
        return context.getSharedPreferences("language", Context.MODE_PRIVATE).getString("selected", "en") ?: "en"
    }
    fun wrap(context: Context): Context {
        Locale.setDefault(Locale.forLanguageTag(current(context)))
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(Locale.forLanguageTag(current(context))))
        return context.createConfigurationContext(config)
    }
    fun select(context: Context, language: String) {
        require(language == "en" || language == "fil")
        context.getSharedPreferences("language", Context.MODE_PRIVATE).edit().putString("selected",language).apply()
        cachedLanguage = null
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(language)
        } else (context as? Activity)?.recreate()
    }
    private data class MessagePattern(val id: Int, val regex: Regex, val indices: List<Int>)
    private var exactMessages: Map<String, Int>? = null
    private var messagePatterns: List<MessagePattern> = emptyList()
    private fun messageResources(language: String): Resources? {
        val context = application ?: return null
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList.forLanguageTags(language))
        return context.createConfigurationContext(configuration).resources
    }
    /** Re-resolve retained system messages after locale changes; record fields never call this. */
    fun message(value: String): String {
        if (application == null) return value
        if (exactMessages == null) {
            val exact = mutableMapOf<String, Int>()
            val patterns = mutableListOf<MessagePattern>()
            val placeholder = Regex("%(\\d+)\\\$s")
            for (language in listOf("en", "fil")) {
                val resources = messageResources(language) ?: continue
                for (id in retainedMessageIds) {
                    val format = resources.getString(id)
                    val matches = placeholder.findAll(format).toList()
                    if (matches.isEmpty()) exact[format] = id else {
                        val pattern = StringBuilder("^"); var end = 0
                        val indices = mutableListOf<Int>()
                        for (match in matches) {
                            pattern.append(Regex.escape(format.substring(end, match.range.first).replace("%%", "%")))
                            pattern.append("(.*?)"); indices.add(match.groupValues[1].toInt() - 1)
                            end = match.range.last + 1
                        }
                        pattern.append(Regex.escape(format.substring(end).replace("%%", "%"))).append("$")
                        patterns.add(MessagePattern(id, Regex(pattern.toString(), RegexOption.DOT_MATCHES_ALL), indices))
                    }
                }
            }
            messagePatterns = patterns; exactMessages = exact
        }
        exactMessages?.get(value)?.let { return text(it, value) }
        for (pattern in messagePatterns) {
            val match = pattern.regex.matchEntire(value) ?: continue
            val args = arrayOfNulls<Any>(pattern.indices.maxOrNull()!! + 1)
            pattern.indices.forEachIndexed { index, argument -> args[argument] = match.groupValues[index + 1] }
            return text(pattern.id, value, *args)
        }
        return value
    }
    fun text(@StringRes id: Int, english: String, vararg args: Any?): String {
        val context = application
        val value = if (context == null) english else {
            val language = current(context)
            if (cachedLanguage != language) {
                cachedResources = wrap(context).resources; cachedLanguage = language
            }
            cachedResources!!.getString(id)
        }
        return if (args.isEmpty()) value else String.format(Locale.ROOT, value, *args)
    }
}
fun uiText(@StringRes id: Int, english: String, vararg args: Any?): String = AppLanguage.text(id, english, *args)

fun systemMessage(value: String): String = AppLanguage.message(value)
