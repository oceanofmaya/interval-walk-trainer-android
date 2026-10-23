package com.oceanofmaya.intervalwalktrainer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Keeps workout execution in foreground priority and publishes its lock-screen controls.
 */
class WorkoutForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                START_NOT_STICKY
            }
            ACTION_PAUSE, ACTION_RESUME -> {
                val isRunning = intent.action == ACTION_RESUME
                currentState = currentState?.copy(isRunning = isRunning)
                currentState?.let(::publishNotification)
                sendWorkoutCommand(if (isRunning) COMMAND_RESUME else COMMAND_PAUSE)
                START_STICKY
            }
            ACTION_UPDATE -> {
                createNotificationChannel()
                currentState = intent.toNotificationState()
                currentState?.let(::startAsForegroundService)
                START_STICKY
            }
            ACTION_START, null -> {
                createNotificationChannel()
                startAsForegroundService(currentState)
                START_STICKY
            }
            else -> START_NOT_STICKY
        }
        return result
    }

    private fun startAsForegroundService(state: WorkoutNotificationState?) {
        val notification = buildNotification(state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun publishNotification(state: WorkoutNotificationState) {
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(state))
        } catch (_: SecurityException) {
            // Android 13+ can reject notification updates when permission is disabled.
        }
    }

    private fun buildNotification(state: WorkoutNotificationState?): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setContentTitle(getString(R.string.notif_workout_in_progress_title))
            .setContentText(getString(R.string.notif_workout_in_progress_body))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        state?.let { configureWorkoutState(builder, it) }

        return builder.build()
    }

    private fun configureWorkoutState(
        builder: NotificationCompat.Builder,
        state: WorkoutNotificationState
    ) {
        val phase = getString(
            when (state.phase) {
                WorkoutNotificationPhase.SLOW -> R.string.label_phase_slow
                WorkoutNotificationPhase.FAST -> R.string.label_phase_fast
            }
        )
        builder.setContentTitle(
            if (state.isRunning) {
                phase
            } else {
                getString(R.string.label_phase_paused)
            }
        )
        val workoutType = getString(
            if (state.isCircuit) {
                R.string.label_workout_type_circuit
            } else {
                R.string.label_workout_type_interval
            }
        )
        val progress = getString(
            R.string.notif_workout_progress,
            workoutType,
            state.currentPosition,
            state.totalPositions
        )
        if (state.isRunning) {
            builder
                .setContentText(progress)
                .setWhen(System.currentTimeMillis() + state.remainingSeconds * 1000L)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
        } else {
            builder
                    .setContentText(progress)
                .setUsesChronometer(false)
                .setShowWhen(false)
        }
        addControlAction(builder, state.action)
    }

    private fun addControlAction(
        builder: NotificationCompat.Builder,
        controlAction: WorkoutNotificationAction
    ) {
        val details = when (controlAction) {
            WorkoutNotificationAction.PAUSE -> NotificationActionDetails(
                action = ACTION_PAUSE,
                labelRes = R.string.action_pause,
                iconRes = R.drawable.baseline_pause_24,
                requestCode = REQUEST_PAUSE
            )
            WorkoutNotificationAction.RESUME -> NotificationActionDetails(
                action = ACTION_RESUME,
                labelRes = R.string.action_resume,
                iconRes = R.drawable.baseline_play_arrow_24,
                requestCode = REQUEST_RESUME
            )
        }
        val actionIntent = Intent(this, WorkoutForegroundService::class.java).apply {
            action = details.action
        }
        val actionPendingIntent = PendingIntent.getService(
            this,
            details.requestCode,
            actionIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder
            .addAction(details.iconRes, getString(details.labelRes), actionPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0)
            )
    }

    private fun sendWorkoutCommand(command: String) {
        sendBroadcast(
            Intent(ACTION_WORKOUT_COMMAND)
                .setPackage(packageName)
                .putExtra(EXTRA_COMMAND, command)
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.oceanofmaya.intervalwalktrainer.action.START_WORKOUT_FGS"
        const val ACTION_STOP = "com.oceanofmaya.intervalwalktrainer.action.STOP_WORKOUT_FGS"
        const val ACTION_UPDATE = "com.oceanofmaya.intervalwalktrainer.action.UPDATE_WORKOUT_FGS"
        const val ACTION_PAUSE = "com.oceanofmaya.intervalwalktrainer.action.PAUSE_WORKOUT"
        const val ACTION_RESUME = "com.oceanofmaya.intervalwalktrainer.action.RESUME_WORKOUT"
        const val ACTION_WORKOUT_COMMAND =
            "com.oceanofmaya.intervalwalktrainer.action.WORKOUT_NOTIFICATION_COMMAND"
        const val EXTRA_COMMAND = "workout_command"
        const val COMMAND_PAUSE = "pause"
        const val COMMAND_RESUME = "resume"

        private const val EXTRA_PHASE = "phase"
        private const val EXTRA_REMAINING_SECONDS = "remaining_seconds"
        private const val EXTRA_CURRENT_POSITION = "current_position"
        private const val EXTRA_TOTAL_POSITIONS = "total_positions"
        private const val EXTRA_IS_CIRCUIT = "is_circuit"
        private const val EXTRA_IS_RUNNING = "is_running"

        private const val CHANNEL_ID = "workout_foreground"
        private const val NOTIFICATION_ID = 2201
        private const val REQUEST_PAUSE = 2202
        private const val REQUEST_RESUME = 2203

        fun updateIntent(
            context: android.content.Context,
            state: WorkoutNotificationState
        ): Intent = Intent(context, WorkoutForegroundService::class.java).apply {
            action = ACTION_UPDATE
            putExtra(EXTRA_PHASE, state.phase.name)
            putExtra(EXTRA_REMAINING_SECONDS, state.remainingSeconds)
            putExtra(EXTRA_CURRENT_POSITION, state.currentPosition)
            putExtra(EXTRA_TOTAL_POSITIONS, state.totalPositions)
            putExtra(EXTRA_IS_CIRCUIT, state.isCircuit)
            putExtra(EXTRA_IS_RUNNING, state.isRunning)
        }
    }

    private var currentState: WorkoutNotificationState? = null

    private data class NotificationActionDetails(
        val action: String,
        val labelRes: Int,
        val iconRes: Int,
        val requestCode: Int
    )

    private fun Intent.toNotificationState(): WorkoutNotificationState? {
        return runCatching {
            WorkoutNotificationState(
                phase = WorkoutNotificationPhase.valueOf(requireNotNull(getStringExtra(EXTRA_PHASE))),
                remainingSeconds = getIntExtra(EXTRA_REMAINING_SECONDS, 0).coerceAtLeast(0),
                currentPosition = getIntExtra(EXTRA_CURRENT_POSITION, 1).coerceAtLeast(1),
                totalPositions = getIntExtra(EXTRA_TOTAL_POSITIONS, 1).coerceAtLeast(1),
                isCircuit = getBooleanExtra(EXTRA_IS_CIRCUIT, false),
                isRunning = getBooleanExtra(EXTRA_IS_RUNNING, false)
            )
        }.getOrNull()
    }
}
