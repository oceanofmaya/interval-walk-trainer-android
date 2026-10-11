package com.oceanofmaya.intervalwalktrainer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingWorkoutStoreTest {
    @Test
    fun pendingWorkoutSurvivesStoreRecreationAndClearsById() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val firstStore = PendingWorkoutStore(context)
        val workout = PendingWorkoutStore.PendingWorkout(
            completionId = "completion-1",
            workoutType = "3-3 Japanese - 5 Rounds",
            minutes = 30,
            completedAtMillis = 1_791_585_600_000L
        )

        firstStore.save(workout)
        val recreatedStore = PendingWorkoutStore(context)

        assertEquals(workout, recreatedStore.load())
        recreatedStore.clearIfMatches("another-id")
        assertEquals(workout, recreatedStore.load())
        recreatedStore.clearIfMatches(workout.completionId)
        assertNull(recreatedStore.load())
    }
}
