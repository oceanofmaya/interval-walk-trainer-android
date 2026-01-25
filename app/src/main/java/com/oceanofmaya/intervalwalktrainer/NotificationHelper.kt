package com.oceanofmaya.intervalwalktrainer

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

import android.media.AudioAttributes

/**
 * Handles notification mechanisms for phase changes: vibration patterns and text-to-speech.
 * 
 * Manages TTS lifecycle and provides distinct vibration patterns for different phases:
 * - Slow phase: Gentle, low-intensity vibration
 * - Fast phase: Strong double-pulse vibration pattern
 * - Completed: Celebration pattern with three pulses
 * 
 * TTS initialization is asynchronous and includes robust error handling with language fallback.
 * Messages are queued if TTS is not yet ready and will be spoken when initialization completes.
 * 
 * @param context Android context for accessing system services
 */
open class NotificationHelper(private val context: Context) {
    private val vibrator: Vibrator? by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var isInitializing = false
    private val pendingSpeech = mutableListOf<String>()
    
    companion object {
        private const val TAG = "NotificationHelper"
    }

    init {
        // Initialize TTS early to avoid lag on first use
        initializeTts()
    }

    private fun initializeTts() {
        if (isInitializing || (isTtsReady && textToSpeech != null)) {
            return
        }
        isInitializing = true
        
        try {
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
            isInitializing = false
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { tts ->
                    try {
                        // Try to set language to device default
                        var langResult = tts.setLanguage(Locale.getDefault())
                        
                        if (langResult == TextToSpeech.LANG_MISSING_DATA || 
                            langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                            // Fallback to US English
                            Log.d(TAG, "Default language not supported, using English")
                            langResult = tts.setLanguage(Locale.US)
                        }
                        
                        if (langResult == TextToSpeech.LANG_MISSING_DATA || 
                            langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                            // Fallback to any English variant
                            Log.d(TAG, "US English not supported, trying generic English")
                            langResult = tts.setLanguage(Locale.ENGLISH)
                        }
                        
                        if (langResult != TextToSpeech.LANG_MISSING_DATA && 
                            langResult != TextToSpeech.LANG_NOT_SUPPORTED) {
                            // Configure TTS for optimal performance
                            tts.setSpeechRate(1.0f) // Normal speed
                            tts.setPitch(1.0f) // Normal pitch
                            
                            // Set AudioAttributes for spoken guidance during workouts
                            // USAGE_ASSISTANCE_NAVIGATION_GUIDANCE ensures announcements are heard
                            // similar to navigation apps giving turn-by-turn directions
                            val attributes = AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                            tts.setAudioAttributes(attributes)
                            
                            // Set up listener for debugging
                            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                override fun onStart(utteranceId: String?) {
                                    Log.d(TAG, "TTS started: $utteranceId")
                                }
                                
                                override fun onDone(utteranceId: String?) {
                                    Log.d(TAG, "TTS completed: $utteranceId")
                                }
                                
                                // Required by base class - delegates to new API
                                @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                                override fun onError(utteranceId: String?) {
                                    // Delegate to the new non-deprecated method with generic error code
                                    onError(utteranceId, -1)
                                }
                                
                                override fun onError(utteranceId: String?, errorCode: Int) {
                                    Log.e(TAG, "TTS error: $utteranceId, errorCode: $errorCode")
                                }
                            })
                            
                            isTtsReady = true
                            Log.d(TAG, "TTS initialized successfully")
                            
                            // Speak any pending messages
                            if (pendingSpeech.isNotEmpty()) {
                                Log.d(TAG, "Speaking ${pendingSpeech.size} pending messages")
                                pendingSpeech.forEach { text ->
                                    speakNow(text)
                                }
                                pendingSpeech.clear()
                            }
                        } else {
                            Log.e(TAG, "No supported language found for TTS")
                            isTtsReady = false
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error configuring TTS", e)
                        isTtsReady = false
                    }
                } ?: run {
                    Log.e(TAG, "TTS initialization callback with null engine")
                    isTtsReady = false
                }
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
                isTtsReady = false
                // Reset textToSpeech to null so we can try again later
                textToSpeech = null
            }
        }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating TextToSpeech", e)
            isInitializing = false
            isTtsReady = false
            textToSpeech = null
        }
    }

    open fun vibrate(durationMillis: Long = 200, amplitude: Int = -1) {
        vibrator?.let { v ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val effectAmplitude = if (amplitude == -1) VibrationEffect.DEFAULT_AMPLITUDE else amplitude
                v.vibrate(VibrationEffect.createOneShot(durationMillis, effectAmplitude))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMillis)
            }
        }
    }


    open fun notifyPhaseChange(phase: IntervalPhase, useVoice: Boolean, useVibration: Boolean) {
        
        // 1. Queue voice message IMMEDIATELY
        if (useVoice) {
            val message = when (phase) {
                is IntervalPhase.Slow -> "Slow walk"
                is IntervalPhase.Fast -> "Fast walk"
                is IntervalPhase.Completed -> "Workout complete"
            }
            speak(message)
        }
        
        // 2. Handle vibration
        // For Completed phase: execute immediately since wake lock will be released right after
        // For other phases: delay slightly to avoid interfering with TTS audio focus acquisition
        if (useVibration) {
            val vibrationRunnable = Runnable {
                try {
                    executeVibration(phase)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during vibration", e)
                }
            }
            
            if (phase is IntervalPhase.Completed) {
                // Execute immediately for completion - wake lock releases right after
                vibrationRunnable.run()
            } else {
                // Delay for other phases to let TTS acquire audio focus first
                Handler(Looper.getMainLooper()).postDelayed(vibrationRunnable, 300)
            }
        }
    }
    
    /**
     * Executes the appropriate vibration pattern for the given phase.
     */
    private fun executeVibration(phase: IntervalPhase) {
        when (phase) {
            is IntervalPhase.Slow -> {
                // Gentle single vibration for slow phase
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // Use moderate amplitude (about 30% of max 255)
                    vibrate(150, 75)
                } else {
                    @Suppress("DEPRECATION")
                    vibrate(150)
                }
            }
            is IntervalPhase.Fast -> {
                // Strong double-pulse vibration for fast phase
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 200, 100, 200)
                    // Use strong amplitude (about 80% of max 255)
                    val strongAmp = 200
                    val amplitudes = intArrayOf(0, strongAmp, 0, strongAmp)
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrate(400)
                }
            }
            is IntervalPhase.Completed -> {
                // Celebration pattern with three pulses for completion
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 200, 150, 200, 150, 200)
                    // Use full amplitude for celebration
                    val fullAmp = 255
                    val amplitudes = intArrayOf(0, fullAmp, 0, fullAmp, 0, fullAmp)
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                } else {
                    // For older APIs, use a longer single vibration
                    @Suppress("DEPRECATION")
                    vibrate(600)
                }
            }
        }
    }

    /**
     * Speaks the given text using TTS if available.
     * Queues the message if TTS is not yet ready, and will speak it when initialization completes.
     * Public method accessible from MainActivity for feedback messages.
     */
    open fun speak(text: String) {
        if (isTtsReady && textToSpeech != null) {
            speakNow(text)
        } else {
            // TTS not ready yet, queue the message
            if (!pendingSpeech.contains(text)) {
                pendingSpeech.add(text)
            }
            
            // Re-initialize if TTS was never created
            if (textToSpeech == null) {
                initializeTts()
            }
        }
    }
    
    /**
     * Immediately speaks the given text, stopping any ongoing speech.
     */
    private fun speakNow(text: String) {
        try {
            textToSpeech?.let { tts ->
                // Stop any ongoing speech for immediate response
                tts.stop()
                
                // Use the modern API with Bundle instead of deprecated HashMap
                val utteranceId = "phase_change_${System.currentTimeMillis()}"
                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }
                
                // Use QUEUE_FLUSH to ensure this message plays immediately, interrupting anything else
                val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                
                if (result == TextToSpeech.ERROR) {
                    Log.e(TAG, "TTS speak() returned ERROR")
                    // Try to recover by reinitializing
                    isTtsReady = false
                    tts.shutdown()
                    textToSpeech = null
                    initializeTts()
                    if (!pendingSpeech.contains(text)) {
                        pendingSpeech.add(text)
                    }
                }
            } ?: run {
                Log.e(TAG, "TTS is null when trying to speak")
                pendingSpeech.add(text)
                initializeTts()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while speaking", e)
            // Try to recover
            isTtsReady = false
            textToSpeech?.shutdown()
            textToSpeech = null
            initializeTts()
            if (!pendingSpeech.contains(text)) {
                pendingSpeech.add(text)
            }
        }
    }

    /**
     * Announces the start of the workout with the initial phase.
     */
    open fun announceStart(startingPhase: IntervalPhase) {
        val message = when (startingPhase) {
            is IntervalPhase.Slow -> "Starting workout. Slow walk"
            is IntervalPhase.Fast -> "Starting workout. Fast walk"
            is IntervalPhase.Completed -> "Workout complete"
        }
        speak(message)
    }
    
    /**
     * Tests TTS by speaking a sample message.
     * Useful for verifying TTS is working when user enables voice notifications.
     */
    open fun testTts() {
        speak("Voice notifications enabled")
    }
    
    /**
     * Returns whether TTS is currently ready to speak.
     */
    fun isTtsReady(): Boolean = isTtsReady
    
    /**
     * Releases all resources. Call this when the helper is no longer needed.
     */
    open fun release() {
        isInitializing = false
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isTtsReady = false
        pendingSpeech.clear()
    }
}

