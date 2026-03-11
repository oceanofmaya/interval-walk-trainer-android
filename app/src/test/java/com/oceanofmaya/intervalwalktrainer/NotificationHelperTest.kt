package com.oceanofmaya.intervalwalktrainer

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Locale

/**
 * Unit tests for NotificationHelper.
 * 
 * Note: NotificationHelper is tightly coupled to Android APIs (TTS, Vibrator).
 * These tests focus on verifying the public API behavior and state management
 * that can be tested without complex Android mocking. The tests verify that
 * methods can be called without throwing exceptions, which is important for
 * robustness. For full integration testing, consider using Robolectric or
 * instrumented tests.
 */
class NotificationHelperTest {

    private lateinit var context: Context

    @BeforeEach
    fun setUp() {
        context = mock(Context::class.java)
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mock(AudioManager::class.java))
    }

    @Test
    fun `isTtsReady returns false initially`() {
        val helper = NotificationHelper(context)
        
        // TTS initialization is asynchronous, so it should be false initially
        assertFalse(helper.isTtsReady())
        
        helper.release()
    }

    @Test
    fun `release can be called multiple times safely`() {
        val helper = NotificationHelper(context)
        
        // Should not throw
        helper.release()
        helper.release()
        helper.release()
    }

    @Test
    fun `speak can be called without throwing`() {
        val helper = NotificationHelper(context)
        
        // Should queue the message if TTS is not ready
        helper.speak("Test message")
        
        helper.release()
    }

    @Test
    fun `notifyPhaseChange can be called for all phases`() {
        val helper = NotificationHelper(context)
        
        // Should not throw for any phase combination
        helper.notifyPhaseChange(IntervalPhase.Slow, useVoice = false, useVibration = false)
        helper.notifyPhaseChange(IntervalPhase.Fast, useVoice = false, useVibration = false)
        helper.notifyPhaseChange(IntervalPhase.Completed, useVoice = false, useVibration = false)
        
        helper.release()
    }

    @Test
    fun `notifyPhaseChange supports vibration when enabled`() {
        val helper = NotificationHelper(context)

        // Should not throw when vibration is enabled
        helper.notifyPhaseChange(IntervalPhase.Slow, useVoice = false, useVibration = true)
        helper.notifyPhaseChange(IntervalPhase.Fast, useVoice = false, useVibration = true)
        helper.notifyPhaseChange(IntervalPhase.Completed, useVoice = false, useVibration = true)

        helper.release()
    }

    @Test
    fun `notifyPhaseChange supports voice and vibration together`() {
        val helper = NotificationHelper(context)

        // Should not throw when both voice and vibration are enabled
        helper.notifyPhaseChange(IntervalPhase.Completed, useVoice = true, useVibration = true)

        helper.release()
    }

    @Test
    fun `testTts can be called without throwing`() {
        val helper = NotificationHelper(context)
        
        helper.testTts()
        
        helper.release()
    }

    @Test
    fun `constructor with prefs and voice key does not throw`() {
        val prefs = mock(SharedPreferences::class.java)
        val helper = NotificationHelper(context, prefs, "tts_voice")
        assertFalse(helper.isTtsReady())
        helper.release()
    }

    @Test
    fun `normalizeLocaleToSupported maps region variant to supported language`() {
        val helper = NotificationHelper(context)

        val normalized = helper.normalizeLocaleToSupported(Locale.forLanguageTag("es-MX"))

        assertEquals("es", normalized?.toLanguageTag())
        helper.release()
    }

    @Test
    fun `resolveSpeechLocale prefers stored locale over active voice locale`() {
        val prefs = mock(SharedPreferences::class.java)
        `when`(prefs.getString("tts_voice_locale", null)).thenReturn("fr-CA")
        val helper = NotificationHelper(context, prefs, "tts_voice", "tts_voice_locale")

        val resolved = helper.resolveSpeechLocale(Locale.forLanguageTag("en-US"))

        assertEquals("fr", resolved.toLanguageTag())
        helper.release()
    }

    @Test
    fun `resolveSpeechLocale falls back to english when unsupported`() {
        val helper = NotificationHelper(context)

        val resolved = helper.resolveSpeechLocale(Locale.forLanguageTag("zz-ZZ"))

        assertEquals("en", resolved.toLanguageTag())
        helper.release()
    }
}

