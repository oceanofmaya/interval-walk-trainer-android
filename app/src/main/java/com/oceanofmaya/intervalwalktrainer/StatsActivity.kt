package com.oceanofmaya.intervalwalktrainer

import android.graphics.Color
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    // Track the currently displayed month
    private var displayedYear: Int = 0
    private var displayedMonth: Int = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        
        // Initialize workout repository
        val database = AppDatabase.getDatabase(this)
        workoutRepository = WorkoutRepository(database.workoutDao(), database.workoutSessionDao(), database)
        
        // Initialize displayed month to current month
        val calendar = Calendar.getInstance()
        displayedYear = calendar.get(Calendar.YEAR)
        displayedMonth = calendar.get(Calendar.MONTH)
        
        setupToolbar()
        setupClearButton()
        setupMonthNavigation()
        setupTodayButton()
        setupPullToRefresh()
        setupWorkoutList()
        loadStatistics()
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

    private fun hapticSuccess(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val isDarkTheme = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.statsRoot) { _, windowInsets ->
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
        binding.todayButton.visibility = if (isCurrentMonth) android.view.View.GONE else android.view.View.VISIBLE
    }
    
    private fun setupPullToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadStatistics()
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
    
    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_stats_title)
            .setMessage(R.string.clear_stats_message)
            .setPositiveButton(R.string.clear) { _, _ ->
                clearAllStats()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun clearAllStats() {
        lifecycleScope.launch {
            try {
                // Clear all data from database
                workoutRepository.clearAllData()
                // Small delay to ensure database operations are fully committed
                kotlinx.coroutines.delay(100)
                // Reload all UI components to reflect cleared state
                loadStatistics()
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
                
                binding.totalWorkoutsValue.text = stats.totalWorkouts.toString()
                binding.totalMinutesValue.text = formatMinutes(stats.totalMinutes)
                binding.currentStreakValue.text = if (stats.currentStreak == 1) {
                    getString(R.string.day_format, stats.currentStreak)
                } else {
                    getString(R.string.days_format, stats.currentStreak)
                }
                binding.longestStreakValue.text = if (stats.longestStreak == 1) {
                    getString(R.string.day_format, stats.longestStreak)
                } else {
                    getString(R.string.days_format, stats.longestStreak)
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
                
                // Show/hide best day card
                if (stats.bestDay != null) {
                    displayBestDay(stats.bestDay)
                    binding.bestDayValue.visibility = android.view.View.VISIBLE
                    binding.bestDayMinutes.visibility = android.view.View.VISIBLE
                    binding.bestDayEmptyState.visibility = android.view.View.GONE
                    binding.bestDayCard.visibility = android.view.View.VISIBLE
                } else {
                    // Show empty state
                    binding.bestDayValue.visibility = android.view.View.GONE
                    binding.bestDayMinutes.visibility = android.view.View.GONE
                    binding.bestDayEmptyState.visibility = android.view.View.VISIBLE
                    binding.bestDayCard.visibility = android.view.View.VISIBLE
                }
                
                // Show/hide empty state and related sections
                val hasWorkouts = stats.totalWorkouts > 0
                if (hasWorkouts) {
                    binding.emptyStateContainer.visibility = android.view.View.GONE
                    // Show calendar and workout sections
                    binding.calendarSectionHeader.visibility = android.view.View.VISIBLE
                    binding.calendarSectionContainer.visibility = android.view.View.VISIBLE
                    binding.workoutsListTitle.visibility = android.view.View.VISIBLE
                } else {
                    binding.emptyStateContainer.visibility = android.view.View.VISIBLE
                    // Hide calendar and workout sections when no workouts exist
                    binding.calendarSectionHeader.visibility = android.view.View.GONE
                    binding.calendarSectionContainer.visibility = android.view.View.GONE
                    binding.workoutsListTitle.visibility = android.view.View.GONE
                    binding.workoutsRecyclerView.visibility = android.view.View.GONE
                    binding.emptyWorkoutListMessage.visibility = android.view.View.GONE
                }
                
                binding.swipeRefreshLayout.isRefreshing = false
            } catch (e: Exception) {
                android.util.Log.e("StatsActivity", "Error loading statistics", e)
                // Set default values on error
                binding.totalWorkoutsValue.text = "0"
                binding.totalMinutesValue.text = getString(R.string.time_format_min, 0)
                binding.currentStreakValue.text = getString(R.string.days_format, 0)
                binding.longestStreakValue.text = getString(R.string.days_format, 0)
                binding.avgWorkoutsPerWeekValue.text = "0.0"
                binding.emptyStateContainer.visibility = android.view.View.VISIBLE
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
                
                // Create calendar grid
                setupCalendarGrid(displayedYear, displayedMonth, workoutDates)
            } catch (e: Exception) {
                android.util.Log.e("StatsActivity", "Error loading calendar", e)
                // Show empty calendar on error
                setupCalendarGrid(displayedYear, displayedMonth, emptySet())
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
                    binding.emptyWorkoutListMessage.text = getString(R.string.no_workouts_in_month, monthName)
                    binding.emptyWorkoutListMessage.visibility = android.view.View.VISIBLE
                    binding.workoutsRecyclerView.visibility = android.view.View.GONE
                } else {
                    binding.emptyWorkoutListMessage.visibility = android.view.View.GONE
                    binding.workoutsRecyclerView.visibility = android.view.View.VISIBLE
                }
                
                binding.swipeRefreshLayout.isRefreshing = false
            } catch (e: Exception) {
                android.util.Log.e("StatsActivity", "Error loading workout list", e)
                // Show empty list on error
                binding.workoutsRecyclerView.adapter = WorkoutListAdapter(emptyList()) { workout ->
                    showWorkoutDetail(workout.date)
                }
                binding.emptyWorkoutListMessage.visibility = android.view.View.VISIBLE
                binding.workoutsRecyclerView.visibility = android.view.View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private fun updateStreakProgress(currentStreak: Int, longestStreak: Int) {
        if (longestStreak > 0) {
            val progress = ((currentStreak.toFloat() / longestStreak.toFloat()) * 100).toInt().coerceIn(0, 100)
            binding.streakProgressBar.progress = progress
            binding.streakProgressText.text = getString(R.string.streak_progress, currentStreak, longestStreak)
            binding.streakProgressBar.visibility = android.view.View.VISIBLE
            binding.streakProgressText.visibility = android.view.View.VISIBLE
        } else {
            binding.streakProgressBar.visibility = android.view.View.GONE
            binding.streakProgressText.visibility = android.view.View.GONE
        }
    }
    
    private fun displayBestDay(bestDay: BestDayInfo) {
        try {
            val date = dateFormat.parse(bestDay.date)
            if (date != null) {
                val displayDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
                val dateText = displayDateFormat.format(date)
                val workoutText = if (bestDay.workoutCount == 1) {
                    getString(R.string.best_day_format_singular, dateText)
                } else {
                    getString(R.string.best_day_format, dateText, bestDay.workoutCount)
                }
                binding.bestDayValue.text = workoutText
                binding.bestDayMinutes.text = formatMinutes(bestDay.totalMinutes)
            } else {
                val workoutText = if (bestDay.workoutCount == 1) {
                    getString(R.string.best_day_format_singular, bestDay.date)
                } else {
                    getString(R.string.best_day_format, bestDay.date, bestDay.workoutCount)
                }
                binding.bestDayValue.text = workoutText
                binding.bestDayMinutes.text = formatMinutes(bestDay.totalMinutes)
            }
        } catch (e: Exception) {
            val workoutText = if (bestDay.workoutCount == 1) {
                getString(R.string.best_day_format_singular, bestDay.date)
            } else {
                getString(R.string.best_day_format, bestDay.date, bestDay.workoutCount)
            }
            binding.bestDayValue.text = workoutText
            binding.bestDayMinutes.text = formatMinutes(bestDay.totalMinutes)
        }
    }
    
    private fun loadMonthComparison() {
        lifecycleScope.launch {
            try {
                val comparison = workoutRepository.getMonthComparison(displayedYear, displayedMonth)
                
                // Show the header and container
                binding.monthlyTrendHeader.visibility = android.view.View.VISIBLE
                binding.monthComparisonContainer.visibility = android.view.View.VISIBLE
                
                // Update workouts card
                if (comparison.previousMonthWorkouts > 0 || comparison.currentMonthWorkouts > 0) {
                    binding.monthComparisonWorkoutsValue.text = if (comparison.currentMonthWorkouts == 1) {
                        getString(R.string.month_comparison_workouts_value_singular)
                    } else {
                        getString(R.string.month_comparison_workouts_value, comparison.currentMonthWorkouts)
                    }
                    updateComparisonBadge(
                        binding.monthComparisonWorkoutsArrow,
                        binding.monthComparisonWorkoutsChange,
                        comparison.workoutChangePercent,
                        comparison.currentMonthWorkouts,
                        comparison.previousMonthWorkouts
                    )
                    binding.monthComparisonWorkoutsBadgeContainer.visibility = android.view.View.VISIBLE
                    binding.monthComparisonWorkoutsEmptyState.visibility = android.view.View.GONE
                } else {
                    binding.monthComparisonWorkoutsBadgeContainer.visibility = android.view.View.GONE
                    binding.monthComparisonWorkoutsEmptyState.visibility = android.view.View.VISIBLE
                }
                
                // Update minutes card
                if (comparison.previousMonthMinutes > 0 || comparison.currentMonthMinutes > 0) {
                    binding.monthComparisonMinutesValue.text = getString(R.string.month_comparison_minutes_value, formatMinutes(comparison.currentMonthMinutes))
                    updateComparisonBadge(
                        binding.monthComparisonMinutesArrow,
                        binding.monthComparisonMinutesChange,
                        comparison.minutesChangePercent,
                        comparison.currentMonthMinutes,
                        comparison.previousMonthMinutes
                    )
                    binding.monthComparisonMinutesBadgeContainer.visibility = android.view.View.VISIBLE
                    binding.monthComparisonMinutesEmptyState.visibility = android.view.View.GONE
                } else {
                    binding.monthComparisonMinutesBadgeContainer.visibility = android.view.View.GONE
                    binding.monthComparisonMinutesEmptyState.visibility = android.view.View.VISIBLE
                }
                
                binding.swipeRefreshLayout.isRefreshing = false
            } catch (e: Exception) {
                android.util.Log.e("StatsActivity", "Error loading month comparison", e)
                binding.monthlyTrendHeader.visibility = android.view.View.GONE
                binding.monthComparisonContainer.visibility = android.view.View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private fun updateComparisonBadge(
        arrowView: TextView,
        percentView: TextView,
        changePercent: Double,
        currentValue: Int,
        previousValue: Int
    ) {
        val primaryBlue = ContextCompat.getColor(this, R.color.button_primary)
        val secondaryRed = ContextCompat.getColor(this, R.color.fast_phase)
        
        when {
            changePercent > 0 -> {
                // Increase - primary blue
                val symbol = "▲" // Thicker up arrow
                val percent = Math.abs(changePercent.toInt())
                arrowView.text = symbol
                arrowView.setTextColor(primaryBlue)
                arrowView.visibility = android.view.View.VISIBLE
                percentView.text = getString(R.string.month_comparison_change, percent)
                percentView.setTextColor(primaryBlue)
                percentView.visibility = android.view.View.VISIBLE
            }
            changePercent < 0 -> {
                // Decrease - secondary red
                val symbol = "▼" // Thicker down arrow
                val percent = Math.abs(changePercent.toInt())
                arrowView.text = symbol
                arrowView.setTextColor(secondaryRed)
                arrowView.visibility = android.view.View.VISIBLE
                percentView.text = getString(R.string.month_comparison_change, percent)
                percentView.setTextColor(secondaryRed)
                percentView.visibility = android.view.View.VISIBLE
            }
            previousValue == 0 && currentValue > 0 -> {
                // New data (no previous month data)
                arrowView.text = "New"
                arrowView.setTextColor(primaryBlue)
                arrowView.visibility = android.view.View.VISIBLE
                percentView.visibility = android.view.View.GONE
            }
            else -> {
                // No change or no data - hide badge
                arrowView.visibility = android.view.View.GONE
                percentView.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun loadWorkoutTypeDistribution() {
        lifecycleScope.launch {
            try {
                val distribution = workoutRepository.getWorkoutTypeDistribution(displayedYear, displayedMonth)
                
                if (distribution.isNotEmpty()) {
                    displayWorkoutTypeDistribution(distribution)
                    binding.workoutTypesHeader.visibility = android.view.View.VISIBLE
                    binding.workoutTypesContainer.visibility = android.view.View.VISIBLE
                } else {
                    binding.workoutTypesHeader.visibility = android.view.View.GONE
                    binding.workoutTypesContainer.visibility = android.view.View.GONE
                }
                binding.swipeRefreshLayout.isRefreshing = false
            } catch (e: Exception) {
                android.util.Log.e("StatsActivity", "Error loading workout type distribution", e)
                binding.workoutTypesHeader.visibility = android.view.View.GONE
                binding.workoutTypesContainer.visibility = android.view.View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private fun displayWorkoutTypeDistribution(distribution: Map<String, Int>) {
        binding.workoutTypesContainer.removeAllViews()
        
        val total = distribution.values.sum()
        val sorted = distribution.toList().sortedByDescending { it.second }
        
        sorted.forEach { (type, count) ->
            val percentage = ((count.toFloat() / total.toFloat()) * 100).toInt()
            
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_workout_type, binding.workoutTypesContainer, false)
            
            val typeName = itemView.findViewById<TextView>(R.id.workoutTypeName)
            val typeCount = itemView.findViewById<TextView>(R.id.workoutTypeCount)
            val typeProgress = itemView.findViewById<android.widget.ProgressBar>(R.id.workoutTypeProgress)
            
            // Fix singular form for "1 Rounds" -> "1 Round"
            val displayType = type.replace("1 Rounds", "1 Round")
            typeName.text = displayType
            typeCount.text = "$count ($percentage%)"
            typeProgress.progress = percentage
            
            binding.workoutTypesContainer.addView(itemView)
        }
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
        
        // Display workout count and total minutes
        val countTextView = view.findViewById<TextView>(R.id.workoutDetailCount)
        countTextView.text = record.completedWorkouts.toString()
        
        val minutesTextView = view.findViewById<TextView>(R.id.workoutDetailMinutes)
        minutesTextView.text = formatMinutes(record.totalMinutes)
        
        // Load and display individual workout sessions
        lifecycleScope.launch {
            try {
                val sessions = workoutRepository.getSessionsByDate(record.date)
                // Sessions are already ordered by timestamp descending
                val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.workoutDetailRecyclerView)
                recyclerView.layoutManager = LinearLayoutManager(this@StatsActivity)
                recyclerView.adapter = WorkoutDetailAdapter(sessions)
            } catch (e: Exception) {
                android.util.Log.e("StatsActivity", "Error loading workout sessions", e)
                // Fallback to empty list if sessions can't be loaded
                val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.workoutDetailRecyclerView)
                recyclerView.layoutManager = LinearLayoutManager(this@StatsActivity)
                recyclerView.adapter = WorkoutDetailAdapter(emptyList())
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
            val minPeekHeight = (screenHeight * 0.50f).toInt()
            val maxPeekHeight = (screenHeight * 0.75f).toInt()
            val widthSpec = View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, View.MeasureSpec.AT_MOST)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
            contentView.measure(widthSpec, heightSpec)
            val contentHeight = contentView.measuredHeight
            behavior.peekHeight = contentHeight.coerceIn(minPeekHeight, maxPeekHeight)
        }
    }
    
    private fun setupCalendarGrid(year: Int, month: Int, workoutDates: Set<String>) {
        val calendar = Calendar.getInstance()
        val todayCalendar = Calendar.getInstance()
        val todayYear = todayCalendar.get(Calendar.YEAR)
        val todayMonth = todayCalendar.get(Calendar.MONTH)
        val todayDay = todayCalendar.get(Calendar.DAY_OF_MONTH)
        
        calendar.set(year, month, 1)
        
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Clear existing views
        binding.calendarGrid.removeAllViews()
        
        // Calculate cell size based on screen width
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val padding = (48 * displayMetrics.density).toInt() // 24dp padding on each side
        val cellWidth = (screenWidth - padding) / 7
        val cellHeight = (40 * displayMetrics.density).toInt()
        
        // Add day headers
        val dayHeaders = arrayOf("S", "M", "T", "W", "T", "F", "S")
        dayHeaders.forEach { day ->
            val textView = TextView(this)
            textView.text = day
            textView.textSize = 12f
            textView.gravity = android.view.Gravity.CENTER
            textView.setTextColor(getColor(R.color.text_secondary))
            textView.layoutParams = android.widget.GridLayout.LayoutParams().apply {
                width = cellWidth
                height = cellHeight
            }
            binding.calendarGrid.addView(textView)
        }
        
        // Add empty cells for days before month starts
        for (unused in 1 until firstDayOfWeek) {
            val emptyView = TextView(this)
            emptyView.layoutParams = android.widget.GridLayout.LayoutParams().apply {
                width = cellWidth
                height = cellHeight
            }
            binding.calendarGrid.addView(emptyView)
        }
        
        // Add day cells
        for (day in 1..daysInMonth) {
            val textView = TextView(this)
            textView.text = String.format(Locale.US, "%d", day)
            textView.gravity = android.view.Gravity.CENTER
            textView.setTextColor(getColor(R.color.text_primary))
            textView.layoutParams = android.widget.GridLayout.LayoutParams().apply {
                width = cellWidth
                height = cellHeight
            }
            
            // Check if this is today
            val isToday = (year == todayYear && month == todayMonth && day == todayDay)
            
            // Check if this date has a workout
            calendar.set(year, month, day)
            val dateString = dateFormat.format(calendar.time)
            val hasWorkout = workoutDates.contains(dateString)
            
            when {
                isToday && hasWorkout -> {
                    // Today with workout: filled blue circle with outer ring
                    textView.setBackgroundResource(R.drawable.calendar_day_today_with_workout)
                    textView.setTextColor(getColor(R.color.white))
                }
                isToday -> {
                    // Today without workout: outline ring
                    textView.setBackgroundResource(R.drawable.calendar_day_today)
                }
                hasWorkout -> {
                    // Past day with workout: filled blue circle
                    textView.setBackgroundResource(R.drawable.calendar_day_highlight)
                    textView.setTextColor(getColor(R.color.white))
                }
            }
            
            // Make clickable if it has a workout
            if (hasWorkout) {
                textView.setOnClickListener { view ->
                    performHapticFeedback(view)
                    showWorkoutDetail(dateString)
                }
                textView.isClickable = true
                textView.isFocusable = true
            }
            
            binding.calendarGrid.addView(textView)
        }
    }
    
    private fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) {
            getString(R.string.time_format_hr_min, hours, mins)
        } else {
            getString(R.string.time_format_min, mins)
        }
    }
}
