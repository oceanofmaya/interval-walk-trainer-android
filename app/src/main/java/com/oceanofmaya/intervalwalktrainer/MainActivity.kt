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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    private var lastDisplayedTime = -1
    private var lastDisplayedPhase: IntervalPhase? = null
    private var hasShownCompletionConfetti = false
    private var preStartCountdownTimer: CountDownTimer? = null
    private var isPreStartCountdownActive = false
    private var shouldStartForegroundServiceAfterPermission = false
    private var settingsNotificationsSwitch: com.google.android.material.switchmaterial.SwitchMaterial? = null
    private var isUpdatingNotificationsSwitch = false
    private var lastKnownNotificationsEnabled = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var completionAtMillis: Long? = null
    private var completionAutoResetRunnable: Runnable? = null

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
            FaqEntry(R.string.faq_question_data_shared, R.string.faq_answer_data_shared),
            FaqEntry(R.string.faq_question_notifications, R.string.faq_answer_notifications),
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
        private const val KEY_CUSTOM_FORMULA_MODE = "custom_formula_mode" // "circuit" or "interval"
        
        // Saved state keys
        private const val KEY_SAVED_FORMULA_NAME = "saved_formula_name"
        private const val KEY_SAVED_TIME_REMAINING = "saved_time_remaining"
        private const val KEY_SAVED_CURRENT_INTERVAL = "saved_current_interval"
        private const val KEY_SAVED_IS_RUNNING = "saved_is_running"
        private const val KEY_SAVED_PHASE = "saved_phase"
        private const val KEY_SAVED_COMPLETION_AT_MILLIS = "saved_completion_at_millis"
        
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

        restoreCustomFormula()
        setupFormulaSpinner()
        setupControls()
        applyAccentStyling()
        setupOverflowMenuButton()
        setupWorkoutHistoryButton()
        setupSettingsButton()
        lastKnownNotificationsEnabled = areAppNotificationsEnabled()
        
        // Restore timer state if activity was recreated (e.g., theme change)
        if (savedInstanceState != null) {
            restoreTimerState(savedInstanceState)
        } else {
            updateUI()
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
    }

    override fun onPause() {
        super.onPause()
        // Ensure this preference is inert once the app leaves the foreground.
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    
    /**
     * Sets up window insets to handle safe areas for edge-to-edge screens.
     * Applies top padding to account for status bar overlap.
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
            outState.putString(KEY_SAVED_PHASE, when (timerState.currentPhase) {
                is IntervalPhase.Slow -> "slow"
                is IntervalPhase.Fast -> "fast"
                is IntervalPhase.Completed -> "completed"
            })
            outState.putLong(KEY_SAVED_COMPLETION_AT_MILLIS, completionAtMillis ?: -1L)
        }
    }
    
    private fun restoreTimerState(savedInstanceState: Bundle) {
        val savedFormulaName = savedInstanceState.getString(KEY_SAVED_FORMULA_NAME)
        val savedTimeRemaining = savedInstanceState.getInt(KEY_SAVED_TIME_REMAINING, -1)
        val savedCurrentInterval = savedInstanceState.getInt(KEY_SAVED_CURRENT_INTERVAL, 0)
        val savedIsRunning = savedInstanceState.getBoolean(KEY_SAVED_IS_RUNNING, false)
        val savedPhase = savedInstanceState.getString(KEY_SAVED_PHASE, "slow")
        val savedCompletionAt = savedInstanceState.getLong(KEY_SAVED_COMPLETION_AT_MILLIS, -1L)
        completionAtMillis = if (savedCompletionAt > 0) savedCompletionAt else null
        
        // Only restore if we have valid saved state
        if (savedFormulaName != null && savedTimeRemaining >= 0) {
            // Try to find the formula in predefined formulas first
            var savedFormula = IntervalFormulas.all.find { it.name == savedFormulaName }
            
            // If not found, and it's a custom formula, restore from SharedPreferences
            if (savedFormula == null && savedFormulaName.startsWith("Custom:")) {
                savedFormula = restoreCustomFormulaFromPrefs()
            }
            
            if (savedFormula != null) {
                currentFormula = savedFormula
                binding.formulaButton.text = currentFormula.name
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
                    isRunning = savedIsRunning
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
            }
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
                binding.formulaButton.text = currentFormula.name
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
            return if (isCircuit) {
                val patternText = if (circuitPattern == "fast_slow_fast") {
                    "${fastMinutes}-${slowMinutes}-${fastMinutes}"
                } else {
                    "${slowMinutes}-${fastMinutes}-${slowMinutes}"
                }
                IntervalFormula(
                    name = if (rounds == 1) {
                        getString(R.string.custom_circuit_name_format_singular, patternText)
                    } else {
                        getString(R.string.custom_circuit_name_format, patternText, rounds)
                    },
                    slowDurationSeconds = slowMinutes * 60,
                    fastDurationSeconds = fastMinutes * 60,
                    totalIntervals = rounds * 2, // Each circuit = 2 intervals
                    startsWithFast = circuitPattern == "fast_slow_fast"
                )
            } else {
                IntervalFormula(
                    name = if (rounds == 1) {
                        getString(R.string.custom_interval_name_format_singular, slowMinutes, fastMinutes)
                    } else {
                        getString(R.string.custom_interval_name_format, slowMinutes, fastMinutes, rounds)
                    },
                    slowDurationSeconds = slowMinutes * 60,
                    fastDurationSeconds = fastMinutes * 60,
                    totalIntervals = rounds,
                    startsWithFast = startsWithFast
                )
            }
        }
        return null
    }
    
    private fun saveCustomFormula(formula: IntervalFormula, isCircuit: Boolean = false, circuitPattern: String = "fast_slow_fast") {
        val slowMinutes = formula.slowDurationSeconds / 60
        val fastMinutes = formula.fastDurationSeconds / 60
        val rounds = if (isCircuit) formula.totalIntervals / 2 else formula.totalIntervals
        
        sharedPreferences.edit {
            putBoolean(KEY_IS_CUSTOM_FORMULA, true)
                .putInt(KEY_CUSTOM_SLOW_MINUTES, slowMinutes)
                .putInt(KEY_CUSTOM_FAST_MINUTES, fastMinutes)
                .putInt(KEY_CUSTOM_ROUNDS, rounds)
                .putBoolean(KEY_CUSTOM_STARTS_WITH_FAST, formula.startsWithFast)
                .putBoolean(KEY_CUSTOM_IS_CIRCUIT, isCircuit)
                .putString(KEY_CUSTOM_CIRCUIT_PATTERN, circuitPattern)
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
        binding.overflowMenuButton.setOnClickListener { view ->
            hapticSelection(view)
            showOverflowBottomSheet()
        }
    }

    private fun showOverflowBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_overflow_menu, android.widget.FrameLayout(this), false)
        bottomSheetDialog.setContentView(view)
        configureBottomSheet(bottomSheetDialog, view)

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

    private fun setupWorkoutHistoryButton() {
        binding.workoutHistoryButton.setOnClickListener { view ->
            hapticSelection(view)
            startActivity(Intent(this, StatsActivity::class.java))
        }
    }

    private fun setupSettingsButton() {
        binding.settingsButton.setOnClickListener { view ->
            hapticSelection(view)
            showSettingsDialog()
        }
    }

    private fun setupFormulaSpinner() {
        binding.formulaButton.text = currentFormula.name
        updateFormulaDetails()
        
        binding.formulaButton.setOnClickListener { view ->
            hapticSelection(view)
            showFormulaSelectorDialog()
        }
        
        // Clear custom formula flag when a predefined formula is selected
        // This is handled in showFormulaSelectorDialog when a regular formula is selected
    }
    
    private fun showFormulaSelectorDialog() {
        val formulas = IntervalFormulas.all
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_formula_selector, android.widget.FrameLayout(this), false)
        bottomSheetDialog.setContentView(view)
        configureBottomSheet(bottomSheetDialog, view)
        
        val recyclerView = view.findViewById<RecyclerView>(R.id.formulaRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = FormulaAdapter(formulas, showCustomOption = true) { formula ->
            if (formula == null) {
                // Custom option selected
                bottomSheetDialog.dismiss()
                showCustomFormulaDialog()
            } else {
                // Regular formula selected
                // Clear custom formula flag
                sharedPreferences.edit { putBoolean(KEY_IS_CUSTOM_FORMULA, false) }
                
                // Only update if a different formula was selected
                if (currentFormula != formula) {
                    currentFormula = formula
                    binding.formulaButton.text = formula.name
                    updateFormulaDetails()
                    // Always reset timer when formula changes, even if running
                    resetTimer()
                }
                bottomSheetDialog.dismiss()
            }
        }
        
        bottomSheetDialog.show()
    }
    
    private fun showCustomFormulaDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_custom_formula, android.widget.FrameLayout(this), false)
        bottomSheetDialog.setContentView(view)
        configureBottomSheet(bottomSheetDialog, view)
        
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
        
        // Pre-fill with current custom formula values if available
        if (currentFormula.name.startsWith("Custom:")) {
            slowMinutes = currentFormula.slowDurationSeconds / 60
            fastMinutes = currentFormula.fastDurationSeconds / 60
            
            // Check if it's a circuit (name contains "Circuit" or totalIntervals is even and > 1)
            val isCircuit = currentFormula.name.contains("Circuit", ignoreCase = true)
            if (isCircuit) {
                isCircuitMode = true
                rounds = currentFormula.totalIntervals / 2 // Convert intervals to circuits
                // Determine pattern from startsWithFast
                circuitPattern = if (currentFormula.startsWithFast) "fast_slow_fast" else "slow_fast_slow"
            } else {
                rounds = currentFormula.totalIntervals
                if (currentFormula.startsWithFast) {
                    fastFirstRadio.isChecked = true
                } else {
                    slowFirstRadio.isChecked = true
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
                        getString(R.string.custom_circuit_name_format_singular, patternText)
                    } else {
                        getString(R.string.custom_circuit_name_format, patternText, rounds)
                    },
                    slowDurationSeconds = slowMinutes * 60,
                    fastDurationSeconds = fastMinutes * 60,
                    totalIntervals = rounds * 2, // Each circuit = 2 intervals
                    startsWithFast = startsWithFast
                )
            } else {
                // Regular interval
                IntervalFormula(
                    name = if (rounds == 1) {
                        getString(R.string.custom_interval_name_format_singular, slowMinutes, fastMinutes)
                    } else {
                        getString(R.string.custom_interval_name_format, slowMinutes, fastMinutes, rounds)
                    },
                    slowDurationSeconds = slowMinutes * 60,
                    fastDurationSeconds = fastMinutes * 60,
                    totalIntervals = rounds,
                    startsWithFast = !slowFirstRadio.isChecked
                )
            }
            
            // Save custom formula to preferences
            saveCustomFormula(customFormula, isCircuitMode, circuitPattern)
            
            // Update current formula and UI
            currentFormula = customFormula
            binding.formulaButton.text = customFormula.name
            updateFormulaDetails()
            resetTimer()
            
            bottomSheetDialog.dismiss()
            
            // Formula is loaded and ready, user can start when ready
        }
        
        bottomSheetDialog.show()
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
            versionText.text = getString(R.string.version, packageInfo.versionName)
        } catch (e: Exception) {
            versionText.text = getString(R.string.version, "Unknown")
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
                notificationHelper?.speak(getString(R.string.voice_notifications_disabled))
            }

            sharedPreferences.edit { putBoolean(KEY_VOICE_ENABLED, isChecked) }
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
        binding.startPauseButton.setOnClickListener { view ->
            hapticSuccess(view)
            animateControlPress(binding.startPauseButton)
            if (isPreStartCountdownActive) {
                cancelPreStartCountdown(startImmediately = true)
            } else if (intervalTimer?.state?.value?.isRunning == true) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        binding.resetButton.setOnClickListener { view ->
            performHapticFeedback(view)
            animateControlPress(binding.resetButton)
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
        binding.startPauseButton.backgroundTintList = android.content.res.ColorStateList.valueOf(accentColor)
        binding.workoutProgress.progressTintList = android.content.res.ColorStateList.valueOf(accentColor)
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

    private fun startPreStartCountdown() {
        if (isPreStartCountdownActive) return
        isPreStartCountdownActive = true
        preStartCountdownTimer?.cancel()
        val countdownSeconds = getStartCountdownSeconds()
        binding.preStartCountdown.text = String.format(Locale.getDefault(), "%d", countdownSeconds)
        binding.preStartOverlay.visibility = View.VISIBLE
        binding.preStartOverlay.setOnClickListener {
            cancelPreStartCountdown(startImmediately = true)
        }

        val useVibration = sharedPreferences.getBoolean(KEY_VIBRATION_ENABLED, true)
        val useVoice = sharedPreferences.getBoolean(KEY_VOICE_ENABLED, true)
        if (useVoice && notificationHelper == null) {
            notificationHelper = createNotificationHelper()
        }

        preStartCountdownTimer = object : CountDownTimer(countdownSeconds * 1000L, 1000L) {
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
                binding.preStartCountdown.text = getString(R.string.go)
                if (useVibration) {
                    hapticSuccess(binding.root)
                }
                if (useVoice) {
                    notificationHelper?.speak(getString(R.string.go))
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
        valueView.text = getString(R.string.countdown_seconds_format, seconds)
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
        
        binding.timeDisplay.text = formatTime(newTime)
        binding.intervalCounter.text = formatIntervalCounter(state.currentInterval, state.totalIntervals)
        updatePhaseDisplay(state.currentPhase)
        updateWorkoutProgress(state)
        updateButtonStates()
        
        lastDisplayedTime = newTime
    }
    
    private fun animateCountdownUpdate() {
        // Subtle scale animation for countdown updates
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.05f, 1.0f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.05f, 1.0f)
        val animator = ObjectAnimator.ofPropertyValuesHolder(binding.timeDisplay, scaleX, scaleY)
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
                binding.phaseLabel.text = getString(R.string.phase_slow_format, getString(R.string.slow_phase))
                binding.phaseLabel.setTextColor(getSlowColor())
                binding.phaseLabel.setTypeface(null, Typeface.BOLD)
            }
            is IntervalPhase.Fast -> {
                binding.phaseLabel.text = getString(R.string.phase_fast_format, getString(R.string.fast_phase))
                binding.phaseLabel.setTextColor(getFastColor())
                binding.phaseLabel.setTypeface(null, Typeface.BOLD)
            }
            is IntervalPhase.Completed -> {
                binding.phaseLabel.text = getString(R.string.completed)
                binding.phaseLabel.setTextColor(getAccentColor())
                binding.phaseLabel.setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun animatePhaseTransition() {
        val phaseScaleX = PropertyValuesHolder.ofFloat("scaleX", 0.96f, 1.0f)
        val phaseScaleY = PropertyValuesHolder.ofFloat("scaleY", 0.96f, 1.0f)
        val phaseAlpha = PropertyValuesHolder.ofFloat("alpha", 0.7f, 1.0f)
        ObjectAnimator.ofPropertyValuesHolder(binding.phaseLabel, phaseScaleX, phaseScaleY, phaseAlpha).apply {
            duration = 180
            interpolator = DecelerateInterpolator()
            start()
        }

        val timeScaleX = PropertyValuesHolder.ofFloat("scaleX", 0.98f, 1.0f)
        val timeScaleY = PropertyValuesHolder.ofFloat("scaleY", 0.98f, 1.0f)
        ObjectAnimator.ofPropertyValuesHolder(binding.timeDisplay, timeScaleX, timeScaleY).apply {
            duration = 180
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun updateWorkoutProgress(state: TimerState) {
        val totalDuration = currentFormula.totalDurationSeconds
        if (totalDuration > 0) {
            val progress = ((state.elapsedSeconds.toFloat() / totalDuration) * 100).toInt().coerceIn(0, 100)
            binding.workoutProgress.progress = progress
            
            // Update elapsed and remaining time displays
            val elapsedSeconds = state.elapsedSeconds.coerceAtMost(totalDuration)
            val remainingSeconds = (totalDuration - elapsedSeconds).coerceAtLeast(0)
            
            binding.elapsedTime.text = formatTime(elapsedSeconds)
            binding.remainingTime.text = formatTime(remainingSeconds)
        } else {
            binding.workoutProgress.progress = 0
            binding.elapsedTime.text = formatTime(0)
            binding.remainingTime.text = formatTime(0)
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
        binding.timeDisplay.text = formatTime(state.timeRemainingSeconds)
        binding.intervalCounter.text = formatIntervalCounter(0, currentFormula.totalIntervals)
        updatePhaseDisplay(state.currentPhase) // Fixed: Update phase label based on current formula
        updateWorkoutProgress(state)
        updateFormulaDetails()
    }

    private fun updateFormulaDetails() {
        val slowMin = currentFormula.slowDurationSeconds / 60
        val slowSec = currentFormula.slowDurationSeconds % 60
        val fastMin = currentFormula.fastDurationSeconds / 60
        val fastSec = currentFormula.fastDurationSeconds % 60
        val totalMin = currentFormula.totalDurationSeconds / 60
        
        val slowText = if (slowMin > 0) {
            if (slowSec > 0) getString(R.string.time_format_m_s, slowMin, slowSec) else getString(R.string.time_format_m, slowMin)
        } else {
            getString(R.string.time_format_s, slowSec)
        }
        
        val fastText = if (fastMin > 0) {
            if (fastSec > 0) getString(R.string.time_format_m_s, fastMin, fastSec) else getString(R.string.time_format_m, fastMin)
        } else {
            getString(R.string.time_format_s, fastSec)
        }
        
        // Build pattern description showing execution pattern
        val isCircuit = currentFormula.name.contains("Circuit", ignoreCase = true)
        val isHighIntensity = currentFormula.name.contains("5-2", ignoreCase = true)
        val isCustom = currentFormula.name.startsWith("Custom:")
        
        val pattern = when {
            isCircuit -> {
                // Circuit pattern: can be Fast-Slow-Fast or Slow-Fast-Slow
                val circuits = currentFormula.totalIntervals / 2
                val circuitPattern = if (currentFormula.startsWithFast) {
                    // Fast-Slow-Fast pattern
                    if (circuits == 1) {
                        getString(R.string.pattern_fast_slow_fast, fastText, slowText)
                    } else {
                        getString(R.string.pattern_fast_slow_fast_rounds, fastText, slowText, circuits)
                    }
                } else {
                    // Slow-Fast-Slow pattern
                    if (circuits == 1) {
                        getString(R.string.pattern_slow_fast_slow, slowText, fastText)
                    } else {
                        getString(R.string.pattern_slow_fast_slow_rounds, slowText, fastText, circuits)
                    }
                }
                circuitPattern
            }
            isHighIntensity -> {
                // High Intensity 5-2 pattern: Fast(5) → Slow(2) × rounds
                getString(R.string.pattern_fast_slow_rounds, fastText, slowText, currentFormula.totalIntervals)
            }
            isCustom && currentFormula.startsWithFast -> {
                // Custom formula starting with fast
                if (currentFormula.totalIntervals == 1) {
                    getString(R.string.pattern_fast_slow, fastText, slowText)
                } else {
                    getString(R.string.pattern_fast_slow_rounds, fastText, slowText, currentFormula.totalIntervals)
                }
            }
            currentFormula.totalIntervals == 1 -> getString(R.string.pattern_slow_fast, slowText, fastText)
            else -> getString(R.string.pattern_slow_fast_rounds, slowText, fastText, currentFormula.totalIntervals)
        }
        
        // Update styled text with colored note
        val fullText = getString(R.string.formula_summary_format, pattern, totalMin)
        val startNoteText = if (currentFormula.startsWithFast) getString(R.string.starts_fast) else getString(R.string.starts_slow)
        
        val combinedText = getString(R.string.formula_full_text_format, fullText, startNoteText)
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
        
        binding.formulaDetails.text = spannable
    }

    private fun updateButtonStates() {
        val state = intervalTimer?.state?.value
        val isRunning = state?.isRunning == true
        binding.startPauseButton.text = when {
            isRunning -> getString(R.string.pause)
            state != null && state.elapsedSeconds > 0 && state.currentPhase !is IntervalPhase.Completed ->
                getString(R.string.resume)
            else -> getString(R.string.start)
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
                    android.util.Log.d("MainActivity", "Workout recorded: $minutes minutes, type: $workoutType")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error recording workout", e)
                }
            }
        }
    }
    
    private fun showClearStatsConfirmationDialog(parentDialog: BottomSheetDialog) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.clear_stats_title)
            .setMessage(R.string.clear_stats_message)
            .setPositiveButton(R.string.clear) { _, _ ->
                clearAllStats()
                parentDialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
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
            .setTitle(R.string.disable_save_workouts_title)
            .setMessage(R.string.disable_save_workouts_message)
            .setPositiveButton(R.string.turn_off) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(R.string.keep_on) { _, _ ->
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
        val isCircuit = currentFormula.name.contains("Circuit", ignoreCase = true)
        return if (isCircuit) {
            // For 5-4-5 pattern, each round = 2 intervals
            // Convert intervals to rounds: round = ceil(interval / 2.0)
            // For even intervals, we've completed a round: round = interval / 2 + 1
            // For odd intervals, we're in the middle: round = (interval + 1) / 2
            val totalRounds = totalIntervals / 2
            val currentRound = if (currentInterval == 0) {
                0
            } else {
                // Convert interval to round: round = (interval + 2 - (interval % 2)) / 2
                // This handles both odd and even intervals in one formula
                minOf((currentInterval + 2 - (currentInterval % 2)) / 2, totalRounds)
            }
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
                getString(R.string.activity_recognition_permission_needed),
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
                getString(R.string.activity_recognition_permission_denied),
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
                getString(R.string.activity_recognition_permission_denied),
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Factory method for creating NotificationHelper. Can be overridden in tests.
     */
    protected open fun createNotificationHelper(): NotificationHelper {
        return NotificationHelper(this)
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

