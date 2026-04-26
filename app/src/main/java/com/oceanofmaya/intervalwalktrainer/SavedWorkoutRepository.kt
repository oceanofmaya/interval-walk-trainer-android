package com.oceanofmaya.intervalwalktrainer

import androidx.room.withTransaction

/**
 * Saved custom workout templates (Room). Enforces [MAX_SAVED_WORKOUTS] on insert/duplicate.
 *
 * All read-then-write paths (insert, duplicate, rename, migration) run inside a Room transaction
 * via [transactor] so cap checks, name uniqueness, and signature dedupe are atomic.
 */
class SavedWorkoutRepository(
    private val dao: SavedWorkoutDao,
    private val transactor: Transactor
) {

    /**
     * Convenience constructor used by app code: routes [Transactor] through Room's
     * [androidx.room.withTransaction]. Tests should use the primary constructor with a
     * pass-through [Transactor] since the Room extension cannot run on a Mockito-mocked database.
     */
    constructor(dao: SavedWorkoutDao, database: AppDatabase) : this(
        dao = dao,
        transactor = object : Transactor {
            override suspend fun <R> invoke(block: suspend () -> R): R =
                database.withTransaction(block)
        }
    )

    /**
     * Indirection over [androidx.room.withTransaction] so multi-step read-then-write paths
     * (cap check + insert, dedupe, rename) can run atomically in production while unit tests
     * supply a synchronous pass-through.
     */
    interface Transactor {
        suspend operator fun <R> invoke(block: suspend () -> R): R
    }

    fun observeAllOrdered() = dao.observeAllOrdered()

    suspend fun getAllOrdered(): List<SavedWorkout> = dao.getAllOrdered()

    suspend fun getById(id: Long): SavedWorkout? = dao.getById(id)

    suspend fun count(): Int = dao.count()

    suspend fun insertFromFormula(
        displayName: String,
        formula: IntervalFormula,
        circuitPattern: String
    ): Result<Long> = transactor {
        if (dao.count() >= MAX_SAVED_WORKOUTS) {
            return@transactor Result.failure(IllegalStateException("cap"))
        }
        val sanitized = displayName.trim().ifEmpty { formula.name }.take(MAX_DISPLAY_NAME_LENGTH)
        val name = uniqueDisplayName(sanitized)
        val nextOrder = dao.maxSortOrder() + 1
        val entity = SavedWorkout(
            displayName = name,
            slowDurationSeconds = formula.slowDurationSeconds,
            fastDurationSeconds = formula.fastDurationSeconds,
            totalIntervals = formula.totalIntervals,
            isCircuit = formula.isCircuit,
            circuitPattern = if (formula.isCircuit) circuitPattern else CIRCUIT_PATTERN_FAST_SLOW_FAST,
            startsWithFast = formula.startsWithFast,
            createdAt = System.currentTimeMillis(),
            sortOrder = nextOrder
        )
        Result.success(dao.insert(entity))
    }

    /**
     * Idempotent migration insert: if a row with the same signature already exists, returns that
     * row's id without inserting a duplicate. Skips the cap check (legacy data only).
     */
    suspend fun insertForMigration(workout: SavedWorkout): Long = transactor {
        val existing = dao.findBySignature(
            slow = workout.slowDurationSeconds,
            fast = workout.fastDurationSeconds,
            intervals = workout.totalIntervals,
            isCircuit = workout.isCircuit,
            circuitPattern = workout.circuitPattern,
            startsWithFast = workout.startsWithFast
        )
        if (existing != null) {
            existing.id
        } else {
            val name = uniqueDisplayName(workout.displayName.take(MAX_DISPLAY_NAME_LENGTH))
            val nextOrder = dao.maxSortOrder() + 1
            dao.insert(workout.copy(displayName = name, sortOrder = nextOrder))
        }
    }

    suspend fun updateDisplayName(id: Long, newName: String): Result<Unit> = transactor {
        val row = dao.getById(id) ?: return@transactor Result.failure(IllegalArgumentException("missing"))
        val sanitized = newName.trim().take(MAX_DISPLAY_NAME_LENGTH)
        if (sanitized.isEmpty()) {
            return@transactor Result.failure(IllegalArgumentException("blank"))
        }
        val unique = uniqueDisplayName(sanitized, excludeId = id)
        dao.update(row.copy(displayName = unique))
        Result.success(Unit)
    }

    /**
     * Updates a saved preset's canonical fields (and optionally its display name) in place.
     * Preserves [SavedWorkout.id], [SavedWorkout.sortOrder], and [SavedWorkout.createdAt] so the
     * row keeps its list position and provenance. Display name is trimmed, length-capped, and
     * uniquified against other rows (via [excludeId]). No cap check (updating, not inserting).
     */
    suspend fun updateFromFormula(
        id: Long,
        newDisplayName: String,
        formula: IntervalFormula,
        circuitPattern: String
    ): Result<Unit> = transactor {
        val row = dao.getById(id)
            ?: return@transactor Result.failure(IllegalArgumentException("missing"))
        val sanitized = newDisplayName.trim().take(MAX_DISPLAY_NAME_LENGTH)
        if (sanitized.isEmpty()) {
            return@transactor Result.failure(IllegalArgumentException("blank"))
        }
        val unique = uniqueDisplayName(sanitized, excludeId = id)
        dao.update(
            row.copy(
                displayName = unique,
                slowDurationSeconds = formula.slowDurationSeconds,
                fastDurationSeconds = formula.fastDurationSeconds,
                totalIntervals = formula.totalIntervals,
                isCircuit = formula.isCircuit,
                circuitPattern = if (formula.isCircuit) circuitPattern else CIRCUIT_PATTERN_FAST_SLOW_FAST,
                startsWithFast = formula.startsWithFast
            )
        )
        Result.success(Unit)
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    /**
     * Re-inserts a previously-deleted [snapshot] preserving its original id, sortOrder, and
     * createdAt — used to back an undo-delete snackbar. Fails with [IllegalStateException] if
     * inserting the snapshot would exceed [MAX_SAVED_WORKOUTS] (e.g. the user added new presets
     * during the undo window and refilled the library). Display name is uniquified against
     * other rows in case a concurrent insert took the name.
     */
    suspend fun restore(snapshot: SavedWorkout): Result<Unit> = transactor {
        if (dao.count() >= MAX_SAVED_WORKOUTS) {
            return@transactor Result.failure(IllegalStateException("cap"))
        }
        val name = uniqueDisplayName(
            snapshot.displayName.take(MAX_DISPLAY_NAME_LENGTH),
            excludeId = snapshot.id
        )
        dao.insertOrReplace(snapshot.copy(displayName = name))
        Result.success(Unit)
    }

    suspend fun duplicate(id: Long): Result<Long> = transactor {
        if (dao.count() >= MAX_SAVED_WORKOUTS) {
            return@transactor Result.failure(IllegalStateException("cap"))
        }
        val src = dao.getById(id)
            ?: return@transactor Result.failure(IllegalArgumentException("missing"))
        val baseName = "${src.displayName} ($COPY_SUFFIX)".take(MAX_DISPLAY_NAME_LENGTH)
        val name = uniqueDisplayName(baseName)
        val nextOrder = dao.maxSortOrder() + 1
        val copy = src.copy(
            id = 0,
            displayName = name,
            createdAt = System.currentTimeMillis(),
            sortOrder = nextOrder
        )
        Result.success(dao.insert(copy))
    }

    suspend fun persistOrder(orderedIds: List<Long>) {
        transactor { applyOrder(orderedIds) }
    }

    /** Visible for testing the minimal-update reorder strategy without going through Room's transaction. */
    internal suspend fun applyOrder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            val row = dao.getById(id) ?: return@forEachIndexed
            if (row.sortOrder != index) {
                dao.update(row.copy(sortOrder = index))
            }
        }
    }

    private suspend fun uniqueDisplayName(base: String, excludeId: Long? = null): String {
        val rows = dao.getAllOrdered()
        val names = rows.filter { excludeId == null || it.id != excludeId }.map { it.displayName }.toMutableSet()
        if (!names.contains(base)) return base
        var n = 2
        while (names.contains("$base ($n)")) n++
        return "$base ($n)"
    }

    companion object {
        const val MAX_SAVED_WORKOUTS = 30
        const val MAX_DISPLAY_NAME_LENGTH = 60
        const val CIRCUIT_PATTERN_FAST_SLOW_FAST = "fast_slow_fast"
        const val CIRCUIT_PATTERN_SLOW_FAST_SLOW = "slow_fast_slow"
        private const val COPY_SUFFIX = "copy"

        fun circuitPatternForFormula(formula: IntervalFormula, explicitPattern: String): String =
            if (formula.isCircuit) explicitPattern else CIRCUIT_PATTERN_FAST_SLOW_FAST

        fun fromFormulaForMigration(
            formula: IntervalFormula,
            circuitPattern: String,
            sortOrder: Int = 0
        ): SavedWorkout = SavedWorkout(
            displayName = formula.name,
            slowDurationSeconds = formula.slowDurationSeconds,
            fastDurationSeconds = formula.fastDurationSeconds,
            totalIntervals = formula.totalIntervals,
            isCircuit = formula.isCircuit,
            circuitPattern = if (formula.isCircuit) circuitPattern else CIRCUIT_PATTERN_FAST_SLOW_FAST,
            startsWithFast = formula.startsWithFast,
            createdAt = System.currentTimeMillis(),
            sortOrder = sortOrder
        )
    }
}
