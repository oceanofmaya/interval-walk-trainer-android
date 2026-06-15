package com.oceanofmaya.intervalwalktrainer

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for managing workout records.
 */
class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val database: AppDatabase? = null
) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    /**
     * Get all workout records as a Flow.
     */
    fun getAllRecords(): Flow<List<WorkoutRecord>> = workoutDao.getAllRecords()
    
    /**
     * Record a completed workout for today.
     * 
     * @param minutes The duration of the workout in minutes
     * @param workoutType The name/type of the workout formula used
     * @param metrics Optional per-session metrics captured during active workout intervals
     */
    suspend fun recordWorkout(
        minutes: Int,
        workoutType: String,
        metrics: WorkoutMetricsSummary? = null
    ) {
        val today = dateFormat.format(Date())
        android.util.Log.d("WorkoutRepository", "Recording workout: date=$today, minutes=$minutes, type=$workoutType")
        
        // Save individual workout session
        val session = WorkoutSession(
            date = today,
            workoutType = workoutType,
            minutes = minutes,
            timestamp = System.currentTimeMillis(),
            stepCount = metrics?.stepCount,
            averageHeartRateBpm = metrics?.averageHeartRateBpm,
            minHeartRateBpm = metrics?.minHeartRateBpm,
            maxHeartRateBpm = metrics?.maxHeartRateBpm,
            stepSource = metrics?.stepSource,
            heartRateSource = metrics?.heartRateSource,
            startedAt = metrics?.startedAt,
            metricsIntervalsJson = metrics?.intervals?.let { WorkoutMetricsIntervalCodec.encode(it) },
            formulaSnapshotJson = metrics?.formulaSnapshot?.let { WorkoutMetricsCodec.encodeFormulaSnapshot(it) },
            metricsPhaseWindowsJson = metrics?.phaseWindows?.let { WorkoutMetricsCodec.encodePhaseWindows(it) },
            fastPhaseAverageHeartRateBpm = metrics?.fastPhaseAverageHeartRateBpm,
            slowPhaseAverageHeartRateBpm = metrics?.slowPhaseAverageHeartRateBpm
        )
        workoutSessionDao.insert(session)
        android.util.Log.d("WorkoutRepository", "Saved workout session: $session")
        
        // Update or create daily aggregate record
        val existingRecord = workoutDao.getRecordByDate(today)
        android.util.Log.d("WorkoutRepository", "Existing record for today: $existingRecord")
        
        if (existingRecord != null) {
            // Update existing record
            val updatedRecord = existingRecord.copy(
                completedWorkouts = existingRecord.completedWorkouts + 1,
                totalMinutes = existingRecord.totalMinutes + minutes,
                lastWorkoutTimestamp = System.currentTimeMillis()
            )
            workoutDao.insertOrUpdate(updatedRecord)
            android.util.Log.d("WorkoutRepository", "Updated record: $updatedRecord")
        } else {
            // Create new record
            val newRecord = WorkoutRecord(
                date = today,
                completedWorkouts = 1,
                totalMinutes = minutes,
                lastWorkoutTimestamp = System.currentTimeMillis()
            )
            workoutDao.insertOrUpdate(newRecord)
            android.util.Log.d("WorkoutRepository", "Created new record: $newRecord")
        }
    }
    
    /**
     * Get workout statistics.
     */
    suspend fun getStatistics(): WorkoutStatistics {
        android.util.Log.d("WorkoutRepository", "Getting statistics...")
        val allRecords = workoutDao.getAllRecordsList()
        android.util.Log.d("WorkoutRepository", "All records: ${allRecords.size}")
        allRecords.forEach { android.util.Log.d("WorkoutRepository", "Record: $it") }
        
        val totalWorkouts = workoutDao.getTotalWorkouts() ?: 0
        val totalMinutes = workoutDao.getTotalMinutes() ?: 0
        val earliestDate = workoutDao.getEarliestDate()
        val latestDate = workoutDao.getLatestDate()
        
        android.util.Log.d("WorkoutRepository", "Stats: totalWorkouts=$totalWorkouts, totalMinutes=$totalMinutes")
        
        // Calculate current streak
        val currentStreak = calculateCurrentStreak(allRecords)
        
        // Calculate longest streak
        val longestStreak = calculateLongestStreak(allRecords)
        
        // Calculate average workouts per week
        val avgWorkoutsPerWeek = calculateAverageWorkoutsPerWeek(allRecords, earliestDate)
        
        // Find the best day (day with most workouts)
        val bestDay = findBestDay(allRecords)
        
        return WorkoutStatistics(
            totalWorkouts = totalWorkouts,
            totalMinutes = totalMinutes,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            averageWorkoutsPerWeek = avgWorkoutsPerWeek,
            earliestDate = earliestDate,
            latestDate = latestDate,
            bestDay = bestDay
        )
    }
    
    /**
     * Find the day with the most workouts.
     */
    private fun findBestDay(records: List<WorkoutRecord>): BestDayInfo? {
        if (records.isEmpty()) return null
        
        val bestRecord = records.maxByOrNull { it.completedWorkouts }
        return bestRecord?.let {
            BestDayInfo(
                date = it.date,
                workoutCount = it.completedWorkouts,
                totalMinutes = it.totalMinutes
            )
        }
    }
    
    /**
     * Get month comparison data.
     */
    suspend fun getMonthComparison(year: Int, month: Int): MonthComparison {
        val currentMonthRecords = getRecordsForMonth(year, month)
        val currentMonthWorkouts = currentMonthRecords.sumOf { it.completedWorkouts }
        val currentMonthMinutes = currentMonthRecords.sumOf { it.totalMinutes }
        
        // Get previous month
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        calendar.add(Calendar.MONTH, -1)
        val prevYear = calendar.get(Calendar.YEAR)
        val prevMonth = calendar.get(Calendar.MONTH)
        
        val previousMonthRecords = getRecordsForMonth(prevYear, prevMonth)
        val previousMonthWorkouts = previousMonthRecords.sumOf { it.completedWorkouts }
        val previousMonthMinutes = previousMonthRecords.sumOf { it.totalMinutes }
        
        return MonthComparison(
            currentMonthWorkouts = currentMonthWorkouts,
            previousMonthWorkouts = previousMonthWorkouts,
            currentMonthMinutes = currentMonthMinutes,
            previousMonthMinutes = previousMonthMinutes
        )
    }
    
    /**
     * Get workout type distribution for a month.
     */
    suspend fun getWorkoutTypeDistribution(year: Int, month: Int): Map<String, Int> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = dateFormat.format(calendar.time)
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = dateFormat.format(calendar.time)
        
        val sessions = workoutSessionDao.getSessionsByDateRange(startDate, endDate)
        return sessions.groupingBy { it.workoutType }.eachCount()
    }

    suspend fun getMetricsSummaryForMonth(year: Int, month: Int): WorkoutMetricsSummary? {
        val sessions = getSessionsForMonth(year, month)
        val totalSteps = sessions.mapNotNull { it.stepCount }.takeIf { it.isNotEmpty() }?.sum()
        val averageHeartRate = sessions.mapNotNull { it.averageHeartRateBpm }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toInt()
        val minHeartRate = sessions.mapNotNull { it.minHeartRateBpm }.minOrNull()
        val maxHeartRate = sessions.mapNotNull { it.maxHeartRateBpm }.maxOrNull()
        return WorkoutMetricsSummary(
            stepCount = totalSteps,
            averageHeartRateBpm = averageHeartRate,
            minHeartRateBpm = minHeartRate,
            maxHeartRateBpm = maxHeartRate
        ).takeIf { it.hasDisplayableValue }
    }
    
    /**
     * Clear all workout data.
     * Uses a transaction to ensure atomicity and immediate visibility.
     */
    suspend fun clearAllData() {
        android.util.Log.d("WorkoutRepository", "Clearing all workout data")
        // Use a transaction to ensure both deletions are atomic and immediately visible
        if (database != null) {
            database.withTransaction {
                workoutDao.deleteAll()
                workoutSessionDao.deleteAll()
            }
        } else {
            // Fallback if database instance not provided
            workoutDao.deleteAll()
            workoutSessionDao.deleteAll()
        }
        android.util.Log.d("WorkoutRepository", "Data cleared successfully")
    }
    
    /**
     * Deletes a single workout session and updates the daily aggregate record.
     * If the date has no sessions left, the WorkoutRecord for that date is removed.
     */
    suspend fun deleteSession(session: WorkoutSession) {
        if (database != null) {
            database.withTransaction {
                workoutSessionDao.deleteById(session.id)
                val existingRecord = workoutDao.getRecordByDate(session.date)
                if (existingRecord != null) {
                    val newCount = existingRecord.completedWorkouts - 1
                    val newMinutes = (existingRecord.totalMinutes - session.minutes).coerceAtLeast(0)
                    if (newCount <= 0) {
                        workoutDao.deleteByDate(session.date)
                    } else {
                        val latestRemainingTimestamp = workoutSessionDao.getLatestTimestampByDate(session.date)
                            ?: existingRecord.lastWorkoutTimestamp
                        workoutDao.insertOrUpdate(
                            existingRecord.copy(
                                completedWorkouts = newCount,
                                totalMinutes = newMinutes,
                                lastWorkoutTimestamp = latestRemainingTimestamp
                            )
                        )
                    }
                }
            }
        } else {
            workoutSessionDao.deleteById(session.id)
            val existingRecord = workoutDao.getRecordByDate(session.date)
            if (existingRecord != null) {
                val newCount = existingRecord.completedWorkouts - 1
                val newMinutes = (existingRecord.totalMinutes - session.minutes).coerceAtLeast(0)
                if (newCount <= 0) {
                    workoutDao.deleteByDate(session.date)
                } else {
                    val latestRemainingTimestamp = workoutSessionDao.getLatestTimestampByDate(session.date)
                        ?: existingRecord.lastWorkoutTimestamp
                    workoutDao.insertOrUpdate(
                        existingRecord.copy(
                            completedWorkouts = newCount,
                            totalMinutes = newMinutes,
                            lastWorkoutTimestamp = latestRemainingTimestamp
                        )
                    )
                }
            }
        }
        android.util.Log.d("WorkoutRepository", "Deleted session id=${session.id} for date=${session.date}")
    }

    /**
     * Get workout sessions for a specific date.
     */
    suspend fun getSessionsByDate(date: String): List<WorkoutSession> {
        return workoutSessionDao.getSessionsByDate(date)
    }

    suspend fun updateSessionMetrics(
        session: WorkoutSession,
        metrics: WorkoutMetricsSummary,
        refreshExisting: Boolean = false
    ): WorkoutSession {
        val updatedSession = if (refreshExisting) {
            WorkoutSessionMetricsMerger.refresh(session, metrics)
        } else {
            WorkoutSessionMetricsMerger.merge(session, metrics)
        }
        if (updatedSession != session) {
            workoutSessionDao.update(updatedSession)
        }
        return updatedSession
    }
    
    /**
     * Get records for a specific month.
     */
    suspend fun getRecordsForMonth(year: Int, month: Int): List<WorkoutRecord> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = dateFormat.format(calendar.time)
        
        // Get last day of the month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = dateFormat.format(calendar.time)
        
        android.util.Log.d("WorkoutRepository", "Getting records for month: startDate=$startDate, endDate=$endDate")
        val records = workoutDao.getRecordsByDateRange(startDate, endDate)
        android.util.Log.d("WorkoutRepository", "Found ${records.size} records")
        return records
    }

    suspend fun getSessionsForMonth(year: Int, month: Int): List<WorkoutSession> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = dateFormat.format(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = dateFormat.format(calendar.time)

        return workoutSessionDao.getSessionsByDateRange(startDate, endDate)
    }

    /**
     * Computes how often the weekly goal was met during the given month, plus the same figure for
     * the previous month, so the History view can show a monthly trend.
     */
    suspend fun getMonthlyGoalMetComparison(
        year: Int,
        month: Int,
        settings: WeeklyGoalSettings,
        nowMillis: Long = System.currentTimeMillis()
    ): MonthlyGoalMetComparison {
        suspend fun monthSummary(summaryYear: Int, summaryMonth: Int): MonthlyGoalMetSummary {
            if (!settings.enabled || !settings.hasAnyTarget) {
                return MonthlyGoalMetSummary(active = false, weeksMet = 0, totalWeeks = 0)
            }
            val ranges = MonthlyGoalMetCalculator.weekStartsInMonth(
                summaryYear,
                summaryMonth,
                settings.weekStartDay
            )
            val windows = ranges.map { range ->
                val records = workoutDao.getRecordsByDateRange(range.startDate, range.endDate)
                WeekWindow(
                    range = range,
                    completedWorkouts = records.sumOf { it.completedWorkouts },
                    completedMinutes = records.sumOf { it.totalMinutes }
                )
            }
            return MonthlyGoalMetCalculator.summarize(settings, windows, nowMillis)
        }

        val current = monthSummary(year, month)
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        calendar.add(Calendar.MONTH, -1)
        val previous = monthSummary(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
        return MonthlyGoalMetComparison(current = current, previous = previous)
    }

    suspend fun getWeeklyGoalProgress(
        settings: WeeklyGoalSettings,
        nowMillis: Long = System.currentTimeMillis()
    ): WeeklyGoalProgress {
        val range = WeeklyGoalCalculator.currentWeekRange(nowMillis, settings.weekStartDay)
        val records = workoutDao.getRecordsByDateRange(range.startDate, range.endDate)
        return WeeklyGoalProgress(
            settings = settings,
            dateRange = range,
            completedWorkouts = records.sumOf { it.completedWorkouts },
            completedMinutes = records.sumOf { it.totalMinutes }
        )
    }
    
    /**
     * Get a workout record by date.
     */
    suspend fun getRecordByDate(date: String): WorkoutRecord? {
        return workoutDao.getRecordByDate(date)
    }

    /**
     * Get the most recently completed workout session.
     */
    suspend fun getLatestSession(): WorkoutSession? {
        return workoutSessionDao.getLatestSession()
    }
    
    /**
     * Calculate the current workout streak (consecutive days with workouts).
     */
    private fun calculateCurrentStreak(records: List<WorkoutRecord>): Int {
        if (records.isEmpty()) return 0
        
        val dateSet = records.map { it.date }.toSet()
        val calendar = Calendar.getInstance()
        var streak = 0
        
        // Start from today and count backwards
        while (true) {
            val dateStr = dateFormat.format(calendar.time)
            if (dateStr in dateSet) {
                streak++
                calendar.add(Calendar.DAY_OF_MONTH, -1)
            } else {
                break
            }
        }
        
        return streak
    }
    
    /**
     * Calculate the longest workout streak.
     */
    private fun calculateLongestStreak(records: List<WorkoutRecord>): Int {
        if (records.isEmpty()) return 0
        
        val sortedDates = records.map { dateFormat.parse(it.date)?.time ?: 0L }
            .filter { it > 0 }
            .sorted()
        
        if (sortedDates.isEmpty()) return 0
        
        var maxConsecutive = 1
        var consecutive = 1
        
        for (i in 1 until sortedDates.size) {
            val daysDiff = (sortedDates[i] - sortedDates[i - 1]) / (1000 * 60 * 60 * 24)
            if (daysDiff == 1L) {
                consecutive++
                maxConsecutive = maxOf(maxConsecutive, consecutive)
            } else {
                consecutive = 1
            }
        }
        
        return maxConsecutive
    }
    
    /**
     * Calculate average workouts per week.
     */
    private fun calculateAverageWorkoutsPerWeek(records: List<WorkoutRecord>, earliestDate: String?): Double {
        if (records.isEmpty() || earliestDate == null) return 0.0
        
        val calendar = Calendar.getInstance()
        val today = calendar.time
        val earliest = dateFormat.parse(earliestDate) ?: return 0.0
        
        val daysDiff = (today.time - earliest.time) / (1000 * 60 * 60 * 24)
        val weeks = maxOf(1.0, daysDiff / 7.0)
        
        val totalWorkouts = records.sumOf { it.completedWorkouts }
        return totalWorkouts / weeks
    }
}

/**
 * Workout statistics data class.
 */
data class WorkoutStatistics(
    val totalWorkouts: Int,
    val totalMinutes: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val averageWorkoutsPerWeek: Double,
    val earliestDate: String?,
    val latestDate: String?,
    val bestDay: BestDayInfo? = null
)

/**
 * Information about the best workout day.
 */
data class BestDayInfo(
    val date: String,
    val workoutCount: Int,
    val totalMinutes: Int
)

/**
 * Month comparison information.
 */
data class MonthComparison(
    val currentMonthWorkouts: Int,
    val previousMonthWorkouts: Int,
    val currentMonthMinutes: Int,
    val previousMonthMinutes: Int
) {
    val workoutChangePercent: Double
        get() = if (previousMonthWorkouts > 0) {
            ((currentMonthWorkouts - previousMonthWorkouts).toDouble() / previousMonthWorkouts) * 100
        } else if (currentMonthWorkouts > 0) {
            100.0
        } else {
            0.0
        }
    
    val minutesChangePercent: Double
        get() = if (previousMonthMinutes > 0) {
            ((currentMonthMinutes - previousMonthMinutes).toDouble() / previousMonthMinutes) * 100
        } else if (currentMonthMinutes > 0) {
            100.0
        } else {
            0.0
        }
}
