package com.oceanofmaya.intervalwalktrainer

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrate2To3CreatesSavedWorkoutsTableAndPreservesExistingData() {
        createVersion2Database()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .build()

        try {
            val db = database.openHelper.writableDatabase
            db.query("SELECT COUNT(*) FROM saved_workouts").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT completedWorkouts, totalMinutes FROM workout_records WHERE date = '2026-04-24'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2, cursor.getInt(0))
                assertEquals(60, cursor.getInt(1))
            }
            db.query("SELECT workoutType, minutes FROM workout_sessions WHERE date = '2026-04-24'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("3-3 Japanese - 5 Rounds", cursor.getString(0))
                assertEquals(30, cursor.getInt(1))
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migrate3To4AddsNullableMetricsColumnsAndPreservesExistingData() {
        createVersion3Database()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(AppDatabase.MIGRATION_3_4)
            .build()

        try {
            val db = database.openHelper.writableDatabase
            db.query(
                """
                SELECT workoutType, minutes, stepCount, averageHeartRateBpm, minHeartRateBpm,
                    maxHeartRateBpm, stepSource, heartRateSource, startedAt
                FROM workout_sessions WHERE date = '2026-04-24'
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("3-3 Japanese - 5 Rounds", cursor.getString(0))
                assertEquals(30, cursor.getInt(1))
                assertTrue(cursor.isNull(2))
                assertTrue(cursor.isNull(3))
                assertTrue(cursor.isNull(4))
                assertTrue(cursor.isNull(5))
                assertTrue(cursor.isNull(6))
                assertTrue(cursor.isNull(7))
                assertTrue(cursor.isNull(8))
            }
            db.execSQL(
                """
                INSERT INTO workout_sessions (
                    date, workoutType, minutes, timestamp, stepCount, averageHeartRateBpm,
                    minHeartRateBpm, maxHeartRateBpm, stepSource, heartRateSource, startedAt
                )
                VALUES (
                    '2026-04-25', '5-2 High Intensity - 4 Rounds', 28, 1777161600000, 2340,
                    118, 102, 132, 'phone_sensor', 'health_connect', 1777159800000
                )
                """.trimIndent()
            )
            db.query(
                """
                SELECT stepCount, averageHeartRateBpm, minHeartRateBpm, maxHeartRateBpm,
                    stepSource, heartRateSource, startedAt
                FROM workout_sessions WHERE date = '2026-04-25'
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2340, cursor.getInt(0))
                assertEquals(118, cursor.getInt(1))
                assertEquals(102, cursor.getInt(2))
                assertEquals(132, cursor.getInt(3))
                assertEquals("phone_sensor", cursor.getString(4))
                assertEquals("health_connect", cursor.getString(5))
                assertEquals(1777159800000, cursor.getLong(6))
            }
        } finally {
            database.close()
        }
    }

    private fun createVersion2Database() {
        context.openOrCreateDatabase(TEST_DB, Context.MODE_PRIVATE, null).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS workout_records (
                    date TEXT NOT NULL,
                    completedWorkouts INTEGER NOT NULL,
                    totalMinutes INTEGER NOT NULL,
                    lastWorkoutTimestamp INTEGER NOT NULL,
                    PRIMARY KEY(date)
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS workout_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    date TEXT NOT NULL,
                    workoutType TEXT NOT NULL,
                    minutes INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO workout_records
                    (date, completedWorkouts, totalMinutes, lastWorkoutTimestamp)
                VALUES ('2026-04-24', 2, 60, 1777075200000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO workout_sessions (date, workoutType, minutes, timestamp)
                VALUES ('2026-04-24', '3-3 Japanese - 5 Rounds', 30, 1777075200000)
                """.trimIndent()
            )
            version = 2
            close()
        }
    }

    @Test
    fun migrate4To5AddsMetricsIntervalsJsonAndPreservesExistingData() {
        createVersion4Database()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(AppDatabase.MIGRATION_4_5)
            .build()

        try {
            val db = database.openHelper.writableDatabase
            db.query(
                """
                SELECT workoutType, minutes, stepCount, startedAt, metricsIntervalsJson
                FROM workout_sessions WHERE date = '2026-04-24'
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("3-3 Japanese - 5 Rounds", cursor.getString(0))
                assertEquals(30, cursor.getInt(1))
                assertEquals(2340, cursor.getInt(2))
                assertEquals(1777159800000, cursor.getLong(3))
                assertTrue(cursor.isNull(4))
            }
            db.execSQL(
                """
                UPDATE workout_sessions
                SET metricsIntervalsJson = '1777159800000,1777161600000'
                WHERE date = '2026-04-24'
                """.trimIndent()
            )
            db.query(
                "SELECT metricsIntervalsJson FROM workout_sessions WHERE date = '2026-04-24'"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("1777159800000,1777161600000", cursor.getString(0))
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migrate5To6AddsPhaseMetricsColumnsAndPreservesExistingData() {
        createVersion5Database()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(AppDatabase.MIGRATION_5_6)
            .build()

        try {
            val db = database.openHelper.writableDatabase
            db.query(
                """
                SELECT stepCount, metricsIntervalsJson, formulaSnapshotJson, metricsPhaseWindowsJson,
                    fastPhaseAverageHeartRateBpm, slowPhaseAverageHeartRateBpm
                FROM workout_sessions WHERE date = '2026-04-24'
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2340, cursor.getInt(0))
                assertTrue(cursor.isNull(1))
                assertTrue(cursor.isNull(2))
                assertTrue(cursor.isNull(3))
                assertTrue(cursor.isNull(4))
                assertTrue(cursor.isNull(5))
            }
        } finally {
            database.close()
        }
    }

    private fun createVersion5Database() {
        createVersion4Database()
        context.openOrCreateDatabase(TEST_DB, Context.MODE_PRIVATE, null).apply {
            execSQL("ALTER TABLE workout_sessions ADD COLUMN metricsIntervalsJson TEXT")
            version = 5
            close()
        }
    }

    private fun createVersion4Database() {
        context.openOrCreateDatabase(TEST_DB, Context.MODE_PRIVATE, null).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS workout_records (
                    date TEXT NOT NULL,
                    completedWorkouts INTEGER NOT NULL,
                    totalMinutes INTEGER NOT NULL,
                    lastWorkoutTimestamp INTEGER NOT NULL,
                    PRIMARY KEY(date)
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS workout_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    date TEXT NOT NULL,
                    workoutType TEXT NOT NULL,
                    minutes INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL,
                    stepCount INTEGER,
                    averageHeartRateBpm INTEGER,
                    minHeartRateBpm INTEGER,
                    maxHeartRateBpm INTEGER,
                    stepSource TEXT,
                    heartRateSource TEXT,
                    startedAt INTEGER
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS saved_workouts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    displayName TEXT NOT NULL,
                    slowDurationSeconds INTEGER NOT NULL,
                    fastDurationSeconds INTEGER NOT NULL,
                    totalIntervals INTEGER NOT NULL,
                    isCircuit INTEGER NOT NULL,
                    circuitPattern TEXT NOT NULL,
                    startsWithFast INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO workout_sessions (
                    date, workoutType, minutes, timestamp, stepCount, averageHeartRateBpm,
                    minHeartRateBpm, maxHeartRateBpm, stepSource, heartRateSource, startedAt
                )
                VALUES (
                    '2026-04-24', '3-3 Japanese - 5 Rounds', 30, 1777161600000, 2340,
                    118, 102, 132, 'health_connect', 'health_connect', 1777159800000
                )
                """.trimIndent()
            )
            version = 4
            close()
        }
    }

    private fun createVersion3Database() {
        context.openOrCreateDatabase(TEST_DB, Context.MODE_PRIVATE, null).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS workout_records (
                    date TEXT NOT NULL,
                    completedWorkouts INTEGER NOT NULL,
                    totalMinutes INTEGER NOT NULL,
                    lastWorkoutTimestamp INTEGER NOT NULL,
                    PRIMARY KEY(date)
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS workout_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    date TEXT NOT NULL,
                    workoutType TEXT NOT NULL,
                    minutes INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS saved_workouts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    displayName TEXT NOT NULL,
                    slowDurationSeconds INTEGER NOT NULL,
                    fastDurationSeconds INTEGER NOT NULL,
                    totalIntervals INTEGER NOT NULL,
                    isCircuit INTEGER NOT NULL,
                    circuitPattern TEXT NOT NULL,
                    startsWithFast INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO workout_sessions (date, workoutType, minutes, timestamp)
                VALUES ('2026-04-24', '3-3 Japanese - 5 Rounds', 30, 1777075200000)
                """.trimIndent()
            )
            version = 3
            close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
