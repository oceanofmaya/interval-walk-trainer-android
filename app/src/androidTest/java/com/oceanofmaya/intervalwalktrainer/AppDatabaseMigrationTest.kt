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

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
