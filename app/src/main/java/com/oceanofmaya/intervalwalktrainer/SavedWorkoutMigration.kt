package com.oceanofmaya.intervalwalktrainer

/**
 * Pure helper for legacy SharedPreferences -> Room migration decisions.
 *
 * Keeping the decision logic free of Android dependencies makes it easy to unit-test
 * the verified-insert / cap-aware policy: insert the legacy custom formula exactly once,
 * mark the prefs flag only after the insert is verified, and skip re-migrating on upgrade.
 */
object SavedWorkoutMigration {

    sealed interface Decision {
        /** Migration already done, or user has no custom formula to migrate. */
        data object Skip : Decision
        /** Cap is full; mark legacy migrated to avoid re-evaluating, do not insert. */
        data object MarkMigratedOnly : Decision
        /** Insert the entity, then (only if read-back succeeds) mark legacy migrated. */
        data class InsertAndMark(val entity: SavedWorkout) : Decision
    }

    fun decide(
        alreadyMigrated: Boolean,
        hasCustomFormula: Boolean,
        legacyFormula: IntervalFormula?,
        circuitPattern: String,
        savedCount: Int,
        cap: Int = SavedWorkoutRepository.MAX_SAVED_WORKOUTS
    ): Decision = when {
        alreadyMigrated -> Decision.Skip
        !hasCustomFormula -> Decision.Skip
        // Legacy custom flag is set but the stored formula is unreadable/corrupt: nothing to migrate,
        // but mark complete so we don't re-attempt this on every cold start.
        legacyFormula == null -> Decision.MarkMigratedOnly
        savedCount >= cap -> Decision.MarkMigratedOnly
        else -> Decision.InsertAndMark(
            SavedWorkoutRepository.fromFormulaForMigration(
                formula = legacyFormula,
                circuitPattern = circuitPattern,
                sortOrder = 0
            )
        )
    }
}
