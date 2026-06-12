package com.oceanofmaya.intervalwalktrainer

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for storing workout records.
 */
@Database(
    entities = [WorkoutRecord::class, WorkoutSession::class, SavedWorkout::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun savedWorkoutDao(): SavedWorkoutDao

    companion object {
        private const val TAG = "AppDatabase"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
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
            }
        }

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
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
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN stepCount INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN averageHeartRateBpm INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN minHeartRateBpm INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN maxHeartRateBpm INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN stepSource TEXT")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN heartRateSource TEXT")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN startedAt INTEGER")
            }
        }

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN metricsIntervalsJson TEXT")
            }
        }

        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN formulaSnapshotJson TEXT")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN metricsPhaseWindowsJson TEXT")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN fastPhaseAverageHeartRateBpm INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN slowPhaseAverageHeartRateBpm INTEGER")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            Log.d(TAG, "Getting database instance")
            return INSTANCE ?: synchronized(this) {
                Log.d(TAG, "Creating new database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_database"
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6
                )
                .build()
                INSTANCE = instance
                Log.d(TAG, "Database instance created")
                instance
            }
        }
    }
}
