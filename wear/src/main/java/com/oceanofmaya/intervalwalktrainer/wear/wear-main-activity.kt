package com.oceanofmaya.intervalwalktrainer.wear

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.oceanofmaya.intervalwalktrainer.IntervalFormula
import com.oceanofmaya.intervalwalktrainer.IntervalFormulas
import com.oceanofmaya.intervalwalktrainer.IntervalPhase
import com.oceanofmaya.intervalwalktrainer.IntervalTimer
import com.oceanofmaya.intervalwalktrainer.TimerState
import com.oceanofmaya.intervalwalktrainer.WearSyncContract
import com.oceanofmaya.intervalwalktrainer.wear.databinding.ActivityWearMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

@Suppress("TooManyFunctions")
class WearMainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    private lateinit var binding: ActivityWearMainBinding
    private lateinit var dataClient: DataClient
    private var formulas = IntervalFormulas.all
    private var syncedPresets: List<IntervalFormula> = emptyList()
    private var activePhoneFormula: IntervalFormula? = null
    private var currentFormula = IntervalFormulas.default
    private var timer: IntervalTimer? = null
    private var timerJob: Job? = null
    private var completionId: String? = null
    private var completionSyncStatusRes = R.string.sync_pending
    private var wakeLock: PowerManager.WakeLock? = null
    private var startTimerAfterNotificationPermission = false
    private var ongoingNotificationStateKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWearMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createOngoingWorkoutNotificationChannel()
        dataClient = Wearable.getDataClient(this)

        binding.formulaButton.setOnClickListener { showFormulaPicker() }
        binding.startPauseButton.setOnClickListener { toggleTimer() }
        binding.resetButton.setOnClickListener { resetTimer() }
        binding.doneButton.setOnClickListener { resetTimer() }
        restoreState(savedInstanceState)
        if (timer == null) {
            createTimer()
        }
        loadPhoneData()
    }

    override fun onResume() {
        super.onResume()
        dataClient.addListener(this)
    }

    override fun onPause() {
        dataClient.removeListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        timerJob?.cancel()
        timer?.dispose()
        releaseWakeLock()
        cancelOngoingActivity()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = timer?.state?.value ?: return
        outState.putString(KEY_FORMULA_NAME, currentFormula.name)
        outState.putInt(KEY_SLOW_SECONDS, currentFormula.slowDurationSeconds)
        outState.putInt(KEY_FAST_SECONDS, currentFormula.fastDurationSeconds)
        outState.putInt(KEY_TOTAL_INTERVALS, currentFormula.totalIntervals)
        outState.putBoolean(KEY_STARTS_FAST, currentFormula.startsWithFast)
        outState.putBoolean(KEY_IS_CIRCUIT, currentFormula.isCircuit)
        outState.putInt(KEY_TIME_REMAINING, state.timeRemainingSeconds)
        outState.putInt(KEY_CURRENT_INTERVAL, state.currentInterval)
        outState.putInt(KEY_ELAPSED_SECONDS, state.elapsedSeconds)
        outState.putBoolean(KEY_IS_RUNNING, state.isRunning)
        outState.putString(KEY_PHASE, phaseName(state.currentPhase))
        outState.putString(KEY_COMPLETION_ID, completionId)
        outState.putInt(KEY_COMPLETION_SYNC_STATUS, completionSyncStatusRes)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (
            requestCode == POST_NOTIFICATIONS_REQUEST_CODE &&
            startTimerAfterNotificationPermission
        ) {
            startTimerAfterNotificationPermission = false
            startTimer()
        }
    }

    private fun showFormulaPicker() {
        if (timer?.state?.value?.elapsedSeconds != 0) return
        WearFormulaPickerDialog(
            context = this,
            formulas = formulas,
            selectedFormula = currentFormula,
            displayName = ::compactFormulaName,
            onFormulaSelected = ::selectFormula
        ).show()
    }

    private fun selectFormula(formula: IntervalFormula) {
        if (formula == currentFormula) return
        currentFormula = formula
        completionId = null
        updateFormulaButton()
        createTimer()
    }

    private fun updateFormulaButton() {
        binding.formulaButton.text = compactFormulaName(currentFormula)
        binding.formulaButton.contentDescription = getString(
            R.string.choose_workout_current,
            currentFormula.name
        )
    }

    private fun compactFormulaName(formula: IntervalFormula): String =
        formula.name.replace(roundSuffixRegex, "")

    private fun createTimer(restoredState: TimerState? = null) {
        updateFormulaButton()
        timerJob?.cancel()
        timer?.dispose()
        timer = IntervalTimer(
            formula = currentFormula,
            onPhaseChange = { phase -> vibrateForPhase(phase) },
            onIntervalComplete = {},
            onWorkoutComplete = ::onWorkoutCompleted
        )
        if (restoredState != null) {
            timer?.restoreState(
                timeRemainingSeconds = restoredState.timeRemainingSeconds,
                currentInterval = restoredState.currentInterval,
                currentPhase = restoredState.currentPhase,
                isRunning = restoredState.isRunning,
                savedElapsedSeconds = restoredState.elapsedSeconds
            )
            if (restoredState.isRunning) acquireWakeLock()
        }
        timerJob = lifecycleScope.launch {
            timer?.state?.collectLatest(::renderState)
        }
    }

    private fun toggleTimer() {
        val state = timer?.state?.value ?: return
        if (state.currentPhase is IntervalPhase.Completed) return
        if (timer?.state?.value?.isRunning == true) {
            timer?.pause()
            releaseWakeLock()
        } else {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                startTimerAfterNotificationPermission = true
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    POST_NOTIFICATIONS_REQUEST_CODE
                )
                return
            }
            startTimer()
        }
    }

    private fun startTimer() {
        if (timer?.state?.value?.elapsedSeconds == 0) {
            completionId = UUID.randomUUID().toString()
        }
        acquireWakeLock()
        timer?.start()
    }

    private fun resetTimer() {
        releaseWakeLock()
        cancelOngoingActivity()
        completionId = null
        completionSyncStatusRes = R.string.sync_pending
        binding.completionSyncStatus.setText(completionSyncStatusRes)
        val availableFormulas = availableFormulaChoices()
        if (currentFormula !in availableFormulas) {
            currentFormula = availableFormulas.firstOrNull { it.name == currentFormula.name }
                ?: activePhoneFormula
                ?: IntervalFormulas.default
            rebuildFormulaChoices()
            createTimer()
        } else {
            timer?.reset()
        }
    }

    private fun renderState(state: TimerState) {
        val isCompleted = state.currentPhase is IntervalPhase.Completed
        binding.timerContent.visibility = if (isCompleted) View.GONE else View.VISIBLE
        binding.completionContent.visibility = if (isCompleted) View.VISIBLE else View.GONE
        if (isCompleted) {
            binding.completionWorkoutSummary.text = getString(
                R.string.workout_complete_summary,
                compactFormulaName(currentFormula),
                (currentFormula.totalDurationSeconds / 60).coerceAtLeast(1)
            )
            binding.completionSyncStatus.setText(completionSyncStatusRes)
        }

        val isResume = !state.isRunning &&
            state.elapsedSeconds > 0 &&
            state.currentPhase !is IntervalPhase.Completed
        val phasePresentation = when (state.currentPhase) {
            is IntervalPhase.Slow -> R.string.phase_slow to R.color.wear_slow
            is IntervalPhase.Fast -> R.string.phase_fast to R.color.wear_fast
            is IntervalPhase.Completed -> R.string.phase_completed to R.color.wear_accent
        }
        val phaseLabelRes = if (isResume) R.string.phase_paused else phasePresentation.first
        binding.phaseLabel.setText(phaseLabelRes)
        val phaseColor = getColor(phasePresentation.second)
        binding.phaseLabel.setTextColor(phaseColor)
        binding.progress.progressTintList = ColorStateList.valueOf(phaseColor)
        binding.timeDisplay.text = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            state.timeRemainingSeconds / 60,
            state.timeRemainingSeconds % 60
        )
        binding.intervalLabel.text = getString(
            R.string.interval_progress,
            state.currentInterval,
            state.totalIntervals
        )
        binding.progress.progress = if (currentFormula.totalDurationSeconds == 0) {
            0
        } else {
            state.elapsedSeconds * 100 / currentFormula.totalDurationSeconds
        }
        val (iconRes, descriptionRes) = when {
            state.isRunning -> R.drawable.baseline_pause_24 to R.string.pause
            isResume -> R.drawable.baseline_play_arrow_24 to R.string.resume
            else -> R.drawable.baseline_play_arrow_24 to R.string.start
        }
        binding.startPauseButton.foreground = ContextCompat.getDrawable(this, iconRes)
        binding.startPauseButton.foregroundTintList =
            ColorStateList.valueOf(getColor(R.color.wear_on_accent))
        binding.startPauseButton.contentDescription = getString(descriptionRes)
        val canChooseFormula = state.elapsedSeconds == 0 && !state.isRunning
        binding.formulaButton.isEnabled = canChooseFormula
        binding.formulaButton.alpha = if (canChooseFormula) 1f else DISABLED_FORMULA_ALPHA
        val canReset = state.elapsedSeconds > 0 || state.isRunning
        binding.resetButton.isEnabled = canReset
        binding.resetButton.alpha = if (canReset) 1f else DISABLED_ALPHA
        if (state.elapsedSeconds > 0 && !isCompleted) {
            publishOngoingActivity(state, phaseLabelRes)
        } else {
            cancelOngoingActivity()
        }
    }

    private fun onWorkoutCompleted() {
        releaseWakeLock()
        cancelOngoingActivity()
        vibrateCompletion()
        val id = completionId ?: UUID.randomUUID().toString().also { completionId = it }
        val completedAt = System.currentTimeMillis()
        val request = PutDataMapRequest.create(
            WearSyncContract.COMPLETED_WORKOUT_PATH_PREFIX + id
        ).apply {
            dataMap.putString(WearSyncContract.KEY_COMPLETION_ID, id)
            dataMap.putLong(WearSyncContract.KEY_COMPLETED_AT_MILLIS, completedAt)
            dataMap.putString(WearSyncContract.KEY_WORKOUT_TYPE, currentFormula.name)
            dataMap.putInt(
                WearSyncContract.KEY_MINUTES,
                (currentFormula.totalDurationSeconds / 60).coerceAtLeast(1)
            )
        }.asPutDataRequest().setUrgent()

        updateCompletionSyncStatus(R.string.sync_pending)
        dataClient.putDataItem(request)
            .addOnSuccessListener {
                if (
                    completionId == id &&
                    timer?.state?.value?.currentPhase is IntervalPhase.Completed
                ) {
                    updateCompletionSyncStatus(R.string.sync_sent)
                }
            }
    }

    private fun updateCompletionSyncStatus(statusRes: Int) {
        completionSyncStatusRes = statusRes
        binding.completionSyncStatus.setText(statusRes)
    }

    private fun loadPhoneData() {
        dataClient.dataItems
            .addOnSuccessListener { buffer ->
                try {
                    buffer.firstOrNull {
                        it.uri.path == WearSyncContract.PRESET_CATALOG_PATH
                    }?.let(::applyPresetCatalogDataItem)
                    buffer.firstOrNull {
                        it.uri.path == WearSyncContract.ACTIVE_FORMULA_PATH
                    }?.let(::applyFormulaDataItem)
                } finally {
                    buffer.release()
                }
            }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            when (event.dataItem.uri.path) {
                WearSyncContract.ACTIVE_FORMULA_PATH ->
                    applyFormulaDataItem(event.dataItem)
                WearSyncContract.PRESET_CATALOG_PATH ->
                    applyPresetCatalogDataItem(event.dataItem)
            }
        }
    }

    private fun applyFormulaDataItem(dataItem: com.google.android.gms.wearable.DataItem) {
        val map = DataMapItem.fromDataItem(dataItem).dataMap
        val formula = map.toFormulaOrNull() ?: return
        activePhoneFormula = formula
        rebuildFormulaChoices()
        if (timer?.state?.value?.elapsedSeconds != 0) return
        currentFormula = formula
        rebuildFormulaChoices()
        updateFormulaButton()
        createTimer()
    }

    private fun applyPresetCatalogDataItem(dataItem: com.google.android.gms.wearable.DataItem) {
        val map = DataMapItem.fromDataItem(dataItem).dataMap
        syncedPresets = map.getDataMapArrayList(WearSyncContract.KEY_PRESETS)
            ?.mapNotNull { it.toFormulaOrNull() }
            .orEmpty()
        val availableFormulas = availableFormulaChoices()
        if (
            timer?.state?.value?.elapsedSeconds == 0 &&
            currentFormula !in availableFormulas
        ) {
            currentFormula = availableFormulas.firstOrNull { it.name == currentFormula.name }
                ?: activePhoneFormula
                ?: IntervalFormulas.default
            rebuildFormulaChoices()
            createTimer()
            return
        }
        rebuildFormulaChoices()
    }

    private fun DataMap.toFormulaOrNull(): IntervalFormula? {
        val formula = IntervalFormula(
            name = getString(WearSyncContract.KEY_FORMULA_NAME) ?: return null,
            slowDurationSeconds = getInt(WearSyncContract.KEY_SLOW_SECONDS),
            fastDurationSeconds = getInt(WearSyncContract.KEY_FAST_SECONDS),
            totalIntervals = getInt(WearSyncContract.KEY_TOTAL_INTERVALS),
            startsWithFast = getBoolean(WearSyncContract.KEY_STARTS_WITH_FAST),
            isCircuit = getBoolean(WearSyncContract.KEY_IS_CIRCUIT)
        )
        return formula.takeIf { it.totalDurationSeconds > 0 }
    }

    private fun rebuildFormulaChoices() {
        formulas = (availableFormulaChoices() + currentFormula).distinct()
    }

    private fun availableFormulaChoices(): List<IntervalFormula> =
        buildList {
            activePhoneFormula?.let(::add)
            addAll(IntervalFormulas.all)
            addAll(syncedPresets)
        }.distinct()

    private fun restoreState(state: Bundle?) {
        state ?: return
        val formulaName = state.getString(KEY_FORMULA_NAME) ?: return
        currentFormula = IntervalFormula(
            name = formulaName,
            slowDurationSeconds = state.getInt(KEY_SLOW_SECONDS),
            fastDurationSeconds = state.getInt(KEY_FAST_SECONDS),
            totalIntervals = state.getInt(KEY_TOTAL_INTERVALS),
            startsWithFast = state.getBoolean(KEY_STARTS_FAST),
            isCircuit = state.getBoolean(KEY_IS_CIRCUIT)
        )
        rebuildFormulaChoices()
        updateFormulaButton()
        completionId = state.getString(KEY_COMPLETION_ID)
        completionSyncStatusRes = state.getInt(
            KEY_COMPLETION_SYNC_STATUS,
            R.string.sync_pending
        )
        val restoredPhase = when (state.getString(KEY_PHASE)) {
            PHASE_FAST -> IntervalPhase.Fast
            PHASE_COMPLETED -> IntervalPhase.Completed
            else -> IntervalPhase.Slow
        }
        createTimer(
            TimerState(
                currentPhase = restoredPhase,
                timeRemainingSeconds = state.getInt(KEY_TIME_REMAINING),
                currentInterval = state.getInt(KEY_CURRENT_INTERVAL),
                totalIntervals = currentFormula.totalIntervals,
                isRunning = state.getBoolean(KEY_IS_RUNNING),
                elapsedSeconds = state.getInt(KEY_ELAPSED_SECONDS)
            )
        )
    }

    private fun vibrateForPhase(phase: IntervalPhase) {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        val duration = if (phase is IntervalPhase.Fast) 250L else 120L
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateCompletion() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0L, 180L, 100L, 180L),
                intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                -1
            )
        )
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IntervalWalkTrainer:WearTimer")
            ?.apply { acquire(MAX_WAKE_LOCK_MILLIS) }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun createOngoingWorkoutNotificationChannel() {
        val channel = NotificationChannel(
            ONGOING_WORKOUT_CHANNEL_ID,
            getString(R.string.ongoing_workout_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.ongoing_workout_channel_description)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    private fun publishOngoingActivity(state: TimerState, phaseLabelRes: Int) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val stateKey = buildString {
            append(phaseName(state.currentPhase))
            append(':')
            append(state.isRunning)
            if (!state.isRunning) {
                append(':')
                append(state.timeRemainingSeconds)
            }
        }
        if (ongoingNotificationStateKey == stateKey) return

        val touchIntent = PendingIntent.getActivity(
            this,
            ONGOING_WORKOUT_REQUEST_CODE,
            Intent(this, WearMainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val phaseLabel = getString(phaseLabelRes).uppercase(Locale.getDefault())
        val timeText = formatTimer(state.timeRemainingSeconds)
        val notificationText = if (state.isRunning) {
            getString(R.string.ongoing_workout_active)
        } else {
            getString(R.string.ongoing_workout_remaining, timeText)
        }
        val notificationBuilder = NotificationCompat.Builder(this, ONGOING_WORKOUT_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_directions_walk_24)
            .setContentTitle(phaseLabel)
            .setContentText(notificationText)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setContentIntent(touchIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(state.isRunning)
            .setUsesChronometer(state.isRunning)
            .setChronometerCountDown(state.isRunning)
        if (state.isRunning) {
            notificationBuilder.setWhen(
                System.currentTimeMillis() + state.timeRemainingSeconds * MILLIS_PER_SECOND
            )
        }

        val statusBuilder = Status.Builder()
            .addTemplate(getString(R.string.ongoing_workout_status_template))
            .addPart("phase", Status.TextPart(phaseLabel))
        if (state.isRunning) {
            val now = SystemClock.elapsedRealtime()
            statusBuilder.addPart(
                "timer",
                Status.TimerPart(
                    now + state.timeRemainingSeconds * MILLIS_PER_SECOND,
                    RUNNING_TIMER_TIMESTAMP,
                    currentFormula.totalDurationSeconds * MILLIS_PER_SECOND
                )
            )
        } else {
            statusBuilder.addPart("timer", Status.TextPart(timeText))
        }
        val status = statusBuilder.build()
        OngoingActivity.Builder(this, ONGOING_WORKOUT_NOTIFICATION_ID, notificationBuilder)
            .setStaticIcon(R.drawable.baseline_directions_walk_24)
            .setTouchIntent(touchIntent)
            .setStatus(status)
            .build()
            .apply(this)

        NotificationManagerCompat.from(this).notify(
            ONGOING_WORKOUT_NOTIFICATION_ID,
            notificationBuilder.build()
        )
        ongoingNotificationStateKey = stateKey
    }

    private fun cancelOngoingActivity() {
        NotificationManagerCompat.from(this).cancel(ONGOING_WORKOUT_NOTIFICATION_ID)
        ongoingNotificationStateKey = null
    }

    private fun formatTimer(seconds: Int): String = String.format(
        Locale.getDefault(),
        "%02d:%02d",
        seconds / SECONDS_PER_MINUTE,
        seconds % SECONDS_PER_MINUTE
    )

    private fun phaseName(phase: IntervalPhase): String = when (phase) {
        is IntervalPhase.Slow -> PHASE_SLOW
        is IntervalPhase.Fast -> PHASE_FAST
        is IntervalPhase.Completed -> PHASE_COMPLETED
    }

    private companion object {
        const val MAX_WAKE_LOCK_MILLIS = 10L * 60L * 60L * 1000L
        const val MILLIS_PER_SECOND = 1000L
        const val SECONDS_PER_MINUTE = 60
        const val RUNNING_TIMER_TIMESTAMP = -1L
        const val ONGOING_WORKOUT_NOTIFICATION_ID = 1802
        const val ONGOING_WORKOUT_REQUEST_CODE = 1802
        const val POST_NOTIFICATIONS_REQUEST_CODE = 1803
        const val ONGOING_WORKOUT_CHANNEL_ID = "ongoing-workout"
        const val DISABLED_ALPHA = 0.45f
        const val DISABLED_FORMULA_ALPHA = 0.62f
        val roundSuffixRegex = Regex(
            """\s+-\s+\d+\s+Rounds(?:\s+\(\d+\s+min\))?$""",
            RegexOption.IGNORE_CASE
        )
        const val KEY_FORMULA_NAME = "formula-name"
        const val KEY_SLOW_SECONDS = "slow-seconds"
        const val KEY_FAST_SECONDS = "fast-seconds"
        const val KEY_TOTAL_INTERVALS = "total-intervals"
        const val KEY_STARTS_FAST = "starts-fast"
        const val KEY_IS_CIRCUIT = "is-circuit"
        const val KEY_TIME_REMAINING = "time-remaining"
        const val KEY_CURRENT_INTERVAL = "current-interval"
        const val KEY_ELAPSED_SECONDS = "elapsed-seconds"
        const val KEY_IS_RUNNING = "is-running"
        const val KEY_PHASE = "phase"
        const val KEY_COMPLETION_ID = "completion-id"
        const val KEY_COMPLETION_SYNC_STATUS = "completion-sync-status"
        const val PHASE_SLOW = "slow"
        const val PHASE_FAST = "fast"
        const val PHASE_COMPLETED = "completed"
    }
}
