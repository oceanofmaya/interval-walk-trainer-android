package com.oceanofmaya.intervalwalktrainer

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class SavedWorkoutRepositoryTest {

    @Mock
    private lateinit var dao: SavedWorkoutDao

    private lateinit var repository: SavedWorkoutRepository

    private val passThroughTransactor = object : SavedWorkoutRepository.Transactor {
        override suspend fun <R> invoke(block: suspend () -> R): R = block()
    }

    private val formula = IntervalFormula(
        name = "Custom: Test",
        slowDurationSeconds = 180,
        fastDurationSeconds = 120,
        totalIntervals = 4,
        startsWithFast = false,
        isCircuit = false
    )

    @BeforeEach
    fun setup() {
        repository = SavedWorkoutRepository(dao, passThroughTransactor)
    }

    @Test
    fun `insertFromFormula returns failure when at cap`() = runTest {
        whenever(dao.count()).thenReturn(SavedWorkoutRepository.MAX_SAVED_WORKOUTS)

        val result = repository.insertFromFormula("Any", formula, SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST)

        assertTrue(result.isFailure)
        verify(dao, never()).insert(any())
    }

    @Test
    fun `insertFromFormula inserts with trimmed name and interval circuit pattern placeholder`() = runTest {
        whenever(dao.count()).thenReturn(0)
        whenever(dao.maxSortOrder()).thenReturn(-1)
        whenever(dao.getAllOrdered()).thenReturn(emptyList())
        whenever(dao.insert(any())).thenReturn(7L)

        val result = repository.insertFromFormula("  My workout  ", formula, "ignored_for_interval")

        assertEquals(7L, result.getOrNull())
        verify(dao).insert(
            argThat { row ->
                row.displayName == "My workout" &&
                    row.slowDurationSeconds == formula.slowDurationSeconds &&
                    row.fastDurationSeconds == formula.fastDurationSeconds &&
                    row.totalIntervals == formula.totalIntervals &&
                    !row.isCircuit &&
                    row.circuitPattern == SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST &&
                    row.sortOrder == 0
            }
        )
    }

    @Test
    fun `insertFromFormula uses formula name when display name blank`() = runTest {
        whenever(dao.count()).thenReturn(0)
        whenever(dao.maxSortOrder()).thenReturn(0)
        whenever(dao.getAllOrdered()).thenReturn(emptyList())
        whenever(dao.insert(any())).thenReturn(1L)

        repository.insertFromFormula("   ", formula, SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST)

        verify(dao).insert(argThat { row -> row.displayName == formula.name })
    }

    @Test
    fun `insertFromFormula appends suffix when name collides`() = runTest {
        whenever(dao.count()).thenReturn(0)
        whenever(dao.maxSortOrder()).thenReturn(0)
        whenever(dao.getAllOrdered()).thenReturn(
            listOf(
                SavedWorkout(
                    id = 1L,
                    displayName = "Dup",
                    slowDurationSeconds = 1,
                    fastDurationSeconds = 1,
                    totalIntervals = 1,
                    isCircuit = false,
                    circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
                    startsWithFast = false,
                    sortOrder = 0
                )
            )
        )
        whenever(dao.insert(any())).thenReturn(2L)

        repository.insertFromFormula("Dup", formula, SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST)

        verify(dao).insert(argThat { row -> row.displayName == "Dup (2)" })
    }

    @Test
    fun `insertFromFormula stores explicit circuit pattern for circuit formulas`() = runTest {
        val circuit = formula.copy(name = "C", isCircuit = true, totalIntervals = 6)
        whenever(dao.count()).thenReturn(0)
        whenever(dao.maxSortOrder()).thenReturn(0)
        whenever(dao.getAllOrdered()).thenReturn(emptyList())
        whenever(dao.insert(any())).thenReturn(3L)

        repository.insertFromFormula("C", circuit, SavedWorkoutRepository.CIRCUIT_PATTERN_SLOW_FAST_SLOW)

        verify(dao).insert(
            argThat { row ->
                row.isCircuit && row.circuitPattern == SavedWorkoutRepository.CIRCUIT_PATTERN_SLOW_FAST_SLOW
            }
        )
    }

    @Test
    fun `duplicate fails when at cap`() = runTest {
        whenever(dao.count()).thenReturn(SavedWorkoutRepository.MAX_SAVED_WORKOUTS)

        val result = repository.duplicate(1L)

        assertTrue(result.isFailure)
        verify(dao, never()).insert(any())
    }

    @Test
    fun `duplicate fails when row missing`() = runTest {
        whenever(dao.count()).thenReturn(5)
        whenever(dao.getById(99L)).thenReturn(null)

        val result = repository.duplicate(99L)

        assertTrue(result.isFailure)
        verify(dao, never()).insert(any())
    }

    @Test
    fun `duplicate inserts copy with copy suffix in name`() = runTest {
        val src = SavedWorkout(
            id = 2L,
            displayName = "Base",
            slowDurationSeconds = 60,
            fastDurationSeconds = 60,
            totalIntervals = 2,
            isCircuit = false,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            startsWithFast = false,
            sortOrder = 1
        )
        whenever(dao.count()).thenReturn(1)
        whenever(dao.getById(2L)).thenReturn(src)
        whenever(dao.getAllOrdered()).thenReturn(listOf(src))
        whenever(dao.maxSortOrder()).thenReturn(10)
        whenever(dao.insert(any())).thenReturn(20L)

        val result = repository.duplicate(2L)

        assertEquals(20L, result.getOrNull())
        verify(dao).insert(
            argThat { row ->
                row.id == 0L &&
                    row.displayName == "Base (copy)" &&
                    row.sortOrder == 11
            }
        )
    }

    @Test
    fun `updateDisplayName fails when row missing`() = runTest {
        whenever(dao.getById(1L)).thenReturn(null)

        val result = repository.updateDisplayName(1L, "New")

        assertTrue(result.isFailure)
        verify(dao, never()).update(any())
    }

    @Test
    fun `updateDisplayName updates row with uniquified name`() = runTest {
        val row = SavedWorkout(
            id = 5L,
            displayName = "Old",
            slowDurationSeconds = 1,
            fastDurationSeconds = 1,
            totalIntervals = 1,
            isCircuit = false,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            startsWithFast = false,
            sortOrder = 0
        )
        whenever(dao.getById(5L)).thenReturn(row)
        whenever(dao.getAllOrdered()).thenReturn(
            listOf(
                row,
                SavedWorkout(
                    id = 6L,
                    displayName = "Taken",
                    slowDurationSeconds = 1,
                    fastDurationSeconds = 1,
                    totalIntervals = 1,
                    isCircuit = false,
                    circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
                    startsWithFast = false,
                    sortOrder = 1
                )
            )
        )

        val result = repository.updateDisplayName(5L, "Taken")

        assertTrue(result.isSuccess)
        verify(dao).update(argThat { updated -> updated.id == 5L && updated.displayName == "Taken (2)" })
    }

    @Test
    fun `updateFromFormula fails when row missing`() = runTest {
        whenever(dao.getById(42L)).thenReturn(null)

        val result = repository.updateFromFormula(
            id = 42L,
            newDisplayName = "New",
            formula = formula,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST
        )

        assertTrue(result.isFailure)
        verify(dao, never()).update(any())
    }

    @Test
    fun `updateFromFormula fails when sanitized name is blank`() = runTest {
        val row = SavedWorkout(
            id = 5L,
            displayName = "Old",
            slowDurationSeconds = 1,
            fastDurationSeconds = 1,
            totalIntervals = 1,
            isCircuit = false,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            startsWithFast = false,
            sortOrder = 0
        )
        whenever(dao.getById(5L)).thenReturn(row)

        val result = repository.updateFromFormula(
            id = 5L,
            newDisplayName = "   ",
            formula = formula,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST
        )

        assertTrue(result.isFailure)
        verify(dao, never()).update(any())
    }

    @Test
    fun `updateFromFormula preserves id sortOrder and createdAt and overwrites canonical fields`() = runTest {
        val original = SavedWorkout(
            id = 5L,
            displayName = "Old",
            slowDurationSeconds = 60,
            fastDurationSeconds = 60,
            totalIntervals = 2,
            isCircuit = false,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            startsWithFast = false,
            createdAt = 1_700_000_000L,
            sortOrder = 3
        )
        whenever(dao.getById(5L)).thenReturn(original)
        whenever(dao.getAllOrdered()).thenReturn(listOf(original))
        val edited = formula.copy(
            name = "ignored",
            slowDurationSeconds = 240,
            fastDurationSeconds = 90,
            totalIntervals = 8,
            startsWithFast = true,
            isCircuit = false
        )

        val result = repository.updateFromFormula(
            id = 5L,
            newDisplayName = "  Renamed  ",
            formula = edited,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST
        )

        assertTrue(result.isSuccess)
        verify(dao).update(
            argThat { row ->
                row.id == 5L &&
                    row.displayName == "Renamed" &&
                    row.slowDurationSeconds == 240 &&
                    row.fastDurationSeconds == 90 &&
                    row.totalIntervals == 8 &&
                    row.startsWithFast &&
                    !row.isCircuit &&
                    row.circuitPattern == SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST &&
                    row.createdAt == 1_700_000_000L &&
                    row.sortOrder == 3
            }
        )
    }

    @Test
    fun `updateFromFormula uniquifies display name against other rows excluding self`() = runTest {
        val target = SavedWorkout(
            id = 5L,
            displayName = "Mine",
            slowDurationSeconds = 1,
            fastDurationSeconds = 1,
            totalIntervals = 1,
            isCircuit = false,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            startsWithFast = false,
            sortOrder = 0
        )
        val sibling = target.copy(id = 6L, displayName = "Taken", sortOrder = 1)
        whenever(dao.getById(5L)).thenReturn(target)
        whenever(dao.getAllOrdered()).thenReturn(listOf(target, sibling))

        val result = repository.updateFromFormula(
            id = 5L,
            newDisplayName = "Taken",
            formula = formula,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST
        )

        assertTrue(result.isSuccess)
        verify(dao).update(argThat { row -> row.id == 5L && row.displayName == "Taken (2)" })
    }

    @Test
    fun `updateFromFormula allows keeping same display name on target row`() = runTest {
        val target = SavedWorkout(
            id = 5L,
            displayName = "Mine",
            slowDurationSeconds = 1,
            fastDurationSeconds = 1,
            totalIntervals = 1,
            isCircuit = false,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            startsWithFast = false,
            sortOrder = 0
        )
        whenever(dao.getById(5L)).thenReturn(target)
        whenever(dao.getAllOrdered()).thenReturn(listOf(target))

        val result = repository.updateFromFormula(
            id = 5L,
            newDisplayName = "Mine",
            formula = formula.copy(slowDurationSeconds = 999),
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST
        )

        assertTrue(result.isSuccess)
        verify(dao).update(
            argThat { row -> row.id == 5L && row.displayName == "Mine" && row.slowDurationSeconds == 999 }
        )
    }

    @Test
    fun `updateFromFormula truncates display name to max length`() = runTest {
        val target = SavedWorkout(
            id = 5L,
            displayName = "Old",
            slowDurationSeconds = 1,
            fastDurationSeconds = 1,
            totalIntervals = 1,
            isCircuit = false,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            startsWithFast = false,
            sortOrder = 0
        )
        whenever(dao.getById(5L)).thenReturn(target)
        whenever(dao.getAllOrdered()).thenReturn(listOf(target))
        val tooLong = "x".repeat(SavedWorkoutRepository.MAX_DISPLAY_NAME_LENGTH + 10)

        val result = repository.updateFromFormula(
            id = 5L,
            newDisplayName = tooLong,
            formula = formula,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST
        )

        assertTrue(result.isSuccess)
        verify(dao).update(
            argThat { row -> row.displayName.length == SavedWorkoutRepository.MAX_DISPLAY_NAME_LENGTH }
        )
    }

    @Test
    fun `updateFromFormula normalizes circuit pattern placeholder for interval mode`() = runTest {
        val target = SavedWorkout(
            id = 5L,
            displayName = "Mine",
            slowDurationSeconds = 1,
            fastDurationSeconds = 1,
            totalIntervals = 1,
            isCircuit = true,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_SLOW_FAST_SLOW,
            startsWithFast = false,
            sortOrder = 0
        )
        whenever(dao.getById(5L)).thenReturn(target)
        whenever(dao.getAllOrdered()).thenReturn(listOf(target))
        val switchedToInterval = formula.copy(isCircuit = false)

        repository.updateFromFormula(
            id = 5L,
            newDisplayName = "Mine",
            formula = switchedToInterval,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_SLOW_FAST_SLOW
        )

        verify(dao).update(
            argThat { row ->
                !row.isCircuit &&
                    row.circuitPattern == SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST
            }
        )
    }

    @Test
    fun `circuitPatternForFormula returns explicit pattern only for circuit`() {
        val interval = formula.copy(isCircuit = false)
        assertEquals(
            SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            SavedWorkoutRepository.circuitPatternForFormula(
                interval,
                SavedWorkoutRepository.CIRCUIT_PATTERN_SLOW_FAST_SLOW
            )
        )
        val circuit = formula.copy(isCircuit = true)
        assertEquals(
            SavedWorkoutRepository.CIRCUIT_PATTERN_SLOW_FAST_SLOW,
            SavedWorkoutRepository.circuitPatternForFormula(
                circuit,
                SavedWorkoutRepository.CIRCUIT_PATTERN_SLOW_FAST_SLOW
            )
        )
    }

    @Test
    fun `applyOrder updates only rows whose sortOrder changes`() = runTest {
        val a = SavedWorkout(
            id = 1L,
            displayName = "A",
            slowDurationSeconds = 1,
            fastDurationSeconds = 1,
            totalIntervals = 1,
            isCircuit = false,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            startsWithFast = false,
            sortOrder = 0
        )
        val b = a.copy(id = 2L, displayName = "B", sortOrder = 1)
        val c = a.copy(id = 3L, displayName = "C", sortOrder = 2)
        whenever(dao.getById(1L)).thenReturn(a)
        whenever(dao.getById(2L)).thenReturn(b)
        whenever(dao.getById(3L)).thenReturn(c)

        repository.applyOrder(listOf(3L, 1L, 2L))

        verify(dao).update(argThat { row -> row.id == 3L && row.sortOrder == 0 })
        verify(dao).update(argThat { row -> row.id == 1L && row.sortOrder == 1 })
        verify(dao).update(argThat { row -> row.id == 2L && row.sortOrder == 2 })
    }

    @Test
    fun `applyOrder skips rows whose sortOrder already matches`() = runTest {
        val a = SavedWorkout(
            id = 1L,
            displayName = "A",
            slowDurationSeconds = 1,
            fastDurationSeconds = 1,
            totalIntervals = 1,
            isCircuit = false,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            startsWithFast = false,
            sortOrder = 0
        )
        val b = a.copy(id = 2L, sortOrder = 1)
        whenever(dao.getById(1L)).thenReturn(a)
        whenever(dao.getById(2L)).thenReturn(b)

        repository.applyOrder(listOf(1L, 2L))

        verify(dao, never()).update(any())
    }

    @Test
    fun `applyOrder tolerates missing rows without throwing`() = runTest {
        val present = SavedWorkout(
            id = 1L,
            displayName = "A",
            slowDurationSeconds = 1,
            fastDurationSeconds = 1,
            totalIntervals = 1,
            isCircuit = false,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            startsWithFast = false,
            sortOrder = 0
        )
        whenever(dao.getById(1L)).thenReturn(present)
        whenever(dao.getById(99L)).thenReturn(null)

        repository.applyOrder(listOf(99L, 1L))

        // Missing 99L is skipped; present 1L moves from sortOrder 0 to index 1.
        verify(dao).update(argThat { row -> row.id == 1L && row.sortOrder == 1 })
    }

    @Test
    fun `insertForMigration returns existing id when signature already present`() = runTest {
        val existing = SavedWorkout(
            id = 42L,
            displayName = "Already there",
            slowDurationSeconds = 180,
            fastDurationSeconds = 120,
            totalIntervals = 4,
            isCircuit = false,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            startsWithFast = false,
            sortOrder = 0
        )
        whenever(
            dao.findBySignature(
                slow = 180,
                fast = 120,
                intervals = 4,
                isCircuit = false,
                circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
                startsWithFast = false
            )
        ).thenReturn(existing)

        val candidate = SavedWorkoutRepository.fromFormulaForMigration(
            formula = formula,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            sortOrder = 0
        )
        val id = repository.insertForMigration(candidate)

        assertEquals(42L, id)
        verify(dao, never()).insert(any())
    }

    @Test
    fun `insertForMigration inserts when signature absent`() = runTest {
        whenever(
            dao.findBySignature(
                slow = 180,
                fast = 120,
                intervals = 4,
                isCircuit = false,
                circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
                startsWithFast = false
            )
        ).thenReturn(null)
        whenever(dao.getAllOrdered()).thenReturn(emptyList())
        whenever(dao.maxSortOrder()).thenReturn(-1)
        whenever(dao.insert(any())).thenReturn(99L)

        val candidate = SavedWorkoutRepository.fromFormulaForMigration(
            formula = formula,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            sortOrder = 0
        )
        val id = repository.insertForMigration(candidate)

        assertEquals(99L, id)
    }

    @Test
    fun `updateDisplayName fails when sanitized name is blank`() = runTest {
        val row = SavedWorkout(
            id = 5L,
            displayName = "Old",
            slowDurationSeconds = 1,
            fastDurationSeconds = 1,
            totalIntervals = 1,
            isCircuit = false,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            startsWithFast = false,
            sortOrder = 0
        )
        whenever(dao.getById(5L)).thenReturn(row)

        val result = repository.updateDisplayName(5L, "    ")

        assertTrue(result.isFailure)
        verify(dao, never()).update(any())
    }

    @Test
    fun `insertFromFormula truncates display name to max length`() = runTest {
        whenever(dao.count()).thenReturn(0)
        whenever(dao.maxSortOrder()).thenReturn(-1)
        whenever(dao.getAllOrdered()).thenReturn(emptyList())
        whenever(dao.insert(any())).thenReturn(1L)
        val tooLong = "x".repeat(SavedWorkoutRepository.MAX_DISPLAY_NAME_LENGTH + 25)

        repository.insertFromFormula(tooLong, formula, SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST)

        verify(dao).insert(
            argThat { row ->
                row.displayName.length == SavedWorkoutRepository.MAX_DISPLAY_NAME_LENGTH
            }
        )
    }

    @Test
    fun `fromFormulaForMigration builds entity from formula`() {
        val circuit = formula.copy(name = "Legacy", isCircuit = true, totalIntervals = 6)
        val entity = SavedWorkoutRepository.fromFormulaForMigration(
            circuit,
            SavedWorkoutRepository.CIRCUIT_PATTERN_SLOW_FAST_SLOW,
            sortOrder = 0
        )
        assertEquals("Legacy", entity.displayName)
        assertTrue(entity.isCircuit)
        assertEquals(SavedWorkoutRepository.CIRCUIT_PATTERN_SLOW_FAST_SLOW, entity.circuitPattern)
        assertEquals(0, entity.sortOrder)
    }

    @Test
    fun `restore fails when library is full`() = runTest {
        whenever(dao.count()).thenReturn(SavedWorkoutRepository.MAX_SAVED_WORKOUTS)
        val snapshot = sampleSavedWorkout(id = 11L, displayName = "Morning walk")

        val result = repository.restore(snapshot)

        assertTrue(result.isFailure)
        verify(dao, never()).insertOrReplace(any())
    }

    @Test
    fun `restore reinserts snapshot preserving id sortOrder and createdAt`() = runTest {
        whenever(dao.count()).thenReturn(5)
        whenever(dao.getAllOrdered()).thenReturn(emptyList())
        val snapshot = sampleSavedWorkout(
            id = 17L,
            displayName = "Evening walk",
            sortOrder = 3,
            createdAt = 1_700_000_000L
        )

        val result = repository.restore(snapshot)

        assertTrue(result.isSuccess)
        verify(dao).insertOrReplace(
            argThat { row ->
                row.id == 17L &&
                    row.displayName == "Evening walk" &&
                    row.sortOrder == 3 &&
                    row.createdAt == 1_700_000_000L
            }
        )
    }

    @Test
    fun `restore uniquifies display name when another row took it during undo window`() = runTest {
        whenever(dao.count()).thenReturn(2)
        // Another row now owns the snapshot's original display name (user renamed something into
        // that slot during the 5-second undo window).
        whenever(dao.getAllOrdered()).thenReturn(
            listOf(
                sampleSavedWorkout(id = 99L, displayName = "Morning walk")
            )
        )
        val snapshot = sampleSavedWorkout(id = 11L, displayName = "Morning walk", sortOrder = 2)

        val result = repository.restore(snapshot)

        assertTrue(result.isSuccess)
        verify(dao).insertOrReplace(
            argThat { row ->
                row.id == 11L &&
                    row.displayName == "Morning walk (2)" &&
                    row.sortOrder == 2
            }
        )
    }

    private fun sampleSavedWorkout(
        id: Long,
        displayName: String,
        sortOrder: Int = 0,
        createdAt: Long = 0L
    ) = SavedWorkout(
        id = id,
        displayName = displayName,
        slowDurationSeconds = 180,
        fastDurationSeconds = 180,
        totalIntervals = 5,
        isCircuit = false,
        circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
        startsWithFast = false,
        createdAt = createdAt,
        sortOrder = sortOrder
    )
}
