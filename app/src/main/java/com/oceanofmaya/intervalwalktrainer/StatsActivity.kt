package com.oceanofmaya.intervalwalktrainer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.oceanofmaya.intervalwalktrainer.databinding.ActivityStatsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity for displaying workout statistics and calendar.
 */
class StatsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatsBinding
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var weeklyGoalController: WeeklyGoalStatsController
    private lateinit var trendsController: StatsTrendsController
    private lateinit var calendarController: StatsCalendarController
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    // Track the currently displayed month
    private var displayedYear: Int = 0
    private var displayedMonth: Int = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        
        // Initialize workout repository
        val database = AppDatabase.getDatabase(this)
        workoutRepository = WorkoutRepository(database.workoutDao(), database.workoutSessionDao(), database)
        weeklyGoalController = WeeklyGoalStatsController(
            activity = this,
            binding = binding,
            workoutRepository = workoutRepository,
            sharedPreferences = sharedPreferences,
            accentColorProvider = ::getAccentColor
        )
        trendsController = StatsTrendsController(
            activity = this,
            binding = binding,
            workoutRepository = workoutRepository,
            accentColorProvider = ::getAccentColor,
            formatMinutes = ::formatMinutes
        )
        calendarController = StatsCalendarController(
            activity = this,
            binding = binding,
            dateFormat = dateFormat,
            accentColorProvider = ::getAccentColor,
            onWorkoutDateSelected = ::showWorkoutDetail
        )
        
        // Initialize displayed month to current month
        val calendar = Calendar.getInstance()
        displayedYear = calendar.get(Calendar.YEAR)
        displayedMonth = calendar.get(Calendar.MONTH)
        
        setupToolbar()
        applyAccentStyling()
        setupSaveWorkoutsButton()
        setupWeeklyGoalCard()
        setupClearButton()
        setupMonthNavigation()
        setupTodayButton()
        setupPullToRefresh()
        setupWorkoutList()
        loadStatistics()
        loadWeeklyGoalProgress()
        loadCalendar()
        loadWorkoutList()
        loadMonthComparison()
        loadWorkoutTypeDistribution()
    }
    
    /**
     * Performs haptic feedback for button taps.
     */
    private fun performHapticFeedback(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun hapticSelection(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    @SuppressLint("InlinedApi")
    private fun hapticSuccess(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    @SuppressLint("InlinedApi")
    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        val isDarkTheme = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.statsRoot) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.statsAppBar.updatePadding(top = insets.top)
            binding.swipeRefreshLayout.updatePadding(
                left = insets.left,
                right = insets.right,
                bottom = insets.bottom
            )
            windowInsets
        }
    }
    
    private fun setupMonthNavigation() {
        binding.prevMonthButton.setOnClickListener { view ->
            performHapticFeedback(view)
            displayedMonth--
            if (displayedMonth < 0) {
                displayedMonth = 11
                displayedYear--
            }
            loadCalendar()
            loadWorkoutList()
            loadMonthComparison()
            loadWorkoutTypeDistribution()
            updateTodayButtonVisibility()
        }
        
        binding.nextMonthButton.setOnClickListener { view ->
            performHapticFeedback(view)
            displayedMonth++
            if (displayedMonth > 11) {
                displayedMonth = 0
                displayedYear++
            }
            loadCalendar()
            loadWorkoutList()
            loadMonthComparison()
            loadWorkoutTypeDistribution()
            updateTodayButtonVisibility()
        }
    }
    
    private fun setupTodayButton() {
        binding.todayButton.setOnClickListener { view ->
            hapticSelection(view)
            val calendar = Calendar.getInstance()
            displayedYear = calendar.get(Calendar.YEAR)
            displayedMonth = calendar.get(Calendar.MONTH)
            loadCalendar()
            loadWorkoutList()
            loadMonthComparison()
            loadWorkoutTypeDistribution()
            updateTodayButtonVisibility()
        }
        updateTodayButtonVisibility()
    }
    
    private fun updateTodayButtonVisibility() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val isCurrentMonth = (displayedYear == currentYear && displayedMonth == currentMonth)
        binding.todayButton.visibility = if (isCurrentMonth) View.GONE else View.VISIBLE
    }
    
    private fun setupPullToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadStatistics()
            loadWeeklyGoalProgress()
            loadCalendar()
            loadWorkoutList()
            loadMonthComparison()
            loadWorkoutTypeDistribution()
            // Note: isRefreshing is set to false in each load method
        }
    }
    
    private fun setupWorkoutList() {
        binding.workoutsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.workoutsRecyclerView.adapter = WorkoutListAdapter(emptyList()) { workout ->
            showWorkoutDetail(workout.date)
        }
    }
    
    private fun setupClearButton() {
        binding.clearStatsButton.setOnClickListener { view ->
            hapticSuccess(view)
            showClearConfirmationDialog()
        }
    }

    private fun setupSaveWorkoutsButton() {
        updateSaveWorkoutsIcon()
        binding.saveWorkoutsButton.setOnClickListener { view ->
            hapticSelection(view)
            val currentEnabled = sharedPreferences.getBoolean(KEY_SAVE_WORKOUTS, true)
            if (currentEnabled) {
                showDisableSaveWorkoutsDialog {
                    sharedPreferences.edit { putBoolean(KEY_SAVE_WORKOUTS, false) }
                    updateSaveWorkoutsIcon()
                }
            } else {
                sharedPreferences.edit { putBoolean(KEY_SAVE_WORKOUTS, true) }
                updateSaveWorkoutsIcon()
            }
        }
    }

    private fun updateSaveWorkoutsIcon() {
        val enabled = sharedPreferences.getBoolean(KEY_SAVE_WORKOUTS, true)
        val tintColor = if (enabled) {
            getAccentColor()
        } else {
            getColor(R.color.text_secondary)
        }
        binding.saveWorkoutsButton.imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
    }

    private fun setupWeeklyGoalCard() {
        weeklyGoalController.setup()
    }

    private fun loadWeeklyGoalProgress() {
        weeklyGoalController.load()
    }

    companion object {
        private const val PREFS_NAME = "interval_walk_trainer_prefs"
        private const val KEY_SAVE_WORKOUTS = "save_workouts"
        private const val KEY_ACCENT_STYLE = "accent_style"
        private const val ACCENT_BLUE = "blue"
        private const val ACCENT_TEAL = "teal"
        private const val ACCENT_PURPLE = "purple"
        private const val ACCENT_AMBER = "amber"
        private const val ACCENT_MAGENTA = "magenta"
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

    private fun applyAccentStyling() {
        val accent = getAccentColor()
        binding.streakProgressBar.progressTintList = android.content.res.ColorStateList.valueOf(accent)
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.title_clear_workout_history)
            .setMessage(R.string.message_clear_workout_history)
            .setPositiveButton(R.string.action_clear) { _, _ ->
                applyEmptyStatsUiImmediately()
                clearAllStats()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * Updates the stats header and sections to show "no data" immediately (optimistic UI).
     * Call before clearAllStats() so totals and related UI clear right away.
     */
    private fun applyEmptyStatsUiImmediately() {
        binding.totalWorkoutsValue.text = getString(R.string.placeholder_zero)
        binding.totalMinutesValue.text = formatMinutes(0)
        binding.currentStreakValue.text = getString(R.string.format_days, 0)
        binding.longestStreakValue.text = getString(R.string.format_days, 0)
        binding.avgWorkoutsPerWeekValue.text = getString(R.string.placeholder_zero_decimal)
        binding.currentStreakCard.setBackgroundResource(R.drawable.formula_details_background)
        updateStreakProgress(0, 0)
        binding.bestDayValue.visibility = View.GONE
        binding.bestDayMinutes.visibility = View.GONE
        binding.bestDayEmptyState.visibility = View.VISIBLE
        binding.bestDayCard.visibility = View.VISIBLE
        weeklyGoalController.applyEmptyUi()
        binding.emptyStateContainer.visibility = View.VISIBLE
        binding.calendarSectionHeader.visibility = View.GONE
        binding.calendarSectionContainer.visibility = View.GONE
        binding.workoutsListTitle.visibility = View.GONE
        binding.workoutsRecyclerView.visibility = View.GONE
        binding.emptyWorkoutListMessage.visibility = View.GONE
        trendsController.applyEmptyUi()
    }

    private fun showDisableSaveWorkoutsDialog(onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.prompt_disable_save_workouts)
            .setMessage(R.string.message_disable_save_workouts)
            .setPositiveButton(R.string.action_turn_off) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(R.string.action_keep_on, null)
            .show()
    }
    
    private fun clearAllStats() {
        lifecycleScope.launch {
            try {
                workoutRepository.clearAllData()
                loadStatistics()
                loadWeeklyGoalProgress()
                loadCalendar()
                loadWorkoutList()
                loadMonthComparison()
                loadWorkoutTypeDistribution()
            } catch (e: Exception) {
                android.util.Log.e("StatsActivity", "Error clearing stats", e)
            }
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { view ->
            hapticSelection(view)
            finish()
        }
    }
    
    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                val stats = workoutRepository.getStatistics()
                
                binding.totalWorkoutsValue.text = String.format(Locale.getDefault(), "%d", stats.totalWorkouts)
                binding.totalMinutesValue.text = formatMinutes(stats.totalMinutes)
                binding.currentStreakValue.text = if (stats.currentStreak == 1) {
                    getString(R.string.format_day, stats.currentStreak)
                } else {
                    getString(R.string.format_days, stats.currentStreak)
                }
                binding.longestStreakValue.text = if (stats.longestStreak == 1) {
                    getString(R.string.format_day, stats.longestStreak)
                } else {
                    getString(R.string.format_days, stats.longestStreak)
                }
                binding.avgWorkoutsPerWeekValue.text = String.format(Locale.US, "%.1f", stats.averageWorkoutsPerWeek)
                
                // Highlight current streak card with subtle tint if streak > 0
                if (stats.currentStreak > 0) {
                    binding.currentStreakCard.setBackgroundResource(R.drawable.stat_card_highlighted_background)
                } else {
                    binding.currentStreakCard.setBackgroundResource(R.drawable.formula_details_background)
                }
                
                // Update streak progress indicator
                updateStreakProgress(stats.currentStreak, stats.longestStreak)
                
                // Show/Hide the best day card
                if (stats.bestDay != null) {
                    displayBestDay(stats.bestDay)
                    binding.bestDayValue.visibility = View.VISIBLE
                    binding.bestDayMinutes.visibility = View.VISIBLE
                    binding.bestDayEmptyState.visibility = View.GONE
                    binding.bestDayCard.visibility = View.VISIBLE
                } else {
                    // Show empty state
                    binding.bestDayValue.visibility = View.GONE
                    binding.bestDayMinutes.visibility = View.GONE
                    binding.bestDayEmptyState.visibility = View.VISIBLE
                    binding.bestDayCard.visibility = View.VISIBLE
                }
                
                // Show/hide empty state and related sections
                val hasWorkouts = stats.totalWorkouts > 0
                if (hasWorkouts) {
                    binding.emptyStateContainer.visibility = View.GONE
                    // Show calendar and workout sections
                    binding.calendarSectionHeader.visibility = View.VISIBLE
                    binding.calendarSectionContainer.visibility = View.VISIBLE
                    binding.workoutsListTitle.visibility = View.VISIBLE
                } else {
                    binding.emptyStateContainer.visibility = View.VISIBLE
                    // Hide calendar and workout sections when no workouts exist
                    binding.calendarSectionHeader.visibility = View.GONE
                    binding.calendarSectionContainer.visibility = View.GONE
                    binding.workoutsListTitle.visibility = View.GONE
                    binding.workoutsRecyclerView.visibility = View.GONE
                    binding.emptyWorkoutListMessage.visibility = View.GONE
                }
                
                binding.swipeRefreshLayout.isRefreshing = false
            } catch (e: Exception) {
                android.util.Log.e("StatsActivity", "Error loading statistics", e)
                // Set default values on error
                binding.totalWorkoutsValue.text = getString(R.string.placeholder_zero)
                binding.totalMinutesValue.text = getString(R.string.format_time_min, 0)
                binding.currentStreakValue.text = getString(R.string.format_days, 0)
                binding.longestStreakValue.text = getString(R.string.format_days, 0)
                binding.avgWorkoutsPerWeekValue.text = getString(R.string.placeholder_zero_decimal)
                binding.emptyStateContainer.visibility = View.VISIBLE
            }
        }
    }
    
    private fun loadCalendar() {
        val calendar = Calendar.getInstance()
        
        // Update month title
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        calendar.set(displayedYear, displayedMonth, 1)
        binding.calendarMonthTitle.text = monthFormat.format(calendar.time)
        
        // Update today button visibility
        updateTodayButtonVisibility()
        
        lifecycleScope.launch {
            try {
                val records = workoutRepository.getRecordsForMonth(displayedYear, displayedMonth)
                android.util.Log.d("StatsActivity", "Found ${records.size} workout records for $displayedYear-$displayedMonth")
                val workoutDates = records.map { it.date }.toSet()
                android.util.Log.d("StatsActivity", "Workout dates: $workoutDates")
                
                calendarController.setupCalendarGrid(displayedYear, displayedMonth, workoutDates)
            } catch (e: Exception) {
                android.util.Log.e("StatsActivity", "Error loading calendar", e)
                calendarController.setupCalendarGrid(displayedYear, displayedMonth, emptySet())
            }
        }
        binding.swipeRefreshLayout.isRefreshing = false
    }
    
    private fun loadWorkoutList() {
        lifecycleScope.launch {
            try {
                val records = workoutRepository.getRecordsForMonth(displayedYear, displayedMonth)
                // Records are already in descending order from the repository
                binding.workoutsRecyclerView.adapter = WorkoutListAdapter(records) { workout ->
                    showWorkoutDetail(workout.date)
                }
                
                // Show/hide empty message
                if (records.isEmpty()) {
                    val calendar = Calendar.getInstance()
                    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
                    calendar.set(displayedYear, displayedMonth, 1)
                    val monthName = monthFormat.format(calendar.time)
                    binding.emptyWorkoutListMessage.text = getString(R.string.format_no_workouts_in_month, monthName)
                    binding.emptyWorkoutListMessage.visibility = View.VISIBLE
                    binding.workoutsRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyWorkoutListMessage.visibility = View.GONE
                    binding.workoutsRecyclerView.visibility = View.VISIBLE
                }
                
                binding.swipeRefreshLayout.isRefreshing = false
            } catch (e: Exception) {
                android.util.Log.e("StatsActivity", "Error loading workout list", e)
                // Show empty list on error
                binding.workoutsRecyclerView.adapter = WorkoutListAdapter(emptyList()) { workout ->
                    showWorkoutDetail(workout.date)
                }
                binding.emptyWorkoutListMessage.visibility = View.VISIBLE
                binding.workoutsRecyclerView.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private fun updateStreakProgress(currentStreak: Int, longestStreak: Int) {
        if (longestStreak > 0) {
            val progress = ((currentStreak.toFloat() / longestStreak.toFloat()) * 100).toInt().coerceIn(0, 100)
            binding.streakProgressBar.progress = progress
            binding.streakProgressText.text = getString(R.string.format_streak_progress, currentStreak, longestStreak)
            binding.streakProgressBar.visibility = View.VISIBLE
            binding.streakProgressText.visibility = View.VISIBLE
        } else {
            binding.streakProgressBar.visibility = View.GONE
            binding.streakProgressText.visibility = View.GONE
        }
    }
    
    private fun displayBestDay(bestDay: BestDayInfo) {
        try {
            val date = dateFormat.parse(bestDay.date)
            if (date != null) {
                val displayDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
                val dateText = displayDateFormat.format(date)
                val workoutText = if (bestDay.workoutCount == 1) {
                    getString(R.string.format_best_day_singular, dateText)
                } else {
                    getString(R.string.format_best_day, dateText, bestDay.workoutCount)
                }
                binding.bestDayValue.text = workoutText
                binding.bestDayMinutes.text = formatMinutes(bestDay.totalMinutes)
            } else {
                val workoutText = if (bestDay.workoutCount == 1) {
                    getString(R.string.format_best_day_singular, bestDay.date)
                } else {
                    getString(R.string.format_best_day, bestDay.date, bestDay.workoutCount)
                }
                binding.bestDayValue.text = workoutText
                binding.bestDayMinutes.text = formatMinutes(bestDay.totalMinutes)
            }
        } catch (e: Exception) {
            val workoutText = if (bestDay.workoutCount == 1) {
                getString(R.string.format_best_day_singular, bestDay.date)
            } else {
                getString(R.string.format_best_day, bestDay.date, bestDay.workoutCount)
            }
            binding.bestDayValue.text = workoutText
            binding.bestDayMinutes.text = formatMinutes(bestDay.totalMinutes)
        }
    }
    
    private fun loadMonthComparison() {
        trendsController.loadMonthComparison(displayedYear, displayedMonth)
    }
    
    private fun loadWorkoutTypeDistribution() {
        trendsController.loadWorkoutTypeDistribution(displayedYear, displayedMonth)
    }
    
    private fun showWorkoutDetail(date: String) {
        lifecycleScope.launch {
            try {
                val record = workoutRepository.getRecordByDate(date)
                if (record != null) {
                    showWorkoutDetailBottomSheet(record)
                }
            } catch (e: Exception) {
                android.util.Log.e("StatsActivity", "Error loading workout detail", e)
            }
        }
    }
    
    private fun showWorkoutDetailBottomSheet(record: WorkoutRecord) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_workout_detail, FrameLayout(this), false)
        bottomSheetDialog.setContentView(view)
        configureBottomSheet(bottomSheetDialog, view)

        val countTextView = view.findViewById<TextView>(R.id.workoutDetailCount)
        val minutesTextView = view.findViewById<TextView>(R.id.workoutDetailMinutes)
        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.workoutDetailRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fun updateSheetWithSessions(sessions: List<WorkoutSession>, dateRecord: WorkoutRecord?) {
            if (dateRecord != null) {
                countTextView.text = String.format(Locale.getDefault(), "%d", dateRecord.completedWorkouts)
                minutesTextView.text = formatMinutes(dateRecord.totalMinutes)
            }
            recyclerView.adapter = WorkoutDetailAdapter(sessions) { session ->
                AlertDialog.Builder(this)
                    .setTitle(R.string.prompt_delete_workout)
                    .setMessage(R.string.message_delete_workout)
                    .setPositiveButton(R.string.action_delete) { _, _ ->
                        lifecycleScope.launch {
                            try {
                                workoutRepository.deleteSession(session)
                                val remaining = workoutRepository.getSessionsByDate(record.date)
                                if (remaining.isEmpty()) {
                                    bottomSheetDialog.dismiss()
                                    loadStatistics()
                                    loadWeeklyGoalProgress()
                                    loadCalendar()
                                    loadWorkoutList()
                                    loadMonthComparison()
                                    loadWorkoutTypeDistribution()
                                } else {
                                    val updatedRecord = workoutRepository.getRecordByDate(record.date)
                                    updateSheetWithSessions(remaining, updatedRecord)
                                    // Refresh main history list and stats so the day's row shows updated total
                                    loadStatistics()
                                    loadWeeklyGoalProgress()
                                    loadCalendar()
                                    loadWorkoutList()
                                    loadMonthComparison()
                                    loadWorkoutTypeDistribution()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("StatsActivity", "Error deleting workout session", e)
                            }
                        }
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
            }
        }

        // Format and display date
        val dateTextView = view.findViewById<TextView>(R.id.workoutDetailDate)
        try {
            val date = dateFormat.parse(record.date)
            if (date != null) {
                val displayDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
                dateTextView.text = displayDateFormat.format(date)
            } else {
                dateTextView.text = record.date
            }
        } catch (e: Exception) {
            dateTextView.text = record.date
        }

        countTextView.text = String.format(Locale.getDefault(), "%d", record.completedWorkouts)
        minutesTextView.text = formatMinutes(record.totalMinutes)

        lifecycleScope.launch {
            try {
                val sessions = workoutRepository.getSessionsByDate(record.date)
                updateSheetWithSessions(sessions, record)
            } catch (e: Exception) {
                android.util.Log.e("StatsActivity", "Error loading workout sessions", e)
                updateSheetWithSessions(emptyList(), null)
            }
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
            val minPeekHeight = (screenHeight * 0.40f).toInt()
            val maxPeekHeight = (screenHeight * 0.80f).toInt()
            val widthSpec = View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, View.MeasureSpec.AT_MOST)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
            contentView.measure(widthSpec, heightSpec)
            val contentHeight = contentView.measuredHeight
            behavior.peekHeight = contentHeight.coerceIn(minPeekHeight, maxPeekHeight)
        }
    }
    private fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) {
            getString(R.string.format_time_hr_min, hours, mins)
        } else {
            getString(R.string.format_time_min, mins)
        }
    }
}
