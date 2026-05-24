package com.oceanofmaya.intervalwalktrainer

import android.annotation.SuppressLint
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import java.util.Locale
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.oceanofmaya.intervalwalktrainer.databinding.ActivityMainBinding
import com.oceanofmaya.intervalwalktrainer.home.HomeInsightRegistry
import com.oceanofmaya.intervalwalktrainer.home.HomeInsightsController
import com.oceanofmaya.intervalwalktrainer.home.InsightCardsEditor
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
/**
 * Main activity for the Interval Walk Trainer app.
 * 
 * Manages the interval training timer, UI state, and user preferences.
 * Supports background operation with wake locks and state restoration.
 * 
 * Features:
 * - Multiple training formulas with customizable durations
 * - Vibration and voice notifications for phase changes
 * - Dark/light theme support with persistence
 * - State preservation across configuration changes
 */
open class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var intervalTimer: IntervalTimer? = null
    private var notificationHelper: NotificationHelper? = null
    private var currentFormula: IntervalFormula = IntervalFormulas.default
    private lateinit var sharedPreferences: SharedPreferences
    private var wakeLock: PowerManager.WakeLock? = null
    private var timerJob: Job? = null
    private var isRestoringTimerState = false
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var savedWorkoutRepository: SavedWorkoutRepository
    private var lastDisplayedTime = -1
    private var lastDisplayedPhase: IntervalPhase? = null
    private var hasShownCompletionConfetti = false
    private var preStartCountdownTimer: CountDownTimer? = null
    private var isPreStartCountdownActive = false
    private var preStartCountdownEndElapsedRealtime: Long = 0L
    private var shouldStartForegroundServiceAfterPermission = false
    private var settingsNotificationsSwitch: com.google.android.material.switchmaterial.SwitchMaterial? = null
    private var isUpdatingNotificationsSwitch = false
    private var lastKnownNotificationsEnabled = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var completionAtMillis: Long? = null
    private var completionAutoResetRunnable: Runnable? = null
    private lateinit var homeInsightsController: HomeInsightsController
    private lateinit var homeInsightRegistry: HomeInsightRegistry

    private val homeWorkoutSetup get() = binding.homeWorkoutSetup
    private val homeSession get() = binding.homeSessionPanel.homeSession
    private val homeActions get() = binding.homeSessionPanel.homeActions
    private val phaseLabel get() = homeSession.phaseLabel

    companion object {
        // FAQ order: basics → how to use → workout flow → data & history → technical & permissions → safety
        private val faqEntries = listOf(
            FaqEntry(R.string.faq_question_interval_walking, R.string.faq_answer_interval_walking),
            FaqEntry(R.string.faq_question_why_interval_walking, R.string.faq_answer_why_interval_walking),
            FaqEntry(R.string.faq_question_slow_fast_mean, R.string.faq_answer_slow_fast_mean),
            FaqEntry(R.string.faq_question_choose_formula, R.string.faq_answer_choose_formula),
            FaqEntry(R.string.faq_question_interval_vs_circuit, R.string.faq_answer_interval_vs_circuit),
            FaqEntry(R.string.faq_question_custom_formula_saved, R.string.faq_answer_custom_formula_saved),
            FaqEntry(R.string.faq_question_countdown, R.string.faq_answer_countdown),
            FaqEntry(R.string.faq_question_pause_reset, R.string.faq_answer_pause_reset),
            FaqEntry(R.string.faq_question_workout_history, R.string.faq_answer_workout_history),
            FaqEntry(R.string.faq_question_weekly_goals, R.string.faq_answer_weekly_goals),
            FaqEntry(R.string.faq_question_insight_cards, R.string.faq_answer_insight_cards),
            FaqEntry(R.string.faq_question_data_shared, R.string.faq_answer_data_shared),
            FaqEntry(R.string.faq_question_notifications, R.string.faq_answer_notifications),
            FaqEntry(R.string.faq_question_voice, R.string.faq_answer_voice),
            FaqEntry(R.string.faq_question_voice_languages, R.string.faq_answer_voice_languages),
            FaqEntry(R.string.faq_question_background, R.string.faq_answer_background),
            FaqEntry(
                R.string.faq_question_physical_activity_permission,
                R.string.faq_answer_physical_activity_permission
            ),
            FaqEntry(
                R.string.faq_question_workout_stops_background,
                R.string.faq_answer_workout_stops_background
            ),
            FaqEntry(R.string.faq_question_keep_screen_awake, R.string.faq_answer_keep_screen_awake),
            FaqEntry(R.string.faq_question_safe, R.string.faq_answer_safe)
        )

        // SharedPreferences keys
        private const val PREFS_NAME = "interval_walk_trainer_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT_STYLE = "accent_style"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_TTS_VOICE = "tts_voice"
        private const val KEY_TTS_VOICE_LOCALE = "tts_voice_locale"
        private const val KEY_TTS_VOICE_DISPLAY = "tts_voice_display"
        private const val KEY_SAVE_WORKOUTS = "save_workouts"
        private const val KEY_KEEP_SCREEN_AWAKE = "keep_screen_awake"
        private const val KEY_START_COUNTDOWN = "start_countdown"
        private const val KEY_START_COUNTDOWN_SECONDS = "start_countdown_seconds"
        private const val KEY_CUSTOM_SLOW_MINUTES = "custom_slow_minutes"
        private const val KEY_CUSTOM_FAST_MINUTES = "custom_fast_minutes"
        private const val KEY_CUSTOM_ROUNDS = "custom_rounds"
        private const val KEY_CUSTOM_STARTS_WITH_FAST = "custom_starts_with_fast"
        private const val KEY_CUSTOM_IS_CIRCUIT = "custom_is_circuit"
        private const val KEY_CUSTOM_CIRCUIT_PATTERN = "custom_circuit_pattern" // "fast_slow_fast" or "slow_fast_slow"
        private const val KEY_IS_CUSTOM_FORMULA = "is_custom_formula"
        private const val KEY_CUSTOM_FORMULA_DISPLAY_NAME = "custom_formula_display_name"
        private const val KEY_ACTIVE_SAVED_WORKOUT_ID = "active_saved_workout_id"
        private const val KEY_CUSTOM_FORMULA_MODE = "custom_formula_mode" // "circuit" or "interval"
        private const val KEY_LEGACY_SAVED_MIGRATED = "saved_workouts_legacy_migrated_v3"
        
        // Saved state keys
        private const val KEY_SAVED_FORMULA_NAME = "saved_formula_name"
        private const val KEY_SAVED_TIME_REMAINING = "saved_time_remaining"
        private const val KEY_SAVED_CURRENT_INTERVAL = "saved_current_interval"
        private const val KEY_SAVED_IS_RUNNING = "saved_is_running"
        private const val KEY_SAVED_PHASE = "saved_phase"
        private const val KEY_SAVED_ELAPSED_SECONDS = "saved_elapsed_seconds"
        private const val KEY_SAVED_COMPLETION_AT_MILLIS = "saved_completion_at_millis"
        private const val KEY_PRE_START_ACTIVE = "pre_start_countdown_active"
        private const val KEY_PRE_START_END_ELAPSED_REALTIME = "pre_start_countdown_end_elapsed_realtime"
        private const val PRE_START_RESUME_THRESHOLD_MS = 250L
        private const val PORTRAIT_LIST_MAX_FRACTION = 0.65f
        private const val LANDSCAPE_LIST_MAX_FRACTION = 0.55f

        // 5 seconds matches the pattern used by Gmail / Keep / Files for undo-able deletes —
        // long enough to read the row name and tap UNDO, short enough to not linger.
        private const val UNDO_SNACKBAR_DURATION_MS = 5000
        
        // Wake lock configuration
        private const val WAKE_LOCK_TAG = "IntervalWalkTrainer:TimerWakeLock"
        private const val WAKE_LOCK_TIMEOUT_HOURS = 10L
        private const val WAKE_LOCK_TIMEOUT_MS = WAKE_LOCK_TIMEOUT_HOURS * 60 * 60 * 1000L
        private const val PRE_START_SECONDS_DEFAULT = 3
        private const val PRE_START_SECONDS_MIN = 1
        private const val PRE_START_SECONDS_MAX = 10
        private const val AUTO_RESET_AFTER_COMPLETION_DELAY_MS = 15_000L
        private const val ACCENT_BLUE = "blue"
        private const val ACCENT_TEAL = "teal"
        private const val ACCENT_PURPLE = "purple"
        private const val ACCENT_AMBER = "amber"
        private const val ACCENT_MAGENTA = "magenta"
        private const val REQUEST_CODE_ACTIVITY_RECOGNITION = 1001
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1002
        private val SUPPORTED_TTS_LOCALE_TAGS = setOf(
            "ar",
            "da",
            "de",
            "en",
            "es",
            "fil",
            "fr",
            "hi",
            "id",
            "it",
            "ja",
            "kn",
            "ko",
            "ml",
            "nl",
            "pl",
            "pt",
            "pt-PT",
            "ru",
            "sv",
            "ta",
            "te",
            "th",
            "tl",
            "tr",
            "ur",
            "vi",
            "zh-CN",
            "zh-HK"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load and apply theme preference before setting content view
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        applyThemePreference()

        // Install splash screen (must be called before super.onCreate)
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Configure edge-to-edge and apply safe area insets
        setupEdgeToEdge()

        // Allow dependency injection for testing, otherwise create default instance
        if (notificationHelper == null) {
            notificationHelper = createNotificationHelper()
        }
        
        // Initialize workout repository
        val database = AppDatabase.getDatabase(this)
        workoutRepository = WorkoutRepository(database.workoutDao(), database.workoutSessionDao(), database)
        savedWorkoutRepository = SavedWorkoutRepository(database.savedWorkoutDao(), database)
        lifecycleScope.launch(Dispatchers.IO) {
            maybeMigrateLegacySavedWorkouts()
        }

        restoreCustomFormula()
        setupFormulaSpinner()
        setupControls()
        applyAccentStyling()
        setupOverflowMenuButton()
        setupHomeInsights()
        lifecycleScope.launch(Dispatchers.IO) {
            WeeklyReminderScheduler(this@MainActivity).scheduleNextReminder()
        }
        lastKnownNotificationsEnabled = areAppNotificationsEnabled()
        
        // Restore timer state if activity was recreated (e.g., theme change)
        if (savedInstanceState != null) {
            restoreTimerState(savedInstanceState)
            restorePreStartCountdownIfActive(savedInstanceState)
        } else {
            updateUI()
        }
    }

    private fun restorePreStartCountdownIfActive(savedInstanceState: Bundle) {
        if (!savedInstanceState.getBoolean(KEY_PRE_START_ACTIVE, false)) return
        val endRealtime = savedInstanceState.getLong(KEY_PRE_START_END_ELAPSED_REALTIME, 0L)
        val remainingMillis = endRealtime - SystemClock.elapsedRealtime()
        if (intervalTimer == null) {
            intervalTimer = createIntervalTimer()
            observeTimerState()
        }
        if (remainingMillis > PRE_START_RESUME_THRESHOLD_MS) {
            startPreStartCountdown(initialMillis = remainingMillis)
        } else {
            // Countdown elapsed during recreate - go straight to running.
            startTimerNow()
        }
    }

    override fun onResume() {
        super.onResume()
        applyKeepScreenAwakePreference()
        maybeAutoResetCompletedTimer()
        val notificationsEnabled = areAppNotificationsEnabled()
        handleNotificationsEnabledTransition(lastKnownNotificationsEnabled, notificationsEnabled)
        lastKnownNotificationsEnabled = notificationsEnabled
        refreshNotificationsSwitchState()
        homeInsightsController.load()
        lifecycleScope.launch(Dispatchers.IO) {
            WeeklyReminderScheduler(this@MainActivity).scheduleNextReminder()
        }
    }

    override fun onPause() {
        super.onPause()
        // Ensure this preference is inert once the app leaves the foreground.
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    
    /**
     * Sets up window insets to handle safe areas for edge-to-edge screens.
     * Applies system bar insets to the scroll area.
     */
    @SuppressLint("InlinedApi")
    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        val isDarkTheme = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainScrollView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = insets.left,
                top = insets.top,
                right = insets.right,
                bottom = insets.bottom
            )
            windowInsets
        }
    }
    
    /**
     * Performs haptic feedback for button taps.
     * Uses KEYBOARD_TAP for a subtle, consistent tap feedback.
     */
    private fun performHapticFeedback(view: View, feedbackType: Int = HapticFeedbackConstants.KEYBOARD_TAP) {
        view.performHapticFeedback(feedbackType)
    }

    private fun hapticSelection(view: View) {
        performHapticFeedback(view, HapticFeedbackConstants.VIRTUAL_KEY)
    }

    @SuppressLint("InlinedApi")
    private fun hapticSuccess(view: View) {
        performHapticFeedback(view, HapticFeedbackConstants.CONFIRM)
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save timer state to preserve it across theme changes
        val timerState = intervalTimer?.state?.value
        if (timerState != null) {
            outState.putString(KEY_SAVED_FORMULA_NAME, currentFormula.name)
            outState.putInt(KEY_SAVED_TIME_REMAINING, timerState.timeRemainingSeconds)
            outState.putInt(KEY_SAVED_CURRENT_INTERVAL, timerState.currentInterval)
            outState.putBoolean(KEY_SAVED_IS_RUNNING, timerState.isRunning)
            outState.putInt(KEY_SAVED_ELAPSED_SECONDS, timerState.elapsedSeconds)
            outState.putString(KEY_SAVED_PHASE, when (timerState.currentPhase) {
                is IntervalPhase.Slow -> "slow"
                is IntervalPhase.Fast -> "fast"
                is IntervalPhase.Completed -> "completed"
            })
            outState.putLong(KEY_SAVED_COMPLETION_AT_MILLIS, completionAtMillis ?: -1L)
        }
        if (isPreStartCountdownActive) {
            outState.putBoolean(KEY_PRE_START_ACTIVE, true)
            outState.putLong(KEY_PRE_START_END_ELAPSED_REALTIME, preStartCountdownEndElapsedRealtime)
        }
    }
    
    private fun restoreTimerState(savedInstanceState: Bundle) {
        val savedFormulaName = savedInstanceState.getString(KEY_SAVED_FORMULA_NAME)
        val savedTimeRemaining = savedInstanceState.getInt(KEY_SAVED_TIME_REMAINING, -1)
        val savedCurrentInterval = savedInstanceState.getInt(KEY_SAVED_CURRENT_INTERVAL, 0)
        val savedIsRunning = savedInstanceState.getBoolean(KEY_SAVED_IS_RUNNING, false)
        val savedElapsedSeconds = savedInstanceState.getInt(KEY_SAVED_ELAPSED_SECONDS, -1)
        val savedPhase = savedInstanceState.getString(KEY_SAVED_PHASE, "slow")
        val savedCompletionAt = savedInstanceState.getLong(KEY_SAVED_COMPLETION_AT_MILLIS, -1L)
        completionAtMillis = if (savedCompletionAt > 0) savedCompletionAt else null
        
        // Only restore if we have valid saved state
        if (savedFormulaName != null && savedTimeRemaining >= 0) {
            // Try to find the formula in predefined formulas first
            var savedFormula = IntervalFormulas.all.find { it.name == savedFormulaName }
            
            // If not found, restore the active custom/saved preset from SharedPreferences.
            if (savedFormula == null && sharedPreferences.getBoolean(KEY_IS_CUSTOM_FORMULA, false)) {
                savedFormula = restoreCustomFormulaFromPrefs()
            }

            if (savedFormula == null) {
                updateUI()
                return
            }

            currentFormula = savedFormula
            homeWorkoutSetup.formulaButton.text = currentFormula.name
            updateFormulaDetails()

            // Restore timer with saved state
            val restoredPhase = when (savedPhase) {
                "slow" -> IntervalPhase.Slow
                "fast" -> IntervalPhase.Fast
                else -> IntervalPhase.Completed
            }

            // Create timer with restored state
            intervalTimer = createIntervalTimer()

            // Set flag to prevent notifications during restoration
            isRestoringTimerState = true

            // Restore the timer state manually
            intervalTimer?.restoreState(
                timeRemainingSeconds = savedTimeRemaining,
                currentInterval = savedCurrentInterval,
                currentPhase = restoredPhase,
                isRunning = savedIsRunning,
                savedElapsedSeconds = savedElapsedSeconds.takeIf { it >= 0 }
            )

            // Clear flag after restoration
            isRestoringTimerState = false

            // Observe state changes with lifecycle awareness
            observeTimerState()

            if (restoredPhase is IntervalPhase.Completed && completionAtMillis == null) {
                // If completion time was not available (e.g., old saved state), start delay from now.
                completionAtMillis = System.currentTimeMillis()
            }

            // Acquire wake lock if timer was running
            if (savedIsRunning) {
                acquireWakeLock()
                startWorkoutForegroundService()
            }

            updateUI()
            updateButtonStates()
        } else {
            updateUI()
        }
    }
    
    private fun restoreCustomFormula() {
        val isCustom = sharedPreferences.getBoolean(KEY_IS_CUSTOM_FORMULA, false)
        if (isCustom) {
            val customFormula = restoreCustomFormulaFromPrefs()
            if (customFormula != null) {
                currentFormula = customFormula
                homeWorkoutSetup.formulaButton.text = currentFormula.name
                updateFormulaDetails()
            }
        }
    }
    
    private fun restoreCustomFormulaFromPrefs(): IntervalFormula? {
        val slowMinutes = sharedPreferences.getInt(KEY_CUSTOM_SLOW_MINUTES, -1)
        val fastMinutes = sharedPreferences.getInt(KEY_CUSTOM_FAST_MINUTES, -1)
        val rounds = sharedPreferences.getInt(KEY_CUSTOM_ROUNDS, -1)
        val startsWithFast = sharedPreferences.getBoolean(KEY_CUSTOM_STARTS_WITH_FAST, false)
        val isCircuit = sharedPreferences.getBoolean(KEY_CUSTOM_IS_CIRCUIT, false)
        val circuitPattern = sharedPreferences.getString(KEY_CUSTOM_CIRCUIT_PATTERN, "fast_slow_fast") ?: "fast_slow_fast"
        
        if (slowMinutes > 0 && fastMinutes > 0 && rounds > 0) {
            val storedDisplayName = sharedPreferences
                .getString(KEY_CUSTOM_FORMULA_DISPLAY_NAME, null)
                ?.takeIf { it.isNotBlank() }
            val restored = if (isCircuit) {
                IntervalFormula(
                    name = generatedCustomFormulaName(slowMinutes, fastMinutes, rounds, true, circuitPattern),
                    slowDurationSeconds = slowMinutes * 60,
                    fastDurationSeconds = fastMinutes * 60,
                    totalIntervals = rounds * 2, // Each circuit = 2 intervals
                    startsWithFast = circuitPattern == "fast_slow_fast",
                    isCircuit = true
                )
            } else {
                IntervalFormula(
                    name = generatedCustomFormulaName(slowMinutes, fastMinutes, rounds, false, circuitPattern),
                    slowDurationSeconds = slowMinutes * 60,
                    fastDurationSeconds = fastMinutes * 60,
                    totalIntervals = rounds,
                    startsWithFast = startsWithFast
                )
            }
            return storedDisplayName?.let { restored.copy(name = it) } ?: restored
        }
        return null
    }

    private fun generatedCustomFormulaName(
        slowMinutes: Int,
        fastMinutes: Int,
        rounds: Int,
        isCircuit: Boolean,
        circuitPattern: String
    ): String {
        return if (isCircuit) {
            val patternText = if (circuitPattern == SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST) {
                "${fastMinutes}-${slowMinutes}-${fastMinutes}"
            } else {
                "${slowMinutes}-${fastMinutes}-${slowMinutes}"
            }
            if (rounds == 1) {
                getString(R.string.format_custom_circuit_name_singular, patternText)
            } else {
                getString(R.string.format_custom_circuit_name, patternText, rounds)
            }
        } else if (rounds == 1) {
            getString(R.string.format_custom_interval_name_singular, slowMinutes, fastMinutes)
        } else {
            getString(R.string.format_custom_interval_name, slowMinutes, fastMinutes, rounds)
        }
    }
    
    private fun saveCustomFormula(
        formula: IntervalFormula,
        isCircuit: Boolean = false,
        circuitPattern: String = "fast_slow_fast",
        activeSavedWorkoutId: Long? = null
    ) {
        val slowMinutes = formula.slowDurationSeconds / 60
        val fastMinutes = formula.fastDurationSeconds / 60
        val rounds = if (isCircuit) formula.totalIntervals / 2 else formula.totalIntervals
        
        sharedPreferences.edit {
            putBoolean(KEY_IS_CUSTOM_FORMULA, true)
                .putString(KEY_CUSTOM_FORMULA_DISPLAY_NAME, formula.name)
                .putInt(KEY_CUSTOM_SLOW_MINUTES, slowMinutes)
                .putInt(KEY_CUSTOM_FAST_MINUTES, fastMinutes)
                .putInt(KEY_CUSTOM_ROUNDS, rounds)
                .putBoolean(KEY_CUSTOM_STARTS_WITH_FAST, formula.startsWithFast)
                .putBoolean(KEY_CUSTOM_IS_CIRCUIT, isCircuit)
                .putString(KEY_CUSTOM_CIRCUIT_PATTERN, circuitPattern)
            if (activeSavedWorkoutId != null) {
                putLong(KEY_ACTIVE_SAVED_WORKOUT_ID, activeSavedWorkoutId)
            } else {
                remove(KEY_ACTIVE_SAVED_WORKOUT_ID)
            }
        }
    }

    private fun isActiveSavedWorkout(workout: SavedWorkout): Boolean =
        sharedPreferences.getLong(KEY_ACTIVE_SAVED_WORKOUT_ID, -1L) == workout.id

    private fun unlinkActiveSavedWorkoutAsCustom(workout: SavedWorkout) {
        val circuitPattern = if (workout.isCircuit) {
            workout.circuitPattern
        } else {
            SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST
        }
        val slowMinutes = workout.slowDurationSeconds / 60
        val fastMinutes = workout.fastDurationSeconds / 60
        val rounds = if (workout.isCircuit) workout.totalIntervals / 2 else workout.totalIntervals
        val customFormula = workout.toIntervalFormula().copy(
            name = generatedCustomFormulaName(
                slowMinutes = slowMinutes,
                fastMinutes = fastMinutes,
                rounds = rounds,
                isCircuit = workout.isCircuit,
                circuitPattern = circuitPattern
            )
        )
        saveCustomFormula(
            formula = customFormula,
            isCircuit = workout.isCircuit,
            circuitPattern = circuitPattern
        )
        currentFormula = customFormula
        homeWorkoutSetup.formulaButton.text = customFormula.name
        updateFormulaDetails()
        updateButtonStates()
    }

    private fun isTimerIdleForFormulaSwap(): Boolean {
        val state = intervalTimer?.state?.value ?: return true
        return !isPreStartCountdownActive &&
            !state.isRunning &&
            state.elapsedSeconds == 0 &&
            state.currentPhase !is IntervalPhase.Completed
    }

    private fun currentFormulaMatches(workout: SavedWorkout): Boolean =
        currentFormula.slowDurationSeconds == workout.slowDurationSeconds &&
            currentFormula.fastDurationSeconds == workout.fastDurationSeconds &&
            currentFormula.totalIntervals == workout.totalIntervals &&
            currentFormula.isCircuit == workout.isCircuit &&
            currentFormula.startsWithFast == workout.startsWithFast

    private fun applySavedWorkoutToHome(workout: SavedWorkout, resetTimerAfterApply: Boolean) {
        val formula = workout.toIntervalFormula()
        val pattern = if (workout.isCircuit) {
            workout.circuitPattern
        } else {
            SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST
        }
        saveCustomFormula(
            formula = formula,
            isCircuit = workout.isCircuit,
            circuitPattern = pattern,
            activeSavedWorkoutId = workout.id
        )
        currentFormula = formula
        homeWorkoutSetup.formulaButton.text = formula.name
        updateFormulaDetails()
        if (resetTimerAfterApply) {
            resetTimer()
        } else {
            updateButtonStates()
        }
    }
    
    /**
     * Creates an IntervalTimer instance with standard phase change and completion callbacks.
     * This method eliminates code duplication across start, reset, and restore operations.
     */
    private fun createIntervalTimer(): IntervalTimer {
        return IntervalTimer(
            formula = currentFormula,
            onPhaseChange = { phase ->
                // Handle phase change notifications (including early notifications for TTS)
                if (!isRestoringTimerState) {
                    if (notificationHelper == null) {
                        notificationHelper = createNotificationHelper()
                    }
                    val useVibration = sharedPreferences.getBoolean(KEY_VIBRATION_ENABLED, true)
                    val useVoice = sharedPreferences.getBoolean(KEY_VOICE_ENABLED, true)
                    
                    // Notify phase change for voice and/or vibration
                    notificationHelper?.notifyPhaseChange(phase, useVoice, useVibration)
                    lastNotifiedPhase = phase
                }

                // Release wake lock and record workout when timer completes (skip record when restoring state to avoid duplicates)
                if (phase is IntervalPhase.Completed) {
                    releaseWakeLock()
                    stopWorkoutForegroundService()
                    if (!isRestoringTimerState) {
                        recordWorkoutCompletion()
                    }
                    scheduleCompletionAutoReset()
                }
            },
            onIntervalComplete = {}
        )
    }
    
    /**
     * Observes timer state changes in a lifecycle-aware manner to prevent memory leaks.
     * Cancels any existing observation before creating a new one.
     */
    private var lastNotifiedPhase: IntervalPhase? = null
    
    private fun observeTimerState() {
        timerJob?.cancel()
        timerJob = lifecycleScope.launch {
            intervalTimer?.state
                ?.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                ?.collect { state ->
                    updateTimerDisplay(state)
                }
        }
    }

    private fun setupOverflowMenuButton() {
        binding.homeSessionPanel.overflowMenuButton.setOnClickListener { view ->
            hapticSelection(view)
            showOverflowBottomSheet()
        }
    }

    private fun showOverflowBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_overflow_menu, android.widget.FrameLayout(this), false)
        bottomSheetDialog.setContentView(view)
        configureBottomSheet(bottomSheetDialog, view)

        view.findViewById<View>(R.id.overflowWorkoutHistory).setOnClickListener {
            hapticSelection(it)
            bottomSheetDialog.dismiss()
            openWorkoutHistory()
        }
        view.findViewById<View>(R.id.overflowSettings).setOnClickListener {
            hapticSelection(it)
            bottomSheetDialog.dismiss()
            showSettingsDialog()
        }
        view.findViewById<View>(R.id.overflowHelp).setOnClickListener {
            hapticSelection(it)
            bottomSheetDialog.dismiss()
            showFaqDialog()
        }
        view.findViewById<View>(R.id.overflowRateApp).setOnClickListener {
            hapticSelection(it)
            bottomSheetDialog.dismiss()
            openUrl("https://play.google.com/store/apps/details?id=com.oceanofmaya.intervalwalktrainer")
        }
        view.findViewById<View>(R.id.overflowReportIssue).setOnClickListener {
            hapticSelection(it)
            bottomSheetDialog.dismiss()
            openUrl("https://github.com/oceanofmaya/interval-walk-trainer-android/issues")
        }

        bottomSheetDialog.show()
    }

    private fun openWorkoutHistory() {
        startActivity(Intent(this, StatsActivity::class.java))
    }

    private fun setupHomeInsights() {
        homeInsightRegistry = HomeInsightRegistry(
            sharedPreferences = sharedPreferences,
            workoutRepository = workoutRepository,
            accentColorProvider = ::getAccentColor,
            onEditWeeklyGoal = ::showWeeklyGoalEditor
        )
        homeInsightsController = HomeInsightsController(
            activity = this,
            binding = binding.homeInsights,
            registry = homeInsightRegistry,
            sharedPreferences = sharedPreferences,
            onEditInsightCards = ::showInsightCardsEditor
        )
    }

    private fun showInsightCardsEditor() {
        InsightCardsEditor(
            activity = this,
            sharedPreferences = sharedPreferences,
            registry = homeInsightRegistry,
            accentColorProvider = ::getAccentColor,
            onSaved = { homeInsightsController.load() }
        ).show()
    }

    private fun showWeeklyGoalEditor() {
        WeeklyGoalEditor(
            activity = this,
            sharedPreferences = sharedPreferences,
            accentColorProvider = ::getAccentColor,
            onSaved = { homeInsightsController.load() }
        ).show()
    }

    private fun setupFormulaSpinner() {
        homeWorkoutSetup.formulaButton.text = currentFormula.name
        updateFormulaDetails()
        
        homeWorkoutSetup.formulaButton.setOnClickListener { view ->
            hapticSelection(view)
            showFormulaSelectorDialog()
        }
        
        // Clear custom formula flag when a predefined formula is selected
        // This is handled in showFormulaSelectorDialog when a regular formula is selected
    }
    
    private fun showFormulaSelectorDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(
            R.layout.bottom_sheet_formula_selector,
            android.widget.FrameLayout(this),
            false
        )
        bottomSheetDialog.setContentView(sheetView)
        configureBottomSheet(bottomSheetDialog, sheetView)
        // Open the picker expanded so all presets are visible immediately.
        // Without this, isFitToContents+responsive height can leave collapsed == expanded,
        // which makes swiping up to "see more" feel broken and confines the user to the list scroll.
        // skipCollapsed=true means drag-down dismisses (no awkward half state).
        bottomSheetDialog.behavior.skipCollapsed = true
        bottomSheetDialog.setOnShowListener {
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        val recyclerView = sheetView.findViewById<RecyclerView>(R.id.formulaRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        applyResponsiveMaxHeight(recyclerView)

        val designYourOwnButton = sheetView.findViewById<MaterialButton>(R.id.designYourOwnButton)
        tintFilledButtonWithAccent(designYourOwnButton)
        designYourOwnButton.setOnClickListener {
            bottomSheetDialog.dismiss()
            showCustomFormulaDialog()
        }

        val refreshSavedList: () -> Unit = {
            lifecycleScope.launch(Dispatchers.IO) {
                val fresh = savedWorkoutRepository.getAllOrdered()
                withContext(Dispatchers.Main) {
                    (recyclerView.adapter as? FormulaSheetAdapter)?.updateSaved(fresh)
                }
            }
        }
        fun refreshAdapter(list: List<SavedWorkout>) {
            val adapter = FormulaSheetAdapter(
                context = this,
                savedWorkouts = list,
                onPickPreset = { formula ->
                    sharedPreferences.edit {
                        putBoolean(KEY_IS_CUSTOM_FORMULA, false)
                        remove(KEY_ACTIVE_SAVED_WORKOUT_ID)
                    }
                    if (currentFormula != formula) {
                        currentFormula = formula
                        homeWorkoutSetup.formulaButton.text = formula.name
                        updateFormulaDetails()
                        resetTimer()
                    }
                    bottomSheetDialog.dismiss()
                },
                onPickSaved = { workout ->
                    applySavedWorkoutToHome(workout, resetTimerAfterApply = true)
                    bottomSheetDialog.dismiss()
                },
                onEmptyCreate = {
                    bottomSheetDialog.dismiss()
                    showCustomFormulaDialog()
                },
                onSavedLongPress = { workout, anchor ->
                    showSavedWorkoutPopupMenu(
                        anchorView = anchor,
                        hostView = sheetView,
                        workout = workout,
                        onChanged = refreshSavedList,
                        onEdit = { row ->
                            bottomSheetDialog.dismiss()
                            editSavedWorkout(row)
                        }
                    )
                },
                onOrderChanged = { ids ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        savedWorkoutRepository.persistOrder(ids)
                    }
                },
                onSavedAction = { actionType, workout ->
                    dispatchSavedAction(
                        actionType = actionType,
                        workout = workout,
                        hostSheet = bottomSheetDialog,
                        hostView = sheetView,
                        refresh = refreshSavedList
                    )
                }
            )
            recyclerView.adapter = adapter
            adapter.attachItemTouchHelper(recyclerView)
        }

        val loadJob = lifecycleScope.launch {
            savedWorkoutRepository.observeAllOrdered().collect { list ->
                withContext(Dispatchers.Main) {
                    if (recyclerView.adapter == null) {
                        refreshAdapter(list)
                    } else {
                        (recyclerView.adapter as? FormulaSheetAdapter)?.updateSaved(list)
                    }
                }
            }
        }
        bottomSheetDialog.setOnDismissListener { loadJob.cancel() }

        bottomSheetDialog.show()
    }

    private suspend fun maybeMigrateLegacySavedWorkouts() {
        val alreadyMigrated = sharedPreferences.getBoolean(KEY_LEGACY_SAVED_MIGRATED, false)
        val isCustom = sharedPreferences.getBoolean(KEY_IS_CUSTOM_FORMULA, false)
        if (alreadyMigrated || !isCustom) return
        // SharedPreferences reads + getString are thread-safe; no need to hop to the main thread.
        val formula = restoreCustomFormulaFromPrefs()
        val pattern = sharedPreferences.getString(
            KEY_CUSTOM_CIRCUIT_PATTERN,
            SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST
        ) ?: SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST
        val decision = SavedWorkoutMigration.decide(
            alreadyMigrated = false,
            hasCustomFormula = true,
            legacyFormula = formula,
            circuitPattern = pattern,
            savedCount = savedWorkoutRepository.count()
        )
        when (decision) {
            SavedWorkoutMigration.Decision.Skip -> Unit
            SavedWorkoutMigration.Decision.MarkMigratedOnly ->
                sharedPreferences.edit { putBoolean(KEY_LEGACY_SAVED_MIGRATED, true) }
            is SavedWorkoutMigration.Decision.InsertAndMark -> {
                val id = savedWorkoutRepository.insertForMigration(decision.entity)
                if (savedWorkoutRepository.getById(id) != null) {
                    sharedPreferences.edit { putBoolean(KEY_LEGACY_SAVED_MIGRATED, true) }
                }
            }
        }
    }

    /**
     * [anchorView] positions the popup menu (the row's ⋮ button); [hostView] is the stable
     * snackbar host. These must differ because when the acted-on row disappears (delete) or the
     * full list rebinds (rename/duplicate → Flow emit → notifyDataSetChanged), the anchor view's
     * ViewHolder is recycled and loses its parent chain, causing Snackbar.make to silently drop
     * the message. [hostView] points at the bottom sheet's root content, which outlives any
     * individual row binding.
     */
    private fun showSavedWorkoutPopupMenu(
        anchorView: View,
        hostView: View,
        workout: SavedWorkout,
        onChanged: () -> Unit,
        onEdit: (SavedWorkout) -> Unit = { editSavedWorkout(it) }
    ) {
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.menu_saved_workout, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit_saved -> {
                    onEdit(workout); true
                }
                R.id.action_rename_saved -> {
                    showRenameSavedWorkoutDialog(workout, hostView, onChanged); true
                }
                R.id.action_duplicate_saved -> {
                    duplicateSavedWorkout(workout, hostView, onChanged); true
                }
                R.id.action_delete_saved -> {
                    removeSavedWorkoutWithUndo(workout, hostView, onChanged); true
                }
                else -> false
            }
        }
        popup.show()
    }

    /**
     * Opens the Design Your Own sheet prefilled from [workout] so the user can tweak its
     * canonical fields. On confirm, the save-after-create sheet routes through
     * [SavedWorkoutRepository.updateFromFormula], preserving the row's id, sortOrder, and createdAt.
     */
    private fun editSavedWorkout(workout: SavedWorkout) {
        showCustomFormulaDialog(editingRow = workout)
    }

    /**
     * Routes a [FormulaSheetAdapter.SavedWorkoutAction] to the correct handler. Extracted so the
     * formula selector's adapter wiring stays small and so Edit can dismiss the host sheet before
     * opening the Design-Your-Own editor (prevents stacked bottom sheets). [hostView] is the view
     * inside the picker that snackbars anchor to (so they remain visible while the sheet is open).
     */
    private fun dispatchSavedAction(
        actionType: FormulaSheetAdapter.SavedWorkoutAction,
        workout: SavedWorkout,
        hostSheet: BottomSheetDialog,
        hostView: View,
        refresh: () -> Unit
    ) {
        when (actionType) {
            FormulaSheetAdapter.SavedWorkoutAction.EDIT -> {
                hostSheet.dismiss()
                editSavedWorkout(workout)
            }
            FormulaSheetAdapter.SavedWorkoutAction.RENAME ->
                showRenameSavedWorkoutDialog(workout, hostView, refresh)
            FormulaSheetAdapter.SavedWorkoutAction.DUPLICATE ->
                duplicateSavedWorkout(workout, hostView, refresh)
            FormulaSheetAdapter.SavedWorkoutAction.DELETE ->
                removeSavedWorkoutWithUndo(workout, hostView, refresh)
        }
    }

    private fun duplicateSavedWorkout(
        workout: SavedWorkout,
        hostView: View,
        onChanged: () -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = savedWorkoutRepository.duplicate(workout.id)
            val copyName = result.getOrNull()?.let { newId ->
                savedWorkoutRepository.getById(newId)?.displayName
            }
            withContext(Dispatchers.Main) {
                if (result.isFailure) {
                    showMaxSavedWorkoutsSnackbar(hostView)
                } else {
                    onChanged()
                    if (copyName != null) {
                        showPresetSnackbar(
                            host = hostView,
                            message = getString(R.string.snackbar_preset_duplicated, copyName)
                        )
                    }
                }
            }
        }
    }

    /**
     * Removes a saved preset immediately and shows an undoable snackbar. The dialog-confirmation
     * flow was dropped in favor of this pattern because (a) it matches modern Material lists
     * (Gmail / Keep / Files), (b) it makes Delete a single tap instead of two, and (c) it is
     * actually safer: mistakes are recoverable for ~5 seconds vs the old "confirm then permanent"
     * flow. Undo re-inserts from the captured [snapshot] preserving id / sortOrder / createdAt
     * via [SavedWorkoutRepository.restore].
     */
    private fun removeSavedWorkoutWithUndo(
        workout: SavedWorkout,
        hostView: View,
        onChanged: () -> Unit
    ) {
        val wasActive = isActiveSavedWorkout(workout)
        if (wasActive) {
            showDeleteActivePresetDialog(workout, hostView, onChanged)
        } else {
            removeSavedWorkoutWithUndoConfirmed(workout, hostView, onChanged, wasActive)
        }
    }

    private fun showDeleteActivePresetDialog(
        workout: SavedWorkout,
        hostView: View,
        onChanged: () -> Unit
    ) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.prompt_delete_active_preset)
            .setMessage(R.string.message_delete_active_preset_keeps_timer)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                removeSavedWorkoutWithUndoConfirmed(
                    workout = workout,
                    hostView = hostView,
                    onChanged = onChanged,
                    wasActive = true
                )
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun removeSavedWorkoutWithUndoConfirmed(
        workout: SavedWorkout,
        hostView: View,
        onChanged: () -> Unit,
        wasActive: Boolean
    ) {
        val snapshot = workout
        lifecycleScope.launch(Dispatchers.IO) {
            savedWorkoutRepository.delete(snapshot.id)
            withContext(Dispatchers.Main) {
                if (wasActive) {
                    unlinkActiveSavedWorkoutAsCustom(snapshot)
                }
                onChanged()
                showPresetSnackbar(
                    host = hostView,
                    message = getString(R.string.snackbar_preset_removed, snapshot.displayName),
                    actionLabelRes = R.string.action_undo,
                    onAction = {
                        undoRemoveSavedWorkout(
                            snapshot,
                            hostView,
                            onChanged,
                            relinkIfStillActiveCopy = wasActive
                        )
                    },
                    durationMs = UNDO_SNACKBAR_DURATION_MS
                )
            }
        }
    }

    private fun undoRemoveSavedWorkout(
        snapshot: SavedWorkout,
        hostView: View,
        onChanged: () -> Unit,
        relinkIfStillActiveCopy: Boolean
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = savedWorkoutRepository.restore(snapshot)
            val restored = if (result.isSuccess) {
                savedWorkoutRepository.getById(snapshot.id) ?: snapshot
            } else {
                null
            }
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    if (relinkIfStillActiveCopy && currentFormulaMatches(snapshot)) {
                        applySavedWorkoutToHome(restored ?: snapshot, resetTimerAfterApply = false)
                    }
                    onChanged()
                } else {
                    showPresetSnackbar(
                        host = hostView,
                        message = getString(R.string.snackbar_preset_restore_failed)
                    )
                }
            }
        }
    }

    private fun showRenameSavedWorkoutDialog(
        workout: SavedWorkout,
        hostView: View,
        onChanged: () -> Unit
    ) {
        val container = FrameLayout(this).apply {
            val pad = (resources.displayMetrics.density * 24).toInt()
            setPadding(pad, pad / 2, pad, 0)
        }
        val input = EditText(this).apply {
            setText(workout.displayName)
            setSelection(text?.length ?: 0)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_DONE
            setSingleLine(true)
            filters = arrayOf(
                android.text.InputFilter.LengthFilter(SavedWorkoutRepository.MAX_DISPLAY_NAME_LENGTH)
            )
        }
        container.addView(
            input,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val wasActive = isActiveSavedWorkout(workout)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.prompt_rename_preset)
            .setView(container)
            .setPositiveButton(R.string.action_rename) { d, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isNotEmpty() && name != workout.displayName) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val result = savedWorkoutRepository.updateDisplayName(workout.id, name)
                        // Read back the persisted name so the snackbar reflects any
                        // uniquification (e.g. "Morning walk" → "Morning walk (2)").
                        val persistedRow = if (result.isSuccess) {
                            savedWorkoutRepository.getById(workout.id)
                        } else {
                            null
                        }
                        withContext(Dispatchers.Main) {
                            onChanged()
                            if (persistedRow != null) {
                                if (wasActive) {
                                    applySavedWorkoutToHome(persistedRow, resetTimerAfterApply = false)
                                }
                                showPresetSnackbar(
                                    host = hostView,
                                    message = getString(
                                        R.string.snackbar_preset_renamed,
                                        persistedRow.displayName
                                    )
                                )
                            }
                        }
                    }
                }
                d.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.performClick()
                true
            } else {
                false
            }
        }
        dialog.show()
    }

    private class SaveSheetState(var capped: Boolean = false)

    private fun buildSaveSheetSummary(formula: IntervalFormula, isCircuit: Boolean): String {
        val totalMinutes = formula.totalDurationSeconds / 60
        val typeLabel = getString(
            if (isCircuit) R.string.label_workout_type_circuit else R.string.label_workout_type_interval
        )
        val rounds = if (isCircuit) formula.totalIntervals / 2 else formula.totalIntervals
        val slowMin = formula.slowDurationSeconds / 60
        val fastMin = formula.fastDurationSeconds / 60
        return "$typeLabel · $rounds × (${slowMin}m slow / ${fastMin}m fast) · ${totalMinutes}m total"
    }

    /**
     * Unified snackbar for preset-mutation feedback (saved / renamed / removed / duplicated /
     * updated / cap-hit / errors).
     *
     * We don't pass [host] straight to [Snackbar.make] because its internal
     * `findSuitableParent` walks up to the first CoordinatorLayout it finds. For a view inside
     * the preset picker's BottomSheetDialog that's the dialog's *internal* CoordinatorLayout,
     * which is where `design_bottom_sheet` also lives — and in practice the Snackbar ends up
     * rendered underneath/behind the sheet content there and is invisible to the user.
     *
     * Instead we walk all the way up to the hosting `Window`'s `android.R.id.content`
     * FrameLayout (for a BottomSheetDialog that's the dialog's own top-level content FrameLayout,
     * one level below its DecorView) and attach the Snackbar there. Children added to that
     * FrameLayout draw on top of everything else in the window, so the Snackbar is guaranteed
     * to overlay the sheet. For activity-level hosts (e.g. after the sheet is dismissed) this
     * resolves to the activity's own content FrameLayout and behaves normally.
     *
     * Messages are pre-resolved by callers to avoid the silent ambiguity of Kotlin named-vararg
     * arguments with getString formatting. When [actionLabelRes] + [onAction] are supplied, the
     * action text is tinted with the app accent.
     */
    private fun showPresetSnackbar(
        host: View,
        message: String,
        @StringRes actionLabelRes: Int = 0,
        onAction: (() -> Unit)? = null,
        durationMs: Int = Snackbar.LENGTH_LONG
    ) {
        val snackbarParent = resolveSnackbarParent(host)
        val snackbar = Snackbar.make(snackbarParent, message, durationMs)
        if (actionLabelRes != 0 && onAction != null) {
            snackbar.setAction(actionLabelRes) { onAction() }
            snackbar.setActionTextColor(getAccentColor())
        }
        snackbar.show()
    }

    /**
     * Walk up the view tree from [host] to the hosting [android.view.Window]'s
     * `android.R.id.content` FrameLayout. Returns [host] itself if none is found (defensive; in
     * practice every dialog/activity window has one).
     */
    private fun resolveSnackbarParent(host: View): View {
        var current: View? = host
        while (current != null) {
            if (current.id == android.R.id.content && current is FrameLayout) {
                return current
            }
            current = current.parent as? View
        }
        return host
    }

    private fun showMaxSavedWorkoutsSnackbar(host: View = binding.root) {
        showPresetSnackbar(
            host = host,
            message = getString(
                R.string.message_max_saved_presets,
                SavedWorkoutRepository.MAX_SAVED_WORKOUTS
            )
        )
    }

    private fun showSaveWorkoutAfterCreateSheet(
        customFormula: IntervalFormula,
        isCircuitMode: Boolean,
        circuitPattern: String,
        editingRow: SavedWorkout? = null
    ) {
        val saveDialog = BottomSheetDialog(this)
        val v = LayoutInflater.from(this).inflate(
            R.layout.bottom_sheet_save_workout,
            android.widget.FrameLayout(this),
            false
        )
        saveDialog.setContentView(v)
        configureBottomSheet(saveDialog, v)
        val isEditing = editingRow != null
        v.findViewById<android.widget.TextView>(R.id.saveSheetTitle).setText(
            if (isEditing) R.string.prompt_update_preset else R.string.prompt_save_preset
        )
        val nameLayout = v.findViewById<TextInputLayout>(R.id.saveWorkoutNameLayout)
        val nameInput = v.findViewById<TextInputEditText>(R.id.saveWorkoutNameInput)
        nameInput.setText(editingRow?.displayName ?: customFormula.name)
        nameInput.setSelection(nameInput.text?.length ?: 0)
        val capMsg = v.findViewById<android.widget.TextView>(R.id.saveWorkoutCapMessage)
        val saveAndUse = v.findViewById<MaterialButton>(R.id.saveAndUseButton)
        val useOnly = v.findViewById<MaterialButton>(R.id.useWithoutSavingButton)
        val saveOnly = v.findViewById<MaterialButton>(R.id.saveOnlyButton)
        if (isEditing) {
            // Edit mode reuses the three-button layout but the semantics are UPDATE-in-place,
            // so all actions use "Update" language to match the sheet prompt.
            saveAndUse.setText(R.string.cta_update_and_use_preset)
            saveOnly.setText(R.string.cta_update_preset)
            useOnly.setText(R.string.cta_use_without_updating)
        }

        tintFilledButtonWithAccent(saveAndUse)
        tintOutlinedButtonWithAccent(saveOnly)
        tintTextButtonWithAccent(useOnly)

        v.findViewById<android.widget.TextView>(R.id.saveSheetSummary).text =
            buildSaveSheetSummary(customFormula, isCircuitMode)

        val saveState = SaveSheetState()
        fun refreshSaveButtons() {
            val hasName = (nameInput.text?.toString()?.trim().orEmpty()).isNotEmpty()
            saveAndUse.isEnabled = hasName && !saveState.capped
            saveOnly.isEnabled = hasName && !saveState.capped
        }
        nameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                nameLayout.error = null
                refreshSaveButtons()
            }
        })
        nameInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && saveAndUse.isEnabled) {
                saveAndUse.performClick()
                true
            } else {
                false
            }
        }
        refreshSaveButtons()

        if (isEditing) {
            // Edit never inserts a new row, so the cap does not apply. saveState.capped stays false.
            refreshSaveButtons()
        } else {
            lifecycleScope.launch {
                val capped = withContext(Dispatchers.IO) {
                    savedWorkoutRepository.count() >= SavedWorkoutRepository.MAX_SAVED_WORKOUTS
                }
                saveState.capped = capped
                if (capped) {
                    capMsg.text = getString(
                        R.string.message_max_saved_presets,
                        SavedWorkoutRepository.MAX_SAVED_WORKOUTS
                    )
                    capMsg.visibility = View.VISIBLE
                }
                refreshSaveButtons()
            }
        }

        wireSaveWorkoutAfterCreateSheetActions(
            saveDialog = saveDialog,
            nameInput = nameInput,
            saveAndUse = saveAndUse,
            useOnly = useOnly,
            saveOnly = saveOnly,
            customFormula = customFormula,
            isCircuitMode = isCircuitMode,
            circuitPattern = circuitPattern,
            editingRow = editingRow
        )
        saveDialog.show()
    }

    /**
     * Context object for the save-after-create / save-after-edit sheet's button handlers.
     * Bundled so we can extract the three click bodies into focused helpers without dragging
     * a forest of parameters through every call.
     */
    private class SaveSheetContext(
        val saveDialog: BottomSheetDialog,
        val nameInput: TextInputEditText,
        val saveAndUse: MaterialButton,
        val useOnly: MaterialButton,
        val saveOnly: MaterialButton,
        val customFormula: IntervalFormula,
        val isCircuitMode: Boolean,
        val circuitPattern: String,
        val editingRow: SavedWorkout?
    ) {
        val inFlight: java.util.concurrent.atomic.AtomicBoolean = java.util.concurrent.atomic.AtomicBoolean(false)
        fun currentName(): String = nameInput.text?.toString()?.trim().orEmpty()
        fun setBusy(busy: Boolean) {
            val hasName = currentName().isNotEmpty()
            saveAndUse.isEnabled = !busy && hasName
            saveOnly.isEnabled = !busy && hasName
            useOnly.isEnabled = !busy
        }
    }

    private suspend fun persistForSaveSheet(
        ctx: SaveSheetContext,
        name: String
    ): Result<Long> = if (ctx.editingRow != null) {
        savedWorkoutRepository.updateFromFormula(
            id = ctx.editingRow.id,
            newDisplayName = name,
            formula = ctx.customFormula,
            circuitPattern = ctx.circuitPattern
        ).map { ctx.editingRow.id }
    } else {
        savedWorkoutRepository.insertFromFormula(name, ctx.customFormula, ctx.circuitPattern)
    }

    private fun showPersistFailure(ctx: SaveSheetContext, result: Result<Long>) {
        val missingRow = ctx.editingRow != null && result.exceptionOrNull() is IllegalArgumentException
        if (missingRow) {
            showPresetSnackbar(
                host = binding.root,
                message = getString(R.string.snackbar_preset_no_longer_exists)
            )
        } else {
            showMaxSavedWorkoutsSnackbar()
        }
    }

    private fun wireSaveWorkoutAfterCreateSheetActions(
        saveDialog: BottomSheetDialog,
        nameInput: TextInputEditText,
        saveAndUse: MaterialButton,
        useOnly: MaterialButton,
        saveOnly: MaterialButton,
        customFormula: IntervalFormula,
        isCircuitMode: Boolean,
        circuitPattern: String,
        editingRow: SavedWorkout? = null
    ) {
        val ctx = SaveSheetContext(
            saveDialog, nameInput, saveAndUse, useOnly, saveOnly,
            customFormula, isCircuitMode, circuitPattern, editingRow
        )
        saveAndUse.setOnClickListener { btn -> onSaveAndUseClicked(ctx, btn) }
        useOnly.setOnClickListener { btn -> onUseWithoutSavingClicked(ctx, btn) }
        saveOnly.setOnClickListener { btn -> onSaveOnlyClicked(ctx, btn) }
    }

    private fun onSaveAndUseClicked(ctx: SaveSheetContext, btn: View) {
        val name = ctx.currentName()
        if (name.isEmpty()) return
        if (!ctx.inFlight.compareAndSet(false, true)) return
        hapticSuccess(btn)
        ctx.setBusy(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val result = persistForSaveSheet(ctx, name)
            withContext(Dispatchers.Main) {
                try {
                    if (result.isFailure) {
                        showPersistFailure(ctx, result)
                    } else {
                        applySaveAndUse(ctx, result.getOrThrow(), name)
                        // Sheet is dismissed; snackbar appears on the main activity as a secondary
                        // confirmation on top of the visible state change (home-screen button
                        // updated). Same message whether save-only or save-and-use, so users build
                        // a consistent mental model of "saved presets live in My saved presets".
                        showSaveSuccessSnackbar(ctx)
                    }
                } finally {
                    ctx.inFlight.set(false)
                    ctx.setBusy(false)
                }
            }
        }
    }

    private suspend fun applySaveAndUse(ctx: SaveSheetContext, rowId: Long, name: String) {
        val row = savedWorkoutRepository.getById(rowId)
        if (row != null) {
            applySavedWorkoutToHome(row, resetTimerAfterApply = true)
        } else {
            val toUse = ctx.customFormula.copy(name = name)
            saveCustomFormula(toUse, ctx.isCircuitMode, ctx.circuitPattern)
            currentFormula = toUse
            homeWorkoutSetup.formulaButton.text = toUse.name
            updateFormulaDetails()
            resetTimer()
        }
        ctx.saveDialog.dismiss()
    }

    private fun showSaveSuccessSnackbar(ctx: SaveSheetContext) {
        val messageRes = if (ctx.editingRow != null) {
            R.string.snackbar_preset_updated
        } else {
            R.string.snackbar_saved_to_my_presets
        }
        showPresetSnackbar(host = binding.root, message = getString(messageRes))
    }

    private fun onUseWithoutSavingClicked(ctx: SaveSheetContext, btn: View) {
        if (!ctx.inFlight.compareAndSet(false, true)) return
        hapticSuccess(btn)
        ctx.setBusy(true)
        // Apply the in-memory (possibly edited) formula once; the saved row, if any, is
        // intentionally left untouched so "use without saving" preserves its prior state.
        saveCustomFormula(ctx.customFormula, ctx.isCircuitMode, ctx.circuitPattern)
        currentFormula = ctx.customFormula
        homeWorkoutSetup.formulaButton.text = ctx.customFormula.name
        updateFormulaDetails()
        resetTimer()
        ctx.saveDialog.dismiss()
        // No need to release - the dialog is gone. Leaving inFlight=true is intentional.
    }

    private fun onSaveOnlyClicked(ctx: SaveSheetContext, btn: View) {
        val name = ctx.currentName()
        if (name.isEmpty()) return
        hapticSuccess(btn)
        val updatesActivePreset = ctx.editingRow?.let { isActiveSavedWorkout(it) } == true
        if (updatesActivePreset && !isTimerIdleForFormulaSwap()) {
            showActivePresetUpdateResetDialog(ctx, name)
            return
        }
        persistSaveOnly(ctx, name, applyActiveAfterUpdate = updatesActivePreset)
    }

    private fun showActivePresetUpdateResetDialog(ctx: SaveSheetContext, name: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.prompt_update_preset)
            .setMessage(R.string.message_update_active_preset_resets_timer)
            .setPositiveButton(R.string.action_update_and_reset) { _, _ ->
                persistSaveOnly(ctx, name, applyActiveAfterUpdate = true)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun persistSaveOnly(
        ctx: SaveSheetContext,
        name: String,
        applyActiveAfterUpdate: Boolean
    ) {
        if (!ctx.inFlight.compareAndSet(false, true)) return
        ctx.setBusy(true)
        lifecycleScope.launch(Dispatchers.IO) {
            val result = persistForSaveSheet(ctx, name)
            val updatedActiveRow = if (result.isSuccess && applyActiveAfterUpdate && ctx.editingRow != null) {
                savedWorkoutRepository.getById(ctx.editingRow.id)
            } else {
                null
            }
            withContext(Dispatchers.Main) {
                try {
                    if (result.isFailure) {
                        showPersistFailure(ctx, result)
                    } else {
                        if (updatedActiveRow != null) {
                            applySavedWorkoutToHome(updatedActiveRow, resetTimerAfterApply = true)
                        }
                        ctx.saveDialog.dismiss()
                        showSaveSuccessSnackbar(ctx)
                    }
                } finally {
                    ctx.inFlight.set(false)
                    ctx.setBusy(false)
                }
            }
        }
    }

    private fun showCustomFormulaDialog(editingRow: SavedWorkout? = null) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_custom_formula, android.widget.FrameLayout(this), false)
        bottomSheetDialog.setContentView(view)
        configureBottomSheet(bottomSheetDialog, view)

        if (editingRow != null) {
            view.findViewById<android.widget.TextView>(R.id.customFormulaSheetTitle)
                .setText(R.string.title_edit_preset)
        }
        
        val slowValue = view.findViewById<android.widget.TextView>(R.id.slowDurationValue)
        val fastValue = view.findViewById<android.widget.TextView>(R.id.fastDurationValue)
        val roundsValue = view.findViewById<android.widget.TextView>(R.id.roundsValue)
        val slowDecrementButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.slowDecrementButton)
        val slowIncrementButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.slowIncrementButton)
        val fastDecrementButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.fastDecrementButton)
        val fastIncrementButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.fastIncrementButton)
        val roundsDecrementButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.roundsDecrementButton)
        val roundsIncrementButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.roundsIncrementButton)
        val modeToggleGroup = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.modeToggleGroup)
        val intervalModeButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.intervalModeButton)
        val circuitModeButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.circuitModeButton)
        val circuitPatternContainer = view.findViewById<ViewGroup>(R.id.circuitPatternContainer)
        val fastSlowFastRadio = view.findViewById<android.widget.RadioButton>(R.id.fastSlowFastRadio)
        val slowFastSlowRadio = view.findViewById<android.widget.RadioButton>(R.id.slowFastSlowRadio)
        val startWithContainer = view.findViewById<ViewGroup>(R.id.startWithContainer)
        val slowFirstRadio = view.findViewById<android.widget.RadioButton>(R.id.slowFirstRadio)
        val fastFirstRadio = view.findViewById<android.widget.RadioButton>(R.id.fastFirstRadio)
        val resetDefaultsButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.resetDefaultsButton)
        val createButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.createButton)
        if (editingRow != null) {
            createButton.setText(R.string.cta_update_preset)
        }
        val accentColor = getAccentColor()
        val accentTint = android.content.res.ColorStateList.valueOf(accentColor)
        createButton.backgroundTintList = accentTint
        slowFirstRadio.buttonTintList = accentTint
        fastFirstRadio.buttonTintList = accentTint
        fastSlowFastRadio.buttonTintList = accentTint
        slowFastSlowRadio.buttonTintList = accentTint
        
        // Default values
        val defaultSlowMinutes = 3
        val defaultFastMinutes = 3
        val defaultRounds = 5
        
        // Initialize values
        var slowMinutes = defaultSlowMinutes
        var fastMinutes = defaultFastMinutes
        var rounds = defaultRounds
        
        // Restore last selected mode (circuit or interval) from preferences
        val savedMode = sharedPreferences.getString(KEY_CUSTOM_FORMULA_MODE, "interval") ?: "interval"
        var isCircuitMode = savedMode == "circuit"
        var circuitPattern = sharedPreferences.getString(KEY_CUSTOM_CIRCUIT_PATTERN, "fast_slow_fast") ?: "fast_slow_fast"

        // Prefill source:
        //  - Edit flow: from the specific SavedWorkout so the editor reflects that row's canonical
        //    fields even if prefs point at a different custom formula (e.g. the user picked another
        //    preset after saving this one).
        //  - Create flow: from prefs when the user has an active custom formula, preserving legacy
        //    "continue tweaking the last thing you made" behavior.
        if (editingRow != null) {
            slowMinutes = editingRow.slowDurationSeconds / 60
            fastMinutes = editingRow.fastDurationSeconds / 60
            if (editingRow.isCircuit) {
                isCircuitMode = true
                rounds = editingRow.totalIntervals / 2
                circuitPattern = editingRow.circuitPattern
            } else {
                isCircuitMode = false
                rounds = editingRow.totalIntervals
                if (editingRow.startsWithFast) {
                    fastFirstRadio.isChecked = true
                } else {
                    slowFirstRadio.isChecked = true
                }
            }
        } else if (sharedPreferences.getBoolean(KEY_IS_CUSTOM_FORMULA, false)) {
            val restored = restoreCustomFormulaFromPrefs()
            if (restored != null) {
                slowMinutes = restored.slowDurationSeconds / 60
                fastMinutes = restored.fastDurationSeconds / 60
                val isCircuit = restored.isCircuit
                if (isCircuit) {
                    isCircuitMode = true
                    rounds = restored.totalIntervals / 2
                    circuitPattern = if (restored.startsWithFast) "fast_slow_fast" else "slow_fast_slow"
                } else {
                    rounds = restored.totalIntervals
                    if (restored.startsWithFast) {
                        fastFirstRadio.isChecked = true
                    } else {
                        slowFirstRadio.isChecked = true
                    }
                }
            }
        }
        
        // Set circuit pattern radio buttons based on restored pattern
        if (circuitPattern == "fast_slow_fast") {
            fastSlowFastRadio.isChecked = true
        } else {
            slowFastSlowRadio.isChecked = true
        }
        
        // Update UI visibility based on circuit mode
        fun updateModeVisibility() {
            if (isCircuitMode) {
                circuitPatternContainer.visibility = View.VISIBLE
                startWithContainer.visibility = View.GONE
            } else {
                circuitPatternContainer.visibility = View.GONE
                startWithContainer.visibility = View.VISIBLE
            }
        }
        
        // Set toggle group selection based on mode
        if (isCircuitMode) {
            modeToggleGroup.check(R.id.circuitModeButton)
        } else {
            modeToggleGroup.check(R.id.intervalModeButton)
        }
        
        // Initialize visibility based on pre-filled values
        updateModeVisibility()
        
        // Update button styling based on selection
        fun updateButtonStyles() {
            val primaryColor = getAccentColor()
            val whiteColor = ContextCompat.getColor(this, R.color.white)
            val surfaceColor = ContextCompat.getColor(this, R.color.surface)
            val textPrimaryColor = ContextCompat.getColor(this, R.color.text_primary)
            val strokeLightColor = ContextCompat.getColor(this, R.color.stroke_light)
            
            if (isCircuitMode) {
                // Circuit mode selected - highlight circuit button
                circuitModeButton.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                circuitModeButton.setTextColor(whiteColor)
                circuitModeButton.strokeColor = android.content.res.ColorStateList.valueOf(primaryColor)
                
                intervalModeButton.backgroundTintList = android.content.res.ColorStateList.valueOf(surfaceColor)
                intervalModeButton.setTextColor(textPrimaryColor)
                intervalModeButton.strokeColor = android.content.res.ColorStateList.valueOf(strokeLightColor)
            } else {
                // Interval mode selected - highlight interval button
                intervalModeButton.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                intervalModeButton.setTextColor(whiteColor)
                intervalModeButton.strokeColor = android.content.res.ColorStateList.valueOf(primaryColor)
                
                circuitModeButton.backgroundTintList = android.content.res.ColorStateList.valueOf(surfaceColor)
                circuitModeButton.setTextColor(textPrimaryColor)
                circuitModeButton.strokeColor = android.content.res.ColorStateList.valueOf(strokeLightColor)
            }
        }
        
        modeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isCircuitMode = checkedId == R.id.circuitModeButton
                // Save the mode preference
                sharedPreferences.edit {
                    putString(KEY_CUSTOM_FORMULA_MODE, if (isCircuitMode) "circuit" else "interval")
                }
                updateButtonStyles()
                updateModeVisibility()
            }
        }
        
        // Initialize button styles
        updateButtonStyles()
        
        fastSlowFastRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                circuitPattern = "fast_slow_fast"
                // Save pattern preference
                sharedPreferences.edit {
                    putString(KEY_CUSTOM_CIRCUIT_PATTERN, "fast_slow_fast")
                }
            }
        }
        
        slowFastSlowRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                circuitPattern = "slow_fast_slow"
                // Save pattern preference
                sharedPreferences.edit {
                    putString(KEY_CUSTOM_CIRCUIT_PATTERN, "slow_fast_slow")
                }
            }
        }
        
        // Update display
        fun updateDisplay() {
            slowValue.text = String.format(Locale.US, "%d", slowMinutes)
            fastValue.text = String.format(Locale.US, "%d", fastMinutes)
            roundsValue.text = String.format(Locale.US, "%d", rounds)
        }
        
        // Reset to defaults function
        fun resetToDefaults() {
            slowMinutes = defaultSlowMinutes
            fastMinutes = defaultFastMinutes
            rounds = defaultRounds
            isCircuitMode = false
            modeToggleGroup.check(R.id.intervalModeButton)
            circuitPattern = "fast_slow_fast"
            fastSlowFastRadio.isChecked = true
            slowFirstRadio.isChecked = true
            fastFirstRadio.isChecked = false
            // Reset mode and pattern preferences
            sharedPreferences.edit {
                putString(KEY_CUSTOM_FORMULA_MODE, "interval")
                    .putString(KEY_CUSTOM_CIRCUIT_PATTERN, "fast_slow_fast")
            }
            updateModeVisibility()
            updateDisplay()
        }
        
        updateDisplay()
        
        // Set up increment/decrement buttons with haptic feedback
        slowDecrementButton.setOnClickListener { btn ->
            performHapticFeedback(btn)
            if (slowMinutes > 1) {
                slowMinutes--
                updateDisplay()
            }
        }
        
        slowIncrementButton.setOnClickListener { btn ->
            performHapticFeedback(btn)
            if (slowMinutes < 60) {
                slowMinutes++
                updateDisplay()
            }
        }
        
        fastDecrementButton.setOnClickListener { btn ->
            performHapticFeedback(btn)
            if (fastMinutes > 1) {
                fastMinutes--
                updateDisplay()
            }
        }
        
        fastIncrementButton.setOnClickListener { btn ->
            performHapticFeedback(btn)
            if (fastMinutes < 60) {
                fastMinutes++
                updateDisplay()
            }
        }
        
        roundsDecrementButton.setOnClickListener { btn ->
            performHapticFeedback(btn)
            if (rounds > 1) {
                rounds--
                updateDisplay()
            }
        }
        
        roundsIncrementButton.setOnClickListener { btn ->
            performHapticFeedback(btn)
            if (rounds < 100) {
                rounds++
                updateDisplay()
            }
        }
        
        resetDefaultsButton.setOnClickListener { btn ->
            performHapticFeedback(btn)
            resetToDefaults()
        }
        
        createButton.setOnClickListener { btn ->
            hapticSuccess(btn)
            // Create custom formula
            val customFormula = if (isCircuitMode) {
                // Circuit: pattern repeats, totalIntervals = rounds * 2 (each circuit = 2 intervals)
                val startsWithFast = circuitPattern == "fast_slow_fast"
                val patternText = if (circuitPattern == "fast_slow_fast") {
                    "${fastMinutes}-${slowMinutes}-${fastMinutes}"
                } else {
                    "${slowMinutes}-${fastMinutes}-${slowMinutes}"
                }
                IntervalFormula(
                    name = if (rounds == 1) {
                        getString(R.string.format_custom_circuit_name_singular, patternText)
                    } else {
                        getString(R.string.format_custom_circuit_name, patternText, rounds)
                    },
                    slowDurationSeconds = slowMinutes * 60,
                    fastDurationSeconds = fastMinutes * 60,
                    totalIntervals = rounds * 2, // Each circuit = 2 intervals
                    startsWithFast = startsWithFast,
                    isCircuit = true
                )
            } else {
                // Regular interval
                IntervalFormula(
                    name = if (rounds == 1) {
                        getString(R.string.format_custom_interval_name_singular, slowMinutes, fastMinutes)
                    } else {
                        getString(R.string.format_custom_interval_name, slowMinutes, fastMinutes, rounds)
                    },
                    slowDurationSeconds = slowMinutes * 60,
                    fastDurationSeconds = fastMinutes * 60,
                    totalIntervals = rounds,
                    startsWithFast = !slowFirstRadio.isChecked
                )
            }
            
            bottomSheetDialog.dismiss()
            showSaveWorkoutAfterCreateSheet(
                customFormula = customFormula,
                isCircuitMode = isCircuitMode,
                circuitPattern = circuitPattern,
                editingRow = editingRow
            )
        }
        
        bottomSheetDialog.show()
    }


    /**
     * Caps a scrollable child of a bottom sheet so the sheet stays usable on small phones
     * and in landscape (where the default 80% sheet height can hide later sections).
     * Uses a one-shot pre-draw listener so we only override when the natural measured height
     * exceeds the cap.
     */
    private fun applyResponsiveMaxHeight(view: View) {
        val isLandscape = resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val fraction = if (isLandscape) LANDSCAPE_LIST_MAX_FRACTION else PORTRAIT_LIST_MAX_FRACTION
        val maxHeightPx = (resources.displayMetrics.heightPixels * fraction).toInt()
        view.viewTreeObserver.addOnPreDrawListener(
            object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (view.height > maxHeightPx) {
                        view.viewTreeObserver.removeOnPreDrawListener(this)
                        val lp = view.layoutParams
                        lp.height = maxHeightPx
                        view.layoutParams = lp
                        return false
                    }
                    return true
                }
            }
        )
    }

    private fun configureBottomSheet(dialog: BottomSheetDialog, contentView: View) {
        val behavior = dialog.behavior
        behavior.isFitToContents = true
        behavior.isDraggable = true
        behavior.skipCollapsed = false
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED

        contentView.doOnLayout {
            val screenHeight = resources.displayMetrics.heightPixels
            val availableWidth = (contentView.parent as? View)?.width?.takeIf { it > 0 }
                ?: resources.displayMetrics.widthPixels
            val minPeekHeight = (screenHeight * 0.40f).toInt()
            val maxPeekHeight = (screenHeight * 0.80f).toInt()
            val widthSpec = View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
            contentView.measure(widthSpec, heightSpec)
            val contentHeight = contentView.measuredHeight
            // Ensure peek height is at least the full content height to prevent bottom buttons from being cut off
            // Always use full content height if it fits, otherwise cap at max
            val finalPeekHeight = if (contentHeight <= maxPeekHeight) {
                // Content fits - use full height to show everything including bottom button
                contentHeight
            } else {
                // Content too tall - use max height (content will be scrollable)
                maxPeekHeight
            }
            behavior.peekHeight = finalPeekHeight.coerceAtLeast(minPeekHeight)
        }
    }
    
    private fun showSettingsDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_settings, android.widget.FrameLayout(this), false)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.setOnDismissListener {
            settingsNotificationsSwitch = null
        }
        
        // Enable edge-to-edge for bottom sheet dialog
        bottomSheetDialog.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)

            val scrollView = view.findViewById<androidx.core.widget.NestedScrollView>(R.id.settingsScroll)
            val basePaddingBottom = scrollView.paddingBottom
            // Apply window insets to account for system navigation bar at bottom
            ViewCompat.setOnApplyWindowInsetsListener(scrollView) { v, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                // Add bottom padding to prevent content from being hidden behind navigation bar
                v.updatePadding(bottom = basePaddingBottom + insets.bottom)
                windowInsets
            }
        }
        
        configureBottomSheet(bottomSheetDialog, view)
        
        // Set app version
        val versionText = view.findViewById<android.widget.TextView>(R.id.appVersion)
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            versionText.text = getString(R.string.format_version, packageInfo.versionName)
        } catch (e: Exception) {
            versionText.text = getString(R.string.format_version, "Unknown")
        }
        
        // Privacy Policy button
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.privacyPolicyButton).setOnClickListener { btn ->
            hapticSelection(btn)
            openUrl("https://github.com/oceanofmaya/interval-walk-trainer-android/blob/main/PRIVACY.md")
            bottomSheetDialog.dismiss()
        }
        
        // Terms button
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.termsButton).setOnClickListener { btn ->
            hapticSelection(btn)
            openUrl("https://github.com/oceanofmaya/interval-walk-trainer-android/blob/main/TERMS.md")
            bottomSheetDialog.dismiss()
        }
        
        // Theme mode buttons
        val currentThemeMode = sharedPreferences.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        val themeSystemButton = view.findViewById<android.widget.ImageButton>(R.id.themeSystemButton)
        val themeLightButton = view.findViewById<android.widget.ImageButton>(R.id.themeLightButton)
        val themeDarkButton = view.findViewById<android.widget.ImageButton>(R.id.themeDarkButton)
        val accentBlueButton = view.findViewById<android.widget.ImageButton>(R.id.accentBlueButton)
        val accentTealButton = view.findViewById<android.widget.ImageButton>(R.id.accentTealButton)
        val accentPurpleButton = view.findViewById<android.widget.ImageButton>(R.id.accentPurpleButton)
        val accentAmberButton = view.findViewById<android.widget.ImageButton>(R.id.accentAmberButton)
        val accentMagentaButton = view.findViewById<android.widget.ImageButton>(R.id.accentMagentaButton)
        var saveWorkoutsSwitchRef: com.google.android.material.switchmaterial.SwitchMaterial? = null
        var vibrationSwitchRef: com.google.android.material.switchmaterial.SwitchMaterial? = null
        var voiceNotificationsSwitchRef: com.google.android.material.switchmaterial.SwitchMaterial? = null
        var notificationsSwitchRef: com.google.android.material.switchmaterial.SwitchMaterial? = null
        var keepScreenAwakeSwitchRef: com.google.android.material.switchmaterial.SwitchMaterial? = null
        var startCountdownSwitchRef: com.google.android.material.switchmaterial.SwitchMaterial? = null

        fun applySelection(button: android.widget.ImageButton, isSelected: Boolean) {
            val strokeWidth = if (isSelected) 3 else 2
            val strokeColor = if (isSelected) {
                getAccentColor()
            } else {
                ContextCompat.getColor(this, R.color.stroke_light)
            }
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke((strokeWidth * resources.displayMetrics.density).toInt(), strokeColor)
            }
            button.background = bg
        }

        fun applyAccentSelections(selectedAccent: String) {
            applySelection(accentBlueButton, selectedAccent == ACCENT_BLUE)
            applySelection(accentTealButton, selectedAccent == ACCENT_TEAL)
            applySelection(accentPurpleButton, selectedAccent == ACCENT_PURPLE)
            applySelection(accentAmberButton, selectedAccent == ACCENT_AMBER)
            applySelection(accentMagentaButton, selectedAccent == ACCENT_MAGENTA)
        }

        fun applyDialogSwitchTints() {
            val thumbTint = createSwitchThumbTint()
            val trackTint = createSwitchTrackTint()
            saveWorkoutsSwitchRef?.thumbTintList = thumbTint
            saveWorkoutsSwitchRef?.trackTintList = trackTint
            vibrationSwitchRef?.thumbTintList = thumbTint
            vibrationSwitchRef?.trackTintList = trackTint
            voiceNotificationsSwitchRef?.thumbTintList = thumbTint
            voiceNotificationsSwitchRef?.trackTintList = trackTint
            notificationsSwitchRef?.thumbTintList = thumbTint
            notificationsSwitchRef?.trackTintList = trackTint
            keepScreenAwakeSwitchRef?.thumbTintList = thumbTint
            keepScreenAwakeSwitchRef?.trackTintList = trackTint
            startCountdownSwitchRef?.thumbTintList = thumbTint
            startCountdownSwitchRef?.trackTintList = trackTint
        }

        applySelection(themeSystemButton, currentThemeMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        applySelection(themeLightButton, currentThemeMode == AppCompatDelegate.MODE_NIGHT_NO)
        applySelection(themeDarkButton, currentThemeMode == AppCompatDelegate.MODE_NIGHT_YES)
        applyAccentSelections(getAccentStyle())
        
        themeSystemButton.setOnClickListener { btn ->
            hapticSelection(btn)
            setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            bottomSheetDialog.dismiss()
        }
        
        themeLightButton.setOnClickListener { btn ->
            hapticSelection(btn)
            setThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
            bottomSheetDialog.dismiss()
        }
        
        themeDarkButton.setOnClickListener { btn ->
            hapticSelection(btn)
            setThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
            bottomSheetDialog.dismiss()
        }

        fun setAccentStyle(style: String) {
            sharedPreferences.edit { putString(KEY_ACCENT_STYLE, style) }
            applyAccentStyling()
            applySelection(themeSystemButton, currentThemeMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            applySelection(themeLightButton, currentThemeMode == AppCompatDelegate.MODE_NIGHT_NO)
            applySelection(themeDarkButton, currentThemeMode == AppCompatDelegate.MODE_NIGHT_YES)
            applyAccentSelections(style)
            applyDialogSwitchTints()
            val currentState = intervalTimer?.state?.value
            if (currentState != null) {
                updateTimerDisplay(currentState)
            } else {
                updateUI()
            }
            updateFormulaDetails()
        }

        accentBlueButton.setOnClickListener { btn ->
            hapticSelection(btn)
            setAccentStyle(ACCENT_BLUE)
        }

        accentTealButton.setOnClickListener { btn ->
            hapticSelection(btn)
            setAccentStyle(ACCENT_TEAL)
        }

        accentPurpleButton.setOnClickListener { btn ->
            hapticSelection(btn)
            setAccentStyle(ACCENT_PURPLE)
        }

        accentAmberButton.setOnClickListener { btn ->
            hapticSelection(btn)
            setAccentStyle(ACCENT_AMBER)
        }

        accentMagentaButton.setOnClickListener { btn ->
            hapticSelection(btn)
            setAccentStyle(ACCENT_MAGENTA)
        }
        
        // Clear stats button
        val clearStatsButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.clearStatsButton)
        // Enable text wrapping for smaller screens
        clearStatsButton.maxLines = 2
        clearStatsButton.ellipsize = null
        clearStatsButton.setOnClickListener { btn ->
            hapticSelection(btn)
            showClearStatsConfirmationDialog(bottomSheetDialog)
        }

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.weeklyGoalSettingsButton)
            .setOnClickListener { btn ->
                hapticSelection(btn)
                bottomSheetDialog.dismiss()
                showWeeklyGoalEditor()
            }

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.insightCardsSettingsButton)
            .setOnClickListener { btn ->
                hapticSelection(btn)
                bottomSheetDialog.dismiss()
                showInsightCardsEditor()
            }

        // Vibration toggle switch
        val vibrationSwitch = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.vibrationSwitch)
        vibrationSwitchRef = vibrationSwitch
        vibrationSwitch.isChecked = sharedPreferences.getBoolean(KEY_VIBRATION_ENABLED, true)
        vibrationSwitch.thumbTintList = createSwitchThumbTint()
        vibrationSwitch.trackTintList = createSwitchTrackTint()
        vibrationSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            hapticSelection(buttonView)
            sharedPreferences.edit { putBoolean(KEY_VIBRATION_ENABLED, isChecked) }
        }

        // Voice notifications toggle switch
        val voiceNotificationsSwitch = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.voiceNotificationsSwitch)
        voiceNotificationsSwitchRef = voiceNotificationsSwitch
        voiceNotificationsSwitch.isChecked = sharedPreferences.getBoolean(KEY_VOICE_ENABLED, true)
        voiceNotificationsSwitch.thumbTintList = createSwitchThumbTint()
        voiceNotificationsSwitch.trackTintList = createSwitchTrackTint()
        voiceNotificationsSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            hapticSelection(buttonView)
            val previousEnabled = sharedPreferences.getBoolean(KEY_VOICE_ENABLED, true)
            if (isChecked == previousEnabled) return@setOnCheckedChangeListener

            if (notificationHelper == null) {
                notificationHelper = createNotificationHelper()
            }
            if (isChecked) {
                notificationHelper?.testTts()
            } else {
                notificationHelper?.speakStringRes(R.string.snackbar_voice_notifications_disabled)
            }

            sharedPreferences.edit { putBoolean(KEY_VOICE_ENABLED, isChecked) }
        }

        // Voice row: tap to pick notification voice
        val voiceRow = view.findViewById<View>(R.id.voiceRow)
        val voiceValue = view.findViewById<android.widget.TextView>(R.id.voiceValue)
        voiceValue.text = getTtsVoiceDisplayLabel()
        voiceRow.setOnClickListener {
            hapticSelection(it)
            showVoicePickerDialog(voiceValue)
        }

        // Save workouts toggle switch
        val saveWorkoutsSwitch = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.saveWorkoutsSwitch)
        saveWorkoutsSwitchRef = saveWorkoutsSwitch
        val saveWorkoutsEnabled = sharedPreferences.getBoolean(KEY_SAVE_WORKOUTS, true)
        saveWorkoutsSwitch.isChecked = saveWorkoutsEnabled
        saveWorkoutsSwitch.thumbTintList = createSwitchThumbTint()
        saveWorkoutsSwitch.trackTintList = createSwitchTrackTint()
        
        var isUpdatingSaveWorkoutsSwitch = false
        saveWorkoutsSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isUpdatingSaveWorkoutsSwitch) return@setOnCheckedChangeListener
            hapticSelection(buttonView)
            if (!isChecked) {
                // User is trying to turn off - show confirmation dialog
                isUpdatingSaveWorkoutsSwitch = true
                saveWorkoutsSwitch.isChecked = true // Revert immediately
                isUpdatingSaveWorkoutsSwitch = false
                showDisableSaveWorkoutsDialog(
                    onConfirm = {
                        sharedPreferences.edit { putBoolean(KEY_SAVE_WORKOUTS, false) }
                        isUpdatingSaveWorkoutsSwitch = true
                        saveWorkoutsSwitch.isChecked = false
                        isUpdatingSaveWorkoutsSwitch = false
                    }
                )
            } else {
                sharedPreferences.edit { putBoolean(KEY_SAVE_WORKOUTS, true) }
            }
        }

        val notificationsSwitch = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.notificationsSwitch)
        notificationsSwitchRef = notificationsSwitch
        settingsNotificationsSwitch = notificationsSwitch
        notificationsSwitch.thumbTintList = createSwitchThumbTint()
        notificationsSwitch.trackTintList = createSwitchTrackTint()
        refreshNotificationsSwitchState()
        notificationsSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isUpdatingNotificationsSwitch) return@setOnCheckedChangeListener
            hapticSelection(buttonView)
            val currentlyEnabled = areAppNotificationsEnabled()
            if (isChecked == currentlyEnabled) return@setOnCheckedChangeListener

            // This toggle reflects system notification state. We can request permission when enabling,
            // but disabling notifications must be done in system settings.
            if (isChecked) {
                requestNotificationPermissionOrOpenSettings()
            } else {
                openAppNotificationSettings()
            }
            refreshNotificationsSwitchState()
        }

        val keepScreenAwakeSwitch = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.keepScreenAwakeSwitch)
        keepScreenAwakeSwitchRef = keepScreenAwakeSwitch
        keepScreenAwakeSwitch.isChecked = sharedPreferences.getBoolean(KEY_KEEP_SCREEN_AWAKE, false)
        keepScreenAwakeSwitch.thumbTintList = createSwitchThumbTint()
        keepScreenAwakeSwitch.trackTintList = createSwitchTrackTint()
        keepScreenAwakeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            hapticSelection(buttonView)
            sharedPreferences.edit { putBoolean(KEY_KEEP_SCREEN_AWAKE, isChecked) }
            applyKeepScreenAwakePreference()
        }

        // Start countdown toggle switch
        val startCountdownSwitch = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.startCountdownSwitch)
        startCountdownSwitchRef = startCountdownSwitch
        val startCountdownEnabled = sharedPreferences.getBoolean(KEY_START_COUNTDOWN, true)
        startCountdownSwitch.isChecked = startCountdownEnabled
        startCountdownSwitch.thumbTintList = createSwitchThumbTint()
        startCountdownSwitch.trackTintList = createSwitchTrackTint()

        startCountdownSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            hapticSelection(buttonView)
            sharedPreferences.edit { putBoolean(KEY_START_COUNTDOWN, isChecked) }
            updateStartCountdownControlsState(view, isChecked)
        }

        val startCountdownValue = view.findViewById<android.widget.TextView>(R.id.startCountdownValue)
        val startCountdownDecrease = view.findViewById<android.widget.ImageButton>(R.id.startCountdownDecrease)
        val startCountdownIncrease = view.findViewById<android.widget.ImageButton>(R.id.startCountdownIncrease)
        updateStartCountdownValue(startCountdownValue)
        updateStartCountdownControlsState(view, startCountdownEnabled, startCountdownDecrease, startCountdownIncrease, startCountdownValue)

        startCountdownDecrease.setOnClickListener { btn ->
            hapticSelection(btn)
            adjustStartCountdownSeconds(startCountdownValue, -1)
        }

        startCountdownIncrease.setOnClickListener { btn ->
            hapticSelection(btn)
            adjustStartCountdownSeconds(startCountdownValue, 1)
        }
        
        bottomSheetDialog.show()
    }

    private fun showFaqDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_faq, android.widget.FrameLayout(this), false)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.setOnShowListener {
            val bottomSheet = bottomSheetDialog.findViewById<android.widget.FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
            bottomSheet?.requestLayout()
        }

        bottomSheetDialog.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val recyclerView = view.findViewById<RecyclerView>(R.id.faqRecyclerView)
            val basePaddingBottom = recyclerView.paddingBottom
            ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                recyclerView.updatePadding(bottom = basePaddingBottom + insets.bottom)
                windowInsets
            }
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.faqRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = FaqAdapter(faqEntries)
        recyclerView.setHasFixedSize(false)

        configureBottomSheet(bottomSheetDialog, view)
        bottomSheetDialog.show()
    }
    
    private fun setThemeMode(mode: Int) {
        sharedPreferences.edit { putInt(KEY_THEME_MODE, mode) }
        Handler(Looper.getMainLooper()).postDelayed({
            AppCompatDelegate.setDefaultNightMode(mode)
        }, 150)
    }
    
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        } catch (e: Exception) {
            // If no browser is available, show error (unlikely on Android)
            android.widget.Toast.makeText(this, "Unable to open link", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupControls() {
        homeActions.startPauseButton.setOnClickListener { view ->
            hapticSuccess(view)
            animateControlPress(homeActions.startPauseButton)
            if (isPreStartCountdownActive) {
                cancelPreStartCountdown(startImmediately = true)
            } else if (intervalTimer?.state?.value?.isRunning == true) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        homeActions.resetButton.setOnClickListener { view ->
            performHapticFeedback(view)
            animateControlPress(homeActions.resetButton)
            cancelPreStartCountdown()
            resetTimer()
        }

        // Initial UI state
        updateButtonStates()
    }

    private fun applyThemePreference() {
        // Default to system mode if no preference is set
        val savedThemeMode = sharedPreferences.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedThemeMode)
    }

    private fun getAccentStyle(): String {
        return sharedPreferences.getString(KEY_ACCENT_STYLE, ACCENT_BLUE) ?: ACCENT_BLUE
    }

    private fun getAccentColorRes(): Int {
        return when (getAccentStyle()) {
            ACCENT_TEAL -> R.color.accent_teal
            ACCENT_PURPLE -> R.color.accent_purple
            ACCENT_AMBER -> R.color.accent_amber
            ACCENT_MAGENTA -> R.color.accent_magenta
            else -> R.color.accent_blue
        }
    }

    private fun getAccentColor(): Int = ContextCompat.getColor(this, getAccentColorRes())

    private fun getSlowColor(): Int = getAccentColor()

    private fun getFastColor(): Int = getAccentColor()

    private fun createSwitchThumbTint(): android.content.res.ColorStateList {
        return android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                getAccentColor(),
                ContextCompat.getColor(this, android.R.color.darker_gray)
            )
        )
    }

    private fun createSwitchTrackTint(): android.content.res.ColorStateList {
        val accentWithAlpha = Color.argb(
            (255 * 0.5f).toInt(),
            Color.red(getAccentColor()),
            Color.green(getAccentColor()),
            Color.blue(getAccentColor())
        )
        val unchecked = if ((resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        ) {
            ContextCompat.getColor(this, R.color.stroke_light)
        } else {
            ContextCompat.getColor(this, android.R.color.darker_gray)
        }
        return android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(accentWithAlpha, unchecked)
        )
    }

    private fun applyAccentStyling() {
        val accentColor = getAccentColor()
        homeActions.startPauseButton.backgroundTintList = android.content.res.ColorStateList.valueOf(accentColor)
        homeSession.workoutProgress.progressTintList = android.content.res.ColorStateList.valueOf(accentColor)
        if (::homeInsightsController.isInitialized) {
            homeInsightsController.applyAccentColor()
        }
    }

    /**
     * Apply the runtime accent color to a filled MaterialButton's background.
     * The framework defaults filled buttons to ?attr/colorPrimary, but this app
     * stores the accent in prefs and tints buttons programmatically (see Create button).
     */
    private fun tintFilledButtonWithAccent(button: MaterialButton) {
        val accent = android.content.res.ColorStateList.valueOf(getAccentColor())
        button.backgroundTintList = accent
        button.setTextColor(ContextCompat.getColor(this, R.color.white))
        button.iconTint = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.white)
        )
    }

    /**
     * Apply the runtime accent color to an outlined MaterialButton (stroke + text + ripple).
     */
    private fun tintOutlinedButtonWithAccent(button: MaterialButton) {
        val accent = android.content.res.ColorStateList.valueOf(getAccentColor())
        button.strokeColor = accent
        button.setTextColor(accent)
        button.iconTint = accent
        button.rippleColor = accent
    }

    /**
     * Apply the runtime accent color to a text-only MaterialButton (text + icon + ripple).
     */
    private fun tintTextButtonWithAccent(button: MaterialButton) {
        val accent = android.content.res.ColorStateList.valueOf(getAccentColor())
        button.setTextColor(accent)
        button.iconTint = accent
        button.rippleColor = accent
    }

    private fun applyKeepScreenAwakePreference() {
        if (sharedPreferences.getBoolean(KEY_KEEP_SCREEN_AWAKE, false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    private fun startTimer() {
        if (intervalTimer == null) {
            intervalTimer = createIntervalTimer()
            observeTimerState()
        }

        val state = intervalTimer?.state?.value
        val startCountdownEnabled = sharedPreferences.getBoolean(KEY_START_COUNTDOWN, true)
        if (startCountdownEnabled && state != null && state.elapsedSeconds == 0 && !state.isRunning && !isPreStartCountdownActive) {
            startPreStartCountdown()
            return
        }

        startTimerNow()
    }

    private fun startTimerNow() {
        cancelCompletionAutoReset(clearCompletionTimestamp = true)
        // Acquire wake lock to keep device awake during timer
        acquireWakeLock()
        startWorkoutForegroundService()
        hasShownCompletionConfetti = false
        intervalTimer?.start()
        
        updateButtonStates()
    }

    private fun pauseTimer() {
        intervalTimer?.pause()
        // Release wake lock when paused
        releaseWakeLock()
        stopWorkoutForegroundService()
        updateButtonStates()
    }

    private fun startPreStartCountdown(initialMillis: Long? = null) {
        if (isPreStartCountdownActive) return
        isPreStartCountdownActive = true
        preStartCountdownTimer?.cancel()
        val totalMillis = initialMillis ?: (getStartCountdownSeconds() * 1000L)
        preStartCountdownEndElapsedRealtime = SystemClock.elapsedRealtime() + totalMillis
        val initialSecondsLeft = ((totalMillis + 999) / 1000).toInt().coerceAtLeast(1)
        binding.preStartCountdown.text = String.format(Locale.getDefault(), "%d", initialSecondsLeft)
        binding.preStartOverlay.visibility = View.VISIBLE
        binding.preStartOverlay.setOnClickListener {
            cancelPreStartCountdown(startImmediately = true)
        }

        val useVibration = sharedPreferences.getBoolean(KEY_VIBRATION_ENABLED, true)
        val useVoice = sharedPreferences.getBoolean(KEY_VOICE_ENABLED, true)
        if (useVoice && notificationHelper == null) {
            notificationHelper = createNotificationHelper()
        }

        preStartCountdownTimer = object : CountDownTimer(totalMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = ((millisUntilFinished + 999) / 1000).toInt()
                binding.preStartCountdown.text = String.format(Locale.getDefault(), "%d", secondsLeft)
                if (useVibration) {
                    hapticSelection(binding.root)
                }
                if (useVoice) {
                    notificationHelper?.speak(secondsLeft.toString())
                }
            }

            override fun onFinish() {
                binding.preStartCountdown.text = getString(R.string.action_go)
                if (useVibration) {
                    hapticSuccess(binding.root)
                }
                if (useVoice) {
                    notificationHelper?.speakStringRes(R.string.action_go)
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    cancelPreStartCountdown(startImmediately = true)
                }, 800)
            }
        }.start()

        updateButtonStates()
    }

    private fun cancelPreStartCountdown(startImmediately: Boolean = false) {
        preStartCountdownTimer?.cancel()
        preStartCountdownTimer = null
        isPreStartCountdownActive = false
        preStartCountdownEndElapsedRealtime = 0L
        binding.preStartOverlay.visibility = View.GONE
        binding.preStartOverlay.setOnClickListener(null)
        if (startImmediately) {
            startTimerNow()
        } else {
            updateButtonStates()
        }
    }

    private fun getStartCountdownSeconds(): Int {
        val stored = sharedPreferences.getInt(KEY_START_COUNTDOWN_SECONDS, PRE_START_SECONDS_DEFAULT)
        return stored.coerceIn(PRE_START_SECONDS_MIN, PRE_START_SECONDS_MAX)
    }

    private fun updateStartCountdownValue(valueView: android.widget.TextView) {
        val seconds = getStartCountdownSeconds()
        valueView.text = getString(R.string.format_countdown_seconds, seconds)
    }

    private fun adjustStartCountdownSeconds(valueView: android.widget.TextView, delta: Int) {
        val current = getStartCountdownSeconds()
        val next = (current + delta).coerceIn(PRE_START_SECONDS_MIN, PRE_START_SECONDS_MAX)
        if (next != current) {
            sharedPreferences.edit { putInt(KEY_START_COUNTDOWN_SECONDS, next) }
            updateStartCountdownValue(valueView)
        }
    }

    private fun updateStartCountdownControlsState(
        view: View, 
        enabled: Boolean,
        decreaseBtn: android.widget.ImageButton? = null,
        increaseBtn: android.widget.ImageButton? = null,
        valueText: android.widget.TextView? = null
    ) {
        val alpha = if (enabled) 1f else 0.4f
        val decrease = decreaseBtn ?: view.findViewById<android.widget.ImageButton>(R.id.startCountdownDecrease)
        val increase = increaseBtn ?: view.findViewById<android.widget.ImageButton>(R.id.startCountdownIncrease)
        val value = valueText ?: view.findViewById<android.widget.TextView>(R.id.startCountdownValue)
        
        decrease?.apply {
            this.alpha = alpha
            isEnabled = enabled
        }
        increase?.apply {
            this.alpha = alpha
            isEnabled = enabled
        }
        value?.alpha = alpha
    }

    private fun resetTimer() {
        cancelCompletionAutoReset(clearCompletionTimestamp = true)
        cancelPreStartCountdown()
        intervalTimer?.dispose()
        intervalTimer = null
        // Release wake lock when reset
        releaseWakeLock()
        stopWorkoutForegroundService()
        hasShownCompletionConfetti = false

        intervalTimer = createIntervalTimer()
        observeTimerState()

        updateButtonStates()
        updateUI()
    }

    private fun updateTimerDisplay(state: TimerState) {
        val newTime = state.timeRemainingSeconds
        val timeChanged = newTime != lastDisplayedTime
        
        if (timeChanged && state.isRunning) {
            // Add subtle pulse animation when time changes during active workout
            animateCountdownUpdate()
        }

        if (state.currentPhase is IntervalPhase.Completed && !hasShownCompletionConfetti) {
            hasShownCompletionConfetti = true
            hapticSuccess(binding.root)
            binding.confettiView.launch()
        }
        
        homeSession.timeDisplay.text = formatTime(newTime)
        homeSession.intervalCounter.text = formatIntervalCounter(state.currentInterval, state.totalIntervals)
        updatePhaseDisplay(state.currentPhase)
        updateWorkoutProgress(state)
        updateButtonStates()
        
        lastDisplayedTime = newTime
    }
    
    private fun animateCountdownUpdate() {
        // Subtle scale animation for countdown updates
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.05f, 1.0f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.05f, 1.0f)
        val animator = ObjectAnimator.ofPropertyValuesHolder(homeSession.timeDisplay, scaleX, scaleY)
        animator.duration = 300
        animator.interpolator = DecelerateInterpolator()
        animator.start()
    }

    private fun updatePhaseDisplay(phase: IntervalPhase) {
        if (phase != lastDisplayedPhase) {
            animatePhaseTransition()
            lastDisplayedPhase = phase
        }

        when (phase) {
            is IntervalPhase.Slow -> {
                phaseLabel.text = getString(
                    R.string.format_phase_slow,
                    getString(R.string.label_phase_slow)
                )
                phaseLabel.setTextColor(getSlowColor())
                phaseLabel.setTypeface(null, Typeface.BOLD)
            }
            is IntervalPhase.Fast -> {
                phaseLabel.text = getString(
                    R.string.format_phase_fast,
                    getString(R.string.label_phase_fast)
                )
                phaseLabel.setTextColor(getFastColor())
                phaseLabel.setTypeface(null, Typeface.BOLD)
            }
            is IntervalPhase.Completed -> {
                phaseLabel.text = getString(R.string.label_completed)
                phaseLabel.setTextColor(getAccentColor())
                phaseLabel.setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun animatePhaseTransition() {
        val phaseScaleX = PropertyValuesHolder.ofFloat("scaleX", 0.96f, 1.0f)
        val phaseScaleY = PropertyValuesHolder.ofFloat("scaleY", 0.96f, 1.0f)
        val phaseAlpha = PropertyValuesHolder.ofFloat("alpha", 0.7f, 1.0f)
        ObjectAnimator.ofPropertyValuesHolder(homeSession.phaseLabel, phaseScaleX, phaseScaleY, phaseAlpha).apply {
            duration = 180
            interpolator = DecelerateInterpolator()
            start()
        }

        val timeScaleX = PropertyValuesHolder.ofFloat("scaleX", 0.98f, 1.0f)
        val timeScaleY = PropertyValuesHolder.ofFloat("scaleY", 0.98f, 1.0f)
        ObjectAnimator.ofPropertyValuesHolder(homeSession.timeDisplay, timeScaleX, timeScaleY).apply {
            duration = 180
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun updateWorkoutProgress(state: TimerState) {
        val totalDuration = currentFormula.totalDurationSeconds
        if (totalDuration > 0) {
            val progress = ((state.elapsedSeconds.toFloat() / totalDuration) * 100).toInt().coerceIn(0, 100)
            homeSession.workoutProgress.progress = progress
            
            // Update elapsed and remaining time displays
            val elapsedSeconds = state.elapsedSeconds.coerceAtMost(totalDuration)
            val remainingSeconds = (totalDuration - elapsedSeconds).coerceAtLeast(0)
            
            homeSession.elapsedTime.text = formatTime(elapsedSeconds)
            homeSession.remainingTime.text = formatTime(remainingSeconds)
        } else {
            homeSession.workoutProgress.progress = 0
            homeSession.elapsedTime.text = formatTime(0)
            homeSession.remainingTime.text = formatTime(0)
        }
    }

    private fun updateUI() {
        val state = intervalTimer?.state?.value ?: run {
            val initialTime = if (currentFormula.startsWithFast) currentFormula.fastDurationSeconds else currentFormula.slowDurationSeconds
            val initialPhase = if (currentFormula.startsWithFast) IntervalPhase.Fast else IntervalPhase.Slow
            TimerState(
                currentPhase = initialPhase,
                totalIntervals = currentFormula.totalIntervals,
                timeRemainingSeconds = initialTime,
                elapsedSeconds = 0
            )
        }
        homeSession.timeDisplay.text = formatTime(state.timeRemainingSeconds)
        homeSession.intervalCounter.text = formatIntervalCounter(0, currentFormula.totalIntervals)
        updatePhaseDisplay(state.currentPhase) // Fixed: Update phase label based on current formula
        updateWorkoutProgress(state)
        updateFormulaDetails()
    }

    private fun formatDurationMinSec(totalSeconds: Int): String {
        val min = totalSeconds / 60
        val sec = totalSeconds % 60
        return when {
            min > 0 && sec > 0 -> getString(R.string.format_time_m_s, min, sec)
            min > 0 -> getString(R.string.format_time_m, min)
            else -> getString(R.string.format_time_s, sec)
        }
    }

    private fun updateFormulaDetails() {
        val totalMin = currentFormula.totalDurationSeconds / 60
        val slowText = formatDurationMinSec(currentFormula.slowDurationSeconds)
        val fastText = formatDurationMinSec(currentFormula.fastDurationSeconds)
        
        // Build pattern description showing execution pattern
        val isCircuit = currentFormula.isCircuit
        val isHighIntensity = currentFormula.name.contains("5-2", ignoreCase = true)
        val isCustom = currentFormula.name.startsWith("Custom:")
        
        val pattern = when {
            isCircuit -> {
                // Circuit pattern: can be Fast-Slow-Fast or Slow-Fast-Slow
                val circuits = currentFormula.totalIntervals / 2
                val circuitPattern = if (currentFormula.startsWithFast) {
                    // Fast-Slow-Fast pattern
                    if (circuits == 1) {
                        getString(R.string.format_pattern_fast_slow_fast, fastText, slowText)
                    } else {
                        getString(R.string.format_pattern_fast_slow_fast_rounds, fastText, slowText, circuits)
                    }
                } else {
                    // Slow-Fast-Slow pattern
                    if (circuits == 1) {
                        getString(R.string.format_pattern_slow_fast_slow, slowText, fastText)
                    } else {
                        getString(R.string.format_pattern_slow_fast_slow_rounds, slowText, fastText, circuits)
                    }
                }
                circuitPattern
            }
            isHighIntensity -> {
                // High Intensity 5-2 pattern: Fast(5) → Slow(2) × rounds
                getString(R.string.format_pattern_fast_slow_rounds, fastText, slowText, currentFormula.totalIntervals)
            }
            isCustom && currentFormula.startsWithFast -> {
                // Custom formula starting with fast
                if (currentFormula.totalIntervals == 1) {
                    getString(R.string.format_pattern_fast_slow, fastText, slowText)
                } else {
                    getString(
                        R.string.format_pattern_fast_slow_rounds,
                        fastText,
                        slowText,
                        currentFormula.totalIntervals
                    )
                }
            }
            currentFormula.totalIntervals == 1 ->
                getString(R.string.format_pattern_slow_fast, slowText, fastText)
            else ->
                getString(
                    R.string.format_pattern_slow_fast_rounds,
                    slowText,
                    fastText,
                    currentFormula.totalIntervals
                )
        }
        
        // Update styled text with colored note
        val fullText = getString(R.string.format_formula_summary, pattern, totalMin)
        val startNoteText = if (currentFormula.startsWithFast) {
            getString(R.string.label_starts_fast)
        } else {
            getString(R.string.label_starts_slow)
        }
        
        val combinedText = getString(R.string.format_formula_full_text, fullText, startNoteText)
        val spannable = SpannableString(combinedText)
        val noteStart = fullText.length + 1
        val noteEnd = combinedText.length
        
        // Keep the note weight for emphasis without accent-color distraction.
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            noteStart,
            noteEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        homeWorkoutSetup.formulaDetails.text = spannable
    }

    private fun updateButtonStates() {
        val state = intervalTimer?.state?.value
        val isRunning = state?.isRunning == true
        homeActions.startPauseButton.text = when {
            isRunning -> getString(R.string.action_pause)
            state != null && state.elapsedSeconds > 0 && state.currentPhase !is IntervalPhase.Completed ->
                getString(R.string.action_resume)
            else -> getString(R.string.action_start)
        }
    }

    private fun animateControlPress(target: View) {
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 0.97f, 1.0f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 0.97f, 1.0f)
        ObjectAnimator.ofPropertyValuesHolder(target, scaleX, scaleY).apply {
            duration = 140
            interpolator = DecelerateInterpolator()
            start()
        }
    }
    
    /**
     * Records a completed workout in the database.
     */
    private fun recordWorkoutCompletion() {
        // Check if workout saving is enabled
        val saveWorkoutsEnabled = sharedPreferences.getBoolean(KEY_SAVE_WORKOUTS, true)
        if (!saveWorkoutsEnabled) {
            android.util.Log.d("MainActivity", "Workout saving is disabled, skipping record")
            return
        }
        
        // Use formula's total duration since state might not be updated yet when called early
        val totalSeconds = currentFormula.totalDurationSeconds
        if (totalSeconds > 0) {
            val minutes = (totalSeconds / 60).coerceAtLeast(1) // At least 1 minute
            val workoutType = currentFormula.name
            lifecycleScope.launch {
                try {
                    workoutRepository.recordWorkout(minutes, workoutType)
                    homeInsightsController.load()
                    WeeklyReminderScheduler(this@MainActivity).scheduleNextReminder()
                    android.util.Log.d("MainActivity", "Workout recorded: $minutes minutes, type: $workoutType")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error recording workout", e)
                }
            }
        }
    }
    
    private fun getTtsVoiceDisplayLabel(): String {
        val localeTag = sharedPreferences.getString(KEY_TTS_VOICE_LOCALE, null).orEmpty()
        val name = sharedPreferences.getString(KEY_TTS_VOICE, null).orEmpty()
        val display = sharedPreferences.getString(KEY_TTS_VOICE_DISPLAY, null).orEmpty()
        return when {
            name.isEmpty() && localeTag.isEmpty() -> getString(R.string.option_voice_default)
            display.isNotBlank() -> display
            localeTag.isNotBlank() -> Locale.forLanguageTag(localeTag).getDisplayName()
            else -> name
        }
    }

    private data class VoiceLanguageOption(
        val localeTag: String,
        val displayName: String,
        val voiceName: String
    )

    private fun normalizeToSupportedLocaleTag(locale: Locale): String? {
        val language = locale.language.lowercase(Locale.ROOT)
        if (language.isBlank()) return null
        val country = locale.country.uppercase(Locale.ROOT)
        val candidates = mutableListOf<String>()
        if (country.isNotBlank()) {
            when {
                language == "pt" && country == "PT" -> candidates.add("pt-PT")
                language == "zh" && country == "HK" -> candidates.add("zh-HK")
                language == "zh" && country == "CN" -> candidates.add("zh-CN")
                else -> candidates.add("$language-$country")
            }
        }
        candidates.add(language)
        return candidates.firstOrNull { tag -> SUPPORTED_TTS_LOCALE_TAGS.contains(tag) }
    }

    private fun buildVoiceLanguageOptions(voices: List<Voice>): List<VoiceLanguageOption> {
        val sortedVoices = voices.sortedWith(
            compareBy<Voice>(
                { it.locale.getDisplayName() },
                { it.name.lowercase(Locale.ROOT) }
            )
        )

        val options = linkedMapOf<String, VoiceLanguageOption>()
        sortedVoices.forEach { voice ->
            val localeTag = normalizeToSupportedLocaleTag(voice.locale) ?: return@forEach
            if (!options.containsKey(localeTag)) {
                options[localeTag] = VoiceLanguageOption(
                    localeTag = localeTag,
                    displayName = Locale.forLanguageTag(localeTag).getDisplayName(),
                    voiceName = voice.name
                )
            }
        }

        return options.values.sortedBy { it.displayName.lowercase(Locale.ROOT) }
    }

    private fun showVoicePickerDialog(voiceValueView: android.widget.TextView) {
        var tempTts: TextToSpeech? = null
        tempTts = TextToSpeech(this) { status ->
            runOnUiThread {
                if (status != TextToSpeech.SUCCESS) {
                    tempTts?.shutdown()
                    val msg = getString(R.string.body_no_voices_available)
                    android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val tts = tempTts ?: return@runOnUiThread
                val voices = tts.voices?.toList() ?: emptyList()
                val voiceOptions = buildVoiceLanguageOptions(voices)
                if (voiceOptions.isEmpty()) {
                    tts.shutdown()
                    val msg = getString(R.string.body_no_voices_available)
                    android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val displayNames =
                    listOf(getString(R.string.option_voice_default)) + voiceOptions.map { it.displayName }
                val voiceNames = listOf("") + voiceOptions.map { it.voiceName }
                val voiceLocaleTags = listOf("") + voiceOptions.map { it.localeTag }
                val currentName = sharedPreferences.getString(KEY_TTS_VOICE, null).orEmpty()
                val currentLocaleTag = sharedPreferences.getString(KEY_TTS_VOICE_LOCALE, null).orEmpty()
                var selectedIndex = if (currentLocaleTag.isNotBlank()) {
                    voiceLocaleTags.indexOf(currentLocaleTag)
                } else {
                    -1
                }
                if (selectedIndex < 0 && currentName.isNotBlank()) {
                    selectedIndex = voiceNames.indexOf(currentName)
                }
                if (selectedIndex < 0) selectedIndex = 0
                showVoicePickerBottomSheet(
                    voiceValueView = voiceValueView,
                    displayNames = displayNames,
                    voiceNames = voiceNames,
                    voiceLocaleTags = voiceLocaleTags,
                    selectedIndex = selectedIndex,
                    tempTts = tts
                )
            }
        }
    }

    private fun showVoicePickerBottomSheet(
        voiceValueView: android.widget.TextView,
        displayNames: List<String>,
        voiceNames: List<String>,
        voiceLocaleTags: List<String>,
        selectedIndex: Int,
        tempTts: TextToSpeech
    ) {
        var pendingSelectionIndex = selectedIndex
        val bottomSheetDialog = BottomSheetDialog(this)
        val contentView = LayoutInflater.from(this).inflate(
            R.layout.bottom_sheet_voice_picker,
            android.widget.FrameLayout(this),
            false
        )
        bottomSheetDialog.setContentView(contentView)
        configureVoicePickerBottomSheet(bottomSheetDialog, contentView)

        val voicePickerList = contentView.findViewById<android.widget.ListView>(R.id.voicePickerList)
        val cancelButton = contentView.findViewById<com.google.android.material.button.MaterialButton>(
            R.id.cancelVoiceSelectionButton
        )
        val applyButton = contentView.findViewById<com.google.android.material.button.MaterialButton>(
            R.id.applyVoiceSelectionButton
        )

        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_list_item_single_choice,
            displayNames
        )
        voicePickerList.adapter = adapter
        voicePickerList.choiceMode = android.widget.ListView.CHOICE_MODE_SINGLE
        voicePickerList.setItemChecked(selectedIndex, true)
        voicePickerList.setOnItemClickListener { _, _, which, _ ->
            pendingSelectionIndex = which
        }

        cancelButton.setOnClickListener { bottomSheetDialog.dismiss() }
        applyButton.setOnClickListener {
            applyVoicePickerSelection(
                voiceValueView = voiceValueView,
                name = voiceNames[pendingSelectionIndex],
                localeTag = voiceLocaleTags[pendingSelectionIndex],
                displayName = displayNames[pendingSelectionIndex]
            )
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setOnDismissListener {
            tempTts.shutdown()
        }
        bottomSheetDialog.show()
    }

    private fun configureVoicePickerBottomSheet(dialog: BottomSheetDialog, contentView: View) {
        val behavior = dialog.behavior
        behavior.isFitToContents = true
        behavior.isDraggable = false
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        contentView.findViewById<View>(R.id.voice_picker_drag_handle)?.visibility = View.GONE
    }

    private fun applyVoicePickerSelection(
        voiceValueView: android.widget.TextView,
        name: String,
        localeTag: String,
        displayName: String
    ) {
        sharedPreferences.edit {
            putString(KEY_TTS_VOICE, name)
            putString(KEY_TTS_VOICE_LOCALE, localeTag)
            putString(KEY_TTS_VOICE_DISPLAY, if (name.isEmpty()) "" else displayName)
        }
        voiceValueView.text = getTtsVoiceDisplayLabel()
        notificationHelper?.release()
        notificationHelper = null
        if (sharedPreferences.getBoolean(KEY_VOICE_ENABLED, true)) {
            notificationHelper = createNotificationHelper()
            notificationHelper?.testTts()
        }
    }

    private fun showClearStatsConfirmationDialog(parentDialog: BottomSheetDialog) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.title_clear_workout_history)
            .setMessage(R.string.message_clear_workout_history)
            .setPositiveButton(R.string.action_clear) { _, _ ->
                clearAllStats()
                parentDialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
    
    private fun clearAllStats() {
        lifecycleScope.launch {
            try {
                workoutRepository.clearAllData()
                kotlinx.coroutines.delay(100)
                android.util.Log.d("MainActivity", "All workout stats cleared")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error clearing stats", e)
            }
        }
    }

    private fun scheduleCompletionAutoReset() {
        completionAtMillis = System.currentTimeMillis()
        val runnable = Runnable {
            val state = intervalTimer?.state?.value
            if (state?.currentPhase is IntervalPhase.Completed && state.isRunning.not()) {
                resetTimer()
            }
        }
        completionAutoResetRunnable?.let { mainHandler.removeCallbacks(it) }
        completionAutoResetRunnable = runnable
        mainHandler.postDelayed(runnable, AUTO_RESET_AFTER_COMPLETION_DELAY_MS)
    }

    private fun cancelCompletionAutoReset(clearCompletionTimestamp: Boolean) {
        completionAutoResetRunnable?.let { mainHandler.removeCallbacks(it) }
        completionAutoResetRunnable = null
        if (clearCompletionTimestamp) {
            completionAtMillis = null
        }
    }

    private fun maybeAutoResetCompletedTimer() {
        val state = intervalTimer?.state?.value ?: return
        if (state.currentPhase !is IntervalPhase.Completed || state.isRunning) return

        val completedAt = completionAtMillis
        if (completedAt == null) {
            // Treat as just-completed when timestamp isn't available.
            scheduleCompletionAutoReset()
            return
        }

        val elapsed = System.currentTimeMillis() - completedAt
        if (elapsed >= AUTO_RESET_AFTER_COMPLETION_DELAY_MS) {
            resetTimer()
        } else {
            val remaining = AUTO_RESET_AFTER_COMPLETION_DELAY_MS - elapsed
            val runnable = Runnable {
                val current = intervalTimer?.state?.value
                if (current?.currentPhase is IntervalPhase.Completed && current.isRunning.not()) {
                    resetTimer()
                }
            }
            completionAutoResetRunnable?.let { mainHandler.removeCallbacks(it) }
            completionAutoResetRunnable = runnable
            mainHandler.postDelayed(runnable, remaining)
        }
    }

    private fun showDisableSaveWorkoutsDialog(onConfirm: () -> Unit, onCancel: (() -> Unit)? = null) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.prompt_disable_save_workouts)
            .setMessage(R.string.message_disable_save_workouts)
            .setPositiveButton(R.string.action_turn_off) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(R.string.action_keep_on) { _, _ ->
                onCancel?.invoke()
            }
            .setOnCancelListener {
                onCancel?.invoke()
            }
            .show()
    }

    /**
     * Formats seconds as MM:SS or H:MM:SS for longer durations.
     * 
     * @param seconds The number of seconds to format
     * @return Formatted time string in MM:SS format (e.g., "03:00", "01:30") or H:MM:SS for hours (e.g., "1:05:30")
     */
    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, secs)
        }
    }

    /**
     * Formats the interval counter display.
     * For 5-4-5 circuit patterns, converts intervals to rounds (each round = 2 intervals).
     * For other patterns, displays intervals as-is.
     * 
     * @param currentInterval Current interval number (1-indexed)
     * @param totalIntervals Total number of intervals
     * @return Formatted string like "1 / 2" for rounds or "1 / 5" for intervals
     */
    private fun formatIntervalCounter(currentInterval: Int, totalIntervals: Int): String {
        val isCircuit = currentFormula.isCircuit
        return if (isCircuit) {
            val totalRounds = totalIntervals / 2
            val currentRound = if (currentInterval <= 0) 1
            else minOf(((currentInterval - 1) / 2) + 1, totalRounds).coerceAtLeast(1)
            "$currentRound / $totalRounds"
        } else {
            "$currentInterval / $totalIntervals"
        }
    }

    /**
     * Acquires a wake lock to keep the device awake during timer execution.
     * This ensures accurate timing even when the screen is off.
     * The wake lock is automatically released when the timer is paused, reset, or completed.
     */
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG
            ).apply {
                acquire(WAKE_LOCK_TIMEOUT_MS)
            }
        }
    }

    /**
     * Releases the wake lock if it's currently held.
     * Safe to call multiple times.
     */

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
            wakeLock = null
        }
    }

    private fun startWorkoutForegroundService() {
        if (needsActivityRecognitionForHealthForegroundService() && !hasActivityRecognitionPermission()) {
            shouldStartForegroundServiceAfterPermission = true
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                REQUEST_CODE_ACTIVITY_RECOGNITION
            )
            android.widget.Toast.makeText(
                this,
                getString(R.string.snackbar_permission_activity_recognition_needed),
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        val intent = Intent(this, WorkoutForegroundService::class.java).apply {
            action = WorkoutForegroundService.ACTION_START
        }
        try {
            ContextCompat.startForegroundService(this, intent)
        } catch (_: SecurityException) {
            // Guard against API 34+ foreground service permission edge-cases.
            android.widget.Toast.makeText(
                this,
                getString(R.string.snackbar_permission_activity_recognition_denied),
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun stopWorkoutForegroundService() {
        shouldStartForegroundServiceAfterPermission = false
        val intent = Intent(this, WorkoutForegroundService::class.java).apply {
            action = WorkoutForegroundService.ACTION_STOP
        }
        stopService(intent)
    }

    private fun needsActivityRecognitionForHealthForegroundService(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }

    private fun hasActivityRecognitionPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun areAppNotificationsEnabled(): Boolean {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            return false
        }
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun refreshNotificationsSwitchState() {
        val switch = settingsNotificationsSwitch ?: return
        isUpdatingNotificationsSwitch = true
        switch.isChecked = areAppNotificationsEnabled()
        isUpdatingNotificationsSwitch = false
    }

    private fun handleNotificationsEnabledTransition(previousEnabled: Boolean, currentEnabled: Boolean) {
        // Re-post the foreground notification when notifications are re-enabled
        // during an active workout so users immediately see ongoing workout status.
        if (!previousEnabled && currentEnabled && intervalTimer?.state?.value?.isRunning == true) {
            startWorkoutForegroundService()
        }
    }

    private fun requestNotificationPermissionOrOpenSettings() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_POST_NOTIFICATIONS
            )
            return
        }
        openAppNotificationSettings()
    }

    @SuppressLint("InlinedApi")
    private fun openAppNotificationSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
            }
            startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            val notificationsEnabled = areAppNotificationsEnabled()
            handleNotificationsEnabledTransition(lastKnownNotificationsEnabled, notificationsEnabled)
            lastKnownNotificationsEnabled = notificationsEnabled
            refreshNotificationsSwitchState()
            if (notificationsEnabled) {
                requestExactAlarmAccessForWeeklyRemindersIfNeeded()
            }
            return
        }
        if (requestCode != REQUEST_CODE_ACTIVITY_RECOGNITION) return

        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (granted && shouldStartForegroundServiceAfterPermission && intervalTimer?.state?.value?.isRunning == true) {
            shouldStartForegroundServiceAfterPermission = false
            startWorkoutForegroundService()
            return
        }

        shouldStartForegroundServiceAfterPermission = false
        if (!granted) {
            android.widget.Toast.makeText(
                this,
                getString(R.string.snackbar_permission_activity_recognition_denied),
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun requestExactAlarmAccessForWeeklyRemindersIfNeeded() {
        val goalSettings = WeeklyGoalPreferences.loadGoalSettings(sharedPreferences)
        val reminderSettings = WeeklyGoalPreferences.loadReminderSettings(sharedPreferences)
        val shouldRequest = reminderSettings.enabled &&
            goalSettings.remindersAvailable() &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            !WeeklyReminderScheduler(this).canScheduleExactReminders()
        if (shouldRequest) {
            openExactAlarmSettings()
            android.widget.Toast.makeText(
                this,
                R.string.toast_allow_exact_reminders,
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openExactAlarmSettings() {
        val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:$packageName")
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            val fallbackIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(fallbackIntent)
        }
    }

    /**
     * Factory method for creating NotificationHelper. Can be overridden in tests.
     */
    protected open fun createNotificationHelper(): NotificationHelper {
        return NotificationHelper(this, sharedPreferences, KEY_TTS_VOICE, KEY_TTS_VOICE_LOCALE)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelCompletionAutoReset(clearCompletionTimestamp = false)
        preStartCountdownTimer?.cancel()
        intervalTimer?.dispose()
        notificationHelper?.release()
        notificationHelper = null
        releaseWakeLock()
        stopWorkoutForegroundService()
    }
}

