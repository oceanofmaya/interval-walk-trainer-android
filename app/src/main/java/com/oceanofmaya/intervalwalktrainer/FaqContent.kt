package com.oceanofmaya.intervalwalktrainer

/**
 * Ordered FAQ sections and entries for the Help bottom sheet.
 */
object FaqContent {
    val items: List<FaqListItem> = buildList {
        addSection(
            R.string.faq_section_getting_started,
            FaqEntry(R.string.faq_question_interval_walking, R.string.faq_answer_interval_walking),
            FaqEntry(R.string.faq_question_why_interval_walking, R.string.faq_answer_why_interval_walking),
            FaqEntry(R.string.faq_question_slow_fast_mean, R.string.faq_answer_slow_fast_mean),
            FaqEntry(R.string.faq_question_choose_formula, R.string.faq_answer_choose_formula),
            FaqEntry(R.string.faq_question_interval_vs_circuit, R.string.faq_answer_interval_vs_circuit)
        )
        addSection(
            R.string.faq_section_during_workout,
            FaqEntry(R.string.faq_question_pause_reset, R.string.faq_answer_pause_reset),
            FaqEntry(R.string.faq_question_countdown, R.string.faq_answer_countdown),
            FaqEntry(R.string.faq_question_notifications, R.string.faq_answer_notifications),
            FaqEntry(R.string.faq_question_voice, R.string.faq_answer_voice),
            FaqEntry(R.string.faq_question_voice_languages, R.string.faq_answer_voice_languages),
            FaqEntry(R.string.faq_question_keep_screen_awake, R.string.faq_answer_keep_screen_awake)
        )
        addSection(
            R.string.faq_section_formulas_presets,
            FaqEntry(R.string.faq_question_custom_formula_saved, R.string.faq_answer_custom_formula_saved)
        )
        addSection(
            R.string.faq_section_history_insights,
            FaqEntry(R.string.faq_question_workout_history, R.string.faq_answer_workout_history),
            FaqEntry(R.string.faq_question_weekly_goals, R.string.faq_answer_weekly_goals),
            FaqEntry(R.string.faq_question_insight_cards, R.string.faq_answer_insight_cards)
        )
        addSection(
            R.string.faq_section_health_connect,
            FaqEntry(R.string.faq_question_workout_metrics, R.string.faq_answer_workout_metrics),
            FaqEntry(R.string.faq_question_metrics_privacy, R.string.faq_answer_metrics_privacy)
        )
        addSection(
            R.string.faq_section_privacy_permissions,
            FaqEntry(R.string.faq_question_data_shared, R.string.faq_answer_data_shared),
            FaqEntry(
                R.string.faq_question_physical_activity_permission,
                R.string.faq_answer_physical_activity_permission
            )
        )
        addSection(
            R.string.faq_section_troubleshooting,
            FaqEntry(R.string.faq_question_background, R.string.faq_answer_background),
            FaqEntry(
                R.string.faq_question_workout_stops_background,
                R.string.faq_answer_workout_stops_background
            )
        )
        addSection(
            R.string.faq_section_safety,
            FaqEntry(R.string.faq_question_safe, R.string.faq_answer_safe)
        )
    }

    private fun MutableList<FaqListItem>.addSection(
        titleResId: Int,
        vararg entries: FaqEntry
    ) {
        add(FaqListItem.Section(titleResId))
        entries.forEach { add(FaqListItem.Entry(it)) }
    }
}

sealed interface FaqListItem {
    data class Section(val titleResId: Int) : FaqListItem
    data class Entry(val entry: FaqEntry) : FaqListItem
}
