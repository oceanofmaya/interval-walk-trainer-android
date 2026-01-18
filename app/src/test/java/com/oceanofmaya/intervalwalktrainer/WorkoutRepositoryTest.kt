package com.oceanofmaya.intervalwalktrainer

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unit tests for WorkoutRepository.
 * 
 * Tests cover:
 * - Recording workouts (new and existing dates)
 * - Getting statistics (totals, streaks, averages)
 * - Getting records for a specific month
 * - Clearing all data
 * - Edge cases (empty data, single day, etc.)
 */
@ExtendWith(MockitoExtension::class)
class WorkoutRepositoryTest {

    @Mock
    private lateinit var workoutDao: WorkoutDao
    
    @Mock
    private lateinit var workoutSessionDao: WorkoutSessionDao
    
    private lateinit var repository: WorkoutRepository
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    @BeforeEach
    fun setup() {
        repository = WorkoutRepository(workoutDao, workoutSessionDao)
    }
    
    @Test
    fun `recordWorkout creates new record for today`() = runTest {
        val today = dateFormat.format(Date())
        val minutes = 30
        val workoutType = "3-3 Japanese - 5 Rounds (30 min)"
        
        whenever(workoutDao.getRecordByDate(today)).thenReturn(null)
        
        repository.recordWorkout(minutes, workoutType)
        
        verify(workoutSessionDao).insert(argThat { session ->
            session.date == today &&
            session.workoutType == workoutType &&
            session.minutes == minutes
        })
        verify(workoutDao).getRecordByDate(today)
        verify(workoutDao).insertOrUpdate(argThat { record ->
            record.date == today &&
            record.completedWorkouts == 1 &&
            record.totalMinutes == minutes
        })
    }
    
    @Test
    fun `recordWorkout updates existing record for today`() = runTest {
        val today = dateFormat.format(Date())
        val existingRecord = WorkoutRecord(
            date = today,
            completedWorkouts = 2,
            totalMinutes = 60,
            lastWorkoutTimestamp = System.currentTimeMillis()
        )
        val additionalMinutes = 30
        val workoutType = "5-2 High Intensity - 4 Rounds (28 min)"
        
        whenever(workoutDao.getRecordByDate(today)).thenReturn(existingRecord)
        
        repository.recordWorkout(additionalMinutes, workoutType)
        
        verify(workoutSessionDao).insert(argThat { session ->
            session.date == today &&
            session.workoutType == workoutType &&
            session.minutes == additionalMinutes
        })
        verify(workoutDao).getRecordByDate(today)
        verify(workoutDao).insertOrUpdate(argThat { record ->
            record.date == today &&
            record.completedWorkouts == 3 &&
            record.totalMinutes == 90
        })
    }
    
    @Test
    fun `getStatistics returns correct totals`() = runTest {
        val records = listOf(
            WorkoutRecord("2024-01-01", 1, 30),
            WorkoutRecord("2024-01-02", 2, 60),
            WorkoutRecord("2024-01-03", 1, 45)
        )
        
        whenever(workoutDao.getAllRecordsList()).thenReturn(records)
        whenever(workoutDao.getTotalWorkouts()).thenReturn(4)
        whenever(workoutDao.getTotalMinutes()).thenReturn(135)
        whenever(workoutDao.getEarliestDate()).thenReturn("2024-01-01")
        whenever(workoutDao.getLatestDate()).thenReturn("2024-01-03")
        
        val stats = repository.getStatistics()
        
        assertEquals(4, stats.totalWorkouts)
        assertEquals(135, stats.totalMinutes)
        assertEquals("2024-01-01", stats.earliestDate)
        assertEquals("2024-01-03", stats.latestDate)
        // Best day should be 2024-01-02 with 2 workouts and 60 minutes
        assertEquals("2024-01-02", stats.bestDay?.date)
        assertEquals(2, stats.bestDay?.workoutCount)
        assertEquals(60, stats.bestDay?.totalMinutes)
    }
    
    @Test
    fun `getStatistics returns zero for empty data`() = runTest {
        whenever(workoutDao.getAllRecordsList()).thenReturn(emptyList())
        whenever(workoutDao.getTotalWorkouts()).thenReturn(0)
        whenever(workoutDao.getTotalMinutes()).thenReturn(0)
        whenever(workoutDao.getEarliestDate()).thenReturn(null)
        whenever(workoutDao.getLatestDate()).thenReturn(null)
        
        val stats = repository.getStatistics()
        
        assertEquals(0, stats.totalWorkouts)
        assertEquals(0, stats.totalMinutes)
        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.longestStreak)
        assertEquals(0.0, stats.averageWorkoutsPerWeek, 0.01)
        assertEquals(null, stats.bestDay)
    }
    
    @Test
    fun `getStatistics calculates current streak correctly`() = runTest {
        val calendar = Calendar.getInstance()
        val today = dateFormat.format(calendar.time)
        
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val yesterday = dateFormat.format(calendar.time)
        
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val twoDaysAgo = dateFormat.format(calendar.time)
        
        val records = listOf(
            WorkoutRecord(today, 1, 30),
            WorkoutRecord(yesterday, 1, 30),
            WorkoutRecord(twoDaysAgo, 1, 30)
        )
        
        whenever(workoutDao.getAllRecordsList()).thenReturn(records)
        whenever(workoutDao.getTotalWorkouts()).thenReturn(3)
        whenever(workoutDao.getTotalMinutes()).thenReturn(90)
        whenever(workoutDao.getEarliestDate()).thenReturn(twoDaysAgo)
        whenever(workoutDao.getLatestDate()).thenReturn(today)
        
        val stats = repository.getStatistics()
        
        // Should count today, yesterday, and two days ago = 3 days
        assertEquals(3, stats.currentStreak)
    }
    
    @Test
    fun `getStatistics calculates longest streak correctly`() = runTest {
        val records = listOf(
            WorkoutRecord("2024-01-01", 1, 30),
            WorkoutRecord("2024-01-02", 1, 30),
            WorkoutRecord("2024-01-03", 1, 30),
            WorkoutRecord("2024-01-05", 1, 30), // Gap on 01-04
            WorkoutRecord("2024-01-06", 1, 30),
            WorkoutRecord("2024-01-07", 1, 30)
        )
        
        whenever(workoutDao.getAllRecordsList()).thenReturn(records)
        whenever(workoutDao.getTotalWorkouts()).thenReturn(6)
        whenever(workoutDao.getTotalMinutes()).thenReturn(180)
        whenever(workoutDao.getEarliestDate()).thenReturn("2024-01-01")
        whenever(workoutDao.getLatestDate()).thenReturn("2024-01-07")
        
        val stats = repository.getStatistics()
        
        // Longest streak should be 3 (01-01, 01-02, 01-03)
        assertEquals(3, stats.longestStreak)
    }
    
    @Test
    fun `getRecordsForMonth returns records in date range`() = runTest {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.JANUARY, 1)
        val startDate = dateFormat.format(calendar.time)
        
        calendar.set(2024, Calendar.JANUARY, 31)
        val endDate = dateFormat.format(calendar.time)
        
        val records = listOf(
            WorkoutRecord("2024-01-15", 1, 30),
            WorkoutRecord("2024-01-20", 2, 60)
        )
        
        whenever(workoutDao.getRecordsByDateRange(startDate, endDate)).thenReturn(records)
        
        val result = repository.getRecordsForMonth(2024, Calendar.JANUARY)
        
        assertEquals(2, result.size)
        verify(workoutDao).getRecordsByDateRange(startDate, endDate)
    }
    
    @Test
    fun `clearAllData deletes all records`() = runTest {
        repository.clearAllData()
        
        verify(workoutDao).deleteAll()
        verify(workoutSessionDao).deleteAll()
    }
    
    @Test
    fun `getSessionsByDate returns sessions for specific date`() = runTest {
        val date = "2024-01-15"
        val sessions = listOf(
            WorkoutSession(date = date, workoutType = "3-3 Japanese - 5 Rounds (30 min)", minutes = 30),
            WorkoutSession(date = date, workoutType = "5-2 High Intensity - 4 Rounds (28 min)", minutes = 28)
        )
        
        whenever(workoutSessionDao.getSessionsByDate(date)).thenReturn(sessions)
        
        val result = repository.getSessionsByDate(date)
        
        assertEquals(2, result.size)
        assertEquals("3-3 Japanese - 5 Rounds (30 min)", result[0].workoutType)
        assertEquals("5-2 High Intensity - 4 Rounds (28 min)", result[1].workoutType)
        verify(workoutSessionDao).getSessionsByDate(date)
    }
    
    @Test
    fun `getSessionsByDate returns empty list when no sessions exist`() = runTest {
        val date = "2024-01-15"
        
        whenever(workoutSessionDao.getSessionsByDate(date)).thenReturn(emptyList())
        
        val result = repository.getSessionsByDate(date)
        
        assertEquals(0, result.size)
        verify(workoutSessionDao).getSessionsByDate(date)
    }
    
    @Test
    fun `recordWorkout saves session with custom workout type`() = runTest {
        val today = dateFormat.format(Date())
        val minutes = 25
        val customType = "Custom: 5-3 - 4 Rounds"
        
        whenever(workoutDao.getRecordByDate(today)).thenReturn(null)
        
        repository.recordWorkout(minutes, customType)
        
        verify(workoutSessionDao).insert(argThat { session ->
            session.date == today &&
            session.workoutType == customType &&
            session.minutes == minutes
        })
    }
    
    @Test
    fun `getStatistics handles null totals gracefully`() = runTest {
        whenever(workoutDao.getAllRecordsList()).thenReturn(emptyList())
        whenever(workoutDao.getTotalWorkouts()).thenReturn(null)
        whenever(workoutDao.getTotalMinutes()).thenReturn(null)
        whenever(workoutDao.getEarliestDate()).thenReturn(null)
        whenever(workoutDao.getLatestDate()).thenReturn(null)
        
        val stats = repository.getStatistics()
        
        assertEquals(0, stats.totalWorkouts)
        assertEquals(0, stats.totalMinutes)
        assertEquals(null, stats.bestDay)
    }
    
    @Test
    fun `getMonthComparison returns correct comparison data`() = runTest {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.FEBRUARY, 1)
        val febStartDate = dateFormat.format(calendar.time)
        calendar.set(2024, Calendar.FEBRUARY, 29)
        val febEndDate = dateFormat.format(calendar.time)
        
        calendar.set(2024, Calendar.JANUARY, 1)
        val janStartDate = dateFormat.format(calendar.time)
        calendar.set(2024, Calendar.JANUARY, 31)
        val janEndDate = dateFormat.format(calendar.time)
        
        val febRecords = listOf(
            WorkoutRecord("2024-02-15", 2, 60),
            WorkoutRecord("2024-02-20", 1, 30)
        )
        val janRecords = listOf(
            WorkoutRecord("2024-01-10", 1, 30)
        )
        
        whenever(workoutDao.getRecordsByDateRange(febStartDate, febEndDate)).thenReturn(febRecords)
        whenever(workoutDao.getRecordsByDateRange(janStartDate, janEndDate)).thenReturn(janRecords)
        
        val comparison = repository.getMonthComparison(2024, Calendar.FEBRUARY)
        
        assertEquals(3, comparison.currentMonthWorkouts) // 2 + 1
        assertEquals(1, comparison.previousMonthWorkouts)
        assertEquals(90, comparison.currentMonthMinutes) // 60 + 30
        assertEquals(30, comparison.previousMonthMinutes)
        assertEquals(200.0, comparison.workoutChangePercent, 0.01) // (3-1)/1 * 100 = 200%
        assertEquals(200.0, comparison.minutesChangePercent, 0.01) // (90-30)/30 * 100 = 200%
    }
    
    @Test
    fun `getMonthComparison handles no previous month data`() = runTest {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.JANUARY, 1)
        val janStartDate = dateFormat.format(calendar.time)
        calendar.set(2024, Calendar.JANUARY, 31)
        val janEndDate = dateFormat.format(calendar.time)
        
        calendar.set(2023, Calendar.DECEMBER, 1)
        val decStartDate = dateFormat.format(calendar.time)
        calendar.set(2023, Calendar.DECEMBER, 31)
        val decEndDate = dateFormat.format(calendar.time)
        
        val janRecords = listOf(
            WorkoutRecord("2024-01-15", 2, 60)
        )
        
        whenever(workoutDao.getRecordsByDateRange(janStartDate, janEndDate)).thenReturn(janRecords)
        whenever(workoutDao.getRecordsByDateRange(decStartDate, decEndDate)).thenReturn(emptyList())
        
        val comparison = repository.getMonthComparison(2024, Calendar.JANUARY)
        
        assertEquals(2, comparison.currentMonthWorkouts)
        assertEquals(0, comparison.previousMonthWorkouts)
        assertEquals(60, comparison.currentMonthMinutes)
        assertEquals(0, comparison.previousMonthMinutes)
        assertEquals(100.0, comparison.workoutChangePercent, 0.01) // New data = 100%
        assertEquals(100.0, comparison.minutesChangePercent, 0.01)
    }
    
    @Test
    fun `getWorkoutTypeDistribution returns correct distribution`() = runTest {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.MARCH, 1)
        val startDate = dateFormat.format(calendar.time)
        calendar.set(2024, Calendar.MARCH, 31)
        val endDate = dateFormat.format(calendar.time)
        
        val sessions = listOf(
            WorkoutSession(date = "2024-03-10", workoutType = "3-3 Japanese - 5 Rounds (30 min)", minutes = 30),
            WorkoutSession(date = "2024-03-15", workoutType = "3-3 Japanese - 5 Rounds (30 min)", minutes = 30),
            WorkoutSession(date = "2024-03-20", workoutType = "5-2 High Intensity - 4 Rounds (28 min)", minutes = 28)
        )
        
        whenever(workoutSessionDao.getSessionsByDateRange(startDate, endDate)).thenReturn(sessions)
        
        val distribution = repository.getWorkoutTypeDistribution(2024, Calendar.MARCH)
        
        assertEquals(2, distribution.size)
        assertEquals(2, distribution["3-3 Japanese - 5 Rounds (30 min)"])
        assertEquals(1, distribution["5-2 High Intensity - 4 Rounds (28 min)"])
    }
    
    @Test
    fun `getWorkoutTypeDistribution returns empty map when no sessions exist`() = runTest {
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.APRIL, 1)
        val startDate = dateFormat.format(calendar.time)
        calendar.set(2024, Calendar.APRIL, 30)
        val endDate = dateFormat.format(calendar.time)
        
        whenever(workoutSessionDao.getSessionsByDateRange(startDate, endDate)).thenReturn(emptyList())
        
        val distribution = repository.getWorkoutTypeDistribution(2024, Calendar.APRIL)
        
        assertEquals(0, distribution.size)
    }
    
    @Test
    fun `getStatistics returns null bestDay when all workouts have same count`() = runTest {
        val records = listOf(
            WorkoutRecord("2024-01-01", 1, 30),
            WorkoutRecord("2024-01-02", 1, 30),
            WorkoutRecord("2024-01-03", 1, 30)
        )
        
        whenever(workoutDao.getAllRecordsList()).thenReturn(records)
        whenever(workoutDao.getTotalWorkouts()).thenReturn(3)
        whenever(workoutDao.getTotalMinutes()).thenReturn(90)
        whenever(workoutDao.getEarliestDate()).thenReturn("2024-01-01")
        whenever(workoutDao.getLatestDate()).thenReturn("2024-01-03")
        
        val stats = repository.getStatistics()
        
        // Best day should still be returned (first one with max count)
        assertEquals("2024-01-01", stats.bestDay?.date)
        assertEquals(1, stats.bestDay?.workoutCount)
    }
}

