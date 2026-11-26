package com.privatecall.browser.privacy

import android.content.Context

class PrivacyPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("privacy_preferences", Context.MODE_PRIVATE)

    var javascriptEnabled: Boolean
        get() = preferences.getBoolean("javascript", true)
        set(value) = preferences.edit().putBoolean("javascript", value).apply()

    var blockThirdPartyCookies: Boolean
        get() = preferences.getBoolean("third_party_cookies", true)
        set(value) = preferences.edit().putBoolean("third_party_cookies", value).apply()

    var saveHistory: Boolean
        get() = preferences.getBoolean("history", false)
        set(value) = preferences.edit().putBoolean("history", value).apply()

    var darkMode: Boolean
        get() = preferences.getBoolean("dark_mode", true)
        set(value) = preferences.edit().putBoolean("dark_mode", value).apply()

    var searchEngineUrl: String
        get() = preferences.getString("search_engine", "https://duckduckgo.com/?q=")
            ?: "https://duckduckgo.com/?q="
        set(value) = preferences.edit().putString("search_engine", value).apply()

    var downloadsEnabled: Boolean
        get() = preferences.getBoolean("downloads", true)
        set(value) = preferences.edit().putBoolean("downloads", value).apply()

    var voiceModulationPreset: String
        get() = preferences.getString("voice_modulation", "Off") ?: "Off"
        set(value) = preferences.edit().putString("voice_modulation", value).apply()

    var voiceModulationEnabled: Boolean
        get() = preferences.getBoolean(
            "voice_modulation_enabled",
            voiceModulationPreset != "Off" || voiceSampleName.isNotBlank(),
        )
        set(value) = preferences.edit().putBoolean("voice_modulation_enabled", value).apply()

    var voiceSampleName: String
        get() = preferences.getString("voice_sample_name", "") ?: ""
        set(value) = preferences.edit().putString("voice_sample_name", value).apply()

    var voiceSampleBrightnessDb: Float
        get() = preferences.getFloat("voice_sample_brightness_db", 0f)
        set(value) = preferences.edit()
            .putFloat("voice_sample_brightness_db", value.coerceIn(-6f, 6f))
            .apply()

    var voiceTestPreset: String
        get() = preferences.getString("voice_test_preset", "Off") ?: "Off"
        set(value) = preferences.edit().putString("voice_test_preset", value).apply()

    var voiceTestEffectsEnabled: Boolean
        get() = preferences.getBoolean("voice_test_effects_enabled", true)
        set(value) = preferences.edit().putBoolean("voice_test_effects_enabled", value).apply()

    var voiceTestNoiseSuppressionEnabled: Boolean
        get() = preferences.getBoolean("voice_test_noise_suppression_enabled", true)
        set(value) = preferences.edit().putBoolean("voice_test_noise_suppression_enabled", value).apply()

    var voiceTestEchoCancellationEnabled: Boolean
        get() = preferences.getBoolean("voice_test_echo_cancellation_enabled", true)
        set(value) = preferences.edit().putBoolean("voice_test_echo_cancellation_enabled", value).apply()

    var voiceTestAutomaticGainControlEnabled: Boolean
        get() = preferences.getBoolean("voice_test_automatic_gain_control_enabled", true)
        set(value) = preferences.edit().putBoolean("voice_test_automatic_gain_control_enabled", value).apply()

    fun voicePresetName(presetId: String): String {
        return preferences.getString("voice_name_$presetId", defaultVoicePresetName(presetId))
            ?: defaultVoicePresetName(presetId)
    }

    fun setVoicePresetName(presetId: String, name: String) {
        val cleanName = name.trim().take(40)
        preferences.edit()
            .putString("voice_name_$presetId", cleanName.ifBlank { defaultVoicePresetName(presetId) })
            .apply()
    }

    companion object {
        val voicePresetIds = listOf(
            "male-bright", "female-bright",
            "male-warm", "female-warm",
            "male-deep", "female-deep",
            "male-radio", "female-radio",
            "male-cinematic", "female-cinematic",
            "male-podcast", "female-podcast",
            "male-bass", "female-bass",
            "male-crisp", "female-crisp",
            "male-telephone", "female-telephone",
            "male-natural", "female-natural",
            "effect-robot", "effect-distorted", "effect-echo",
        )

        val voiceTypes = listOf(
            "Bright", "Warm", "Deep", "Radio", "Cinematic",
            "Podcast", "Bass", "Crisp", "Telephone", "Natural",
        )

        fun defaultVoicePresetName(presetId: String): String {
            if (presetId.startsWith("effect-")) {
                return when (presetId.removePrefix("effect-")) {
                    "robot" -> "Robotic"
                    "distorted" -> "Distorted"
                    "echo" -> "Echo"
                    else -> "Audio effect"
                }
            }
            val gender = if (presetId.startsWith("male-")) "Male" else "Female"
            val type = presetId.substringAfter('-', "").replaceFirstChar { it.uppercase() }
            return "$gender $type"
        }
    }
}
