package com.oceanofmaya.intervalwalktrainer

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

class HealthConnectMetricsSource(private val context: Context) {
    private val heartRatePermission = HealthPermission.getReadPermission(HeartRateRecord::class)
    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)

    val requiredPermissions: Set<String> = setOf(heartRatePermission, stepsPermission)

    fun isAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasAnyPermission(): Boolean {
        if (!isAvailable()) return false
        val granted = HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
        return granted.any { it in requiredPermissions }
    }

    suspend fun missingPermissions(): Set<String> {
        if (!isAvailable()) return emptySet()
        val granted = HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
        return requiredPermissions - granted
    }

    suspend fun readSummary(
        intervals: List<WorkoutMetricsInterval>,
        phaseWindows: List<WorkoutPhaseWindow> = emptyList()
    ): WorkoutMetricsSummary? {
        if (intervals.isEmpty() || !isAvailable()) return null
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        val stepCount = if (stepsPermission in granted) {
            runCatching { readStepCount(client, intervals) }.getOrNull()
        } else {
            null
        }
        val heartRateSummary = if (heartRatePermission in granted) {
            runCatching { readHeartRateSummary(client, intervals) }.getOrNull()
        } else {
            null
        }
        val phaseHeartRateSummary = if (heartRatePermission in granted && phaseWindows.isNotEmpty()) {
            runCatching { readPhaseHeartRateSummary(client, intervals, phaseWindows) }.getOrNull()
        } else {
            null
        }
        return WorkoutMetricsSummary(
            stepCount = stepCount,
            averageHeartRateBpm = heartRateSummary?.averageBpm,
            minHeartRateBpm = heartRateSummary?.minBpm,
            maxHeartRateBpm = heartRateSummary?.maxBpm,
            stepSource = stepCount?.let { WorkoutMetricsSources.HEALTH_CONNECT },
            heartRateSource = heartRateSummary?.let { WorkoutMetricsSources.HEALTH_CONNECT },
            phaseWindows = phaseWindows,
            fastPhaseAverageHeartRateBpm = phaseHeartRateSummary?.fastPhaseAverageBpm,
            slowPhaseAverageHeartRateBpm = phaseHeartRateSummary?.slowPhaseAverageBpm
        ).takeIf { it.hasDisplayableValue }
    }

    private suspend fun readPhaseHeartRateSummary(
        client: HealthConnectClient,
        workoutIntervals: List<WorkoutMetricsInterval>,
        phaseWindows: List<WorkoutPhaseWindow>
    ): PhaseHeartRateSummary {
        val timestampedSamples = readHeartRateSamplesWithTimestamps(client, workoutIntervals)
        return PhaseHeartRateSummary(
            fastPhaseAverageBpm = readPhaseTypeAverageBpm(
                client = client,
                phaseWindows = phaseWindows,
                phaseType = WorkoutPhaseType.FAST,
                timestampedSamples = timestampedSamples
            ),
            slowPhaseAverageBpm = readPhaseTypeAverageBpm(
                client = client,
                phaseWindows = phaseWindows,
                phaseType = WorkoutPhaseType.SLOW,
                timestampedSamples = timestampedSamples
            )
        )
    }

    private suspend fun readPhaseTypeAverageBpm(
        client: HealthConnectClient,
        phaseWindows: List<WorkoutPhaseWindow>,
        phaseType: WorkoutPhaseType,
        timestampedSamples: List<TimestampedHeartRateSample>
    ): Int? {
        val windows = phaseWindows.filter { it.phase == phaseType }
        if (windows.isEmpty()) {
            return null
        }

        val phaseBpms = timestampedSamples
            .filter { sample -> windows.any { window -> window.contains(sample.timeMillis) } }
            .map { it.bpm }
        val sampleAverage = HeartRateSummaryCalculator.summarize(phaseBpms)?.averageBpm

        val windowIntervals = windows.map { window ->
            WorkoutMetricsInterval(window.startedAtMillis, window.endedAtMillis)
        }
        val aggregateAverage = readHeartRateSummaryFromAggregate(client, windowIntervals)?.averageBpm
        return sampleAverage ?: aggregateAverage
    }

    private fun WorkoutPhaseWindow.contains(timeMillis: Long): Boolean {
        return timeMillis in startedAtMillis until endedAtMillis
    }

    private suspend fun readStepCount(
        client: HealthConnectClient,
        intervals: List<WorkoutMetricsInterval>
    ): Int? {
        val total = intervals.sumOf { interval ->
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = interval.toTimeRangeFilter()
                )
            )
            response[StepsRecord.COUNT_TOTAL] ?: 0L
        }
        return total.toInt().takeIf { it >= MIN_DISPLAYABLE_STEP_COUNT }
    }

    private suspend fun readHeartRateSummary(
        client: HealthConnectClient,
        intervals: List<WorkoutMetricsInterval>
    ): HeartRateSummary? {
        val aggregateSummary = readHeartRateSummaryFromAggregate(client, intervals)
        val sampleSummary = HeartRateSummaryCalculator.summarize(
            intervals.flatMap { interval -> readHeartRateSamples(client, interval) }
        )
        return mergeHeartRateSummaries(aggregateSummary, sampleSummary)
    }

    private fun mergeHeartRateSummaries(
        aggregateSummary: HeartRateSummary?,
        sampleSummary: HeartRateSummary?
    ): HeartRateSummary? {
        return when {
            aggregateSummary == null -> sampleSummary
            sampleSummary == null -> aggregateSummary
            else -> HeartRateSummary(
                averageBpm = aggregateSummary.averageBpm,
                minBpm = minOf(aggregateSummary.minBpm, sampleSummary.minBpm),
                maxBpm = maxOf(aggregateSummary.maxBpm, sampleSummary.maxBpm)
            )
        }
    }

    private suspend fun readHeartRateSummaryFromAggregate(
        client: HealthConnectClient,
        intervals: List<WorkoutMetricsInterval>
    ): HeartRateSummary? {
        val averageBpms = mutableListOf<Int>()
        var minBpm: Int? = null
        var maxBpm: Int? = null

        for (interval in intervals) {
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        HeartRateRecord.BPM_AVG,
                        HeartRateRecord.BPM_MIN,
                        HeartRateRecord.BPM_MAX
                    ),
                    timeRangeFilter = interval.toTimeRangeFilter()
                )
            )
            val averageBpm = response[HeartRateRecord.BPM_AVG]?.toInt()?.takeIf { it > 0 }
            val intervalMinBpm = response[HeartRateRecord.BPM_MIN]?.toInt()?.takeIf { it > 0 }
            val intervalMaxBpm = response[HeartRateRecord.BPM_MAX]?.toInt()?.takeIf { it > 0 }

            averageBpm?.let { averageBpms += it }
            intervalMinBpm?.let { bpm -> minBpm = minBpm?.coerceAtMost(bpm) ?: bpm }
            intervalMaxBpm?.let { bpm -> maxBpm = maxBpm?.coerceAtLeast(bpm) ?: bpm }
        }

        if (averageBpms.isEmpty()) return null
        return HeartRateSummary(
            averageBpm = averageBpms.average().toInt(),
            minBpm = minBpm ?: averageBpms.min(),
            maxBpm = maxBpm ?: averageBpms.max()
        )
    }

    private suspend fun readHeartRateSamples(
        client: HealthConnectClient,
        interval: WorkoutMetricsInterval
    ): List<Int> {
        return readHeartRateSamplesWithTimestamps(client, listOf(interval)).map { it.bpm }
    }

    private suspend fun readHeartRateSamplesWithTimestamps(
        client: HealthConnectClient,
        intervals: List<WorkoutMetricsInterval>
    ): List<TimestampedHeartRateSample> {
        if (intervals.isEmpty()) return emptyList()
        val samples = mutableListOf<TimestampedHeartRateSample>()
        for (interval in intervals) {
            var pageToken: String? = null
            do {
                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = interval.toTimeRangeFilter(),
                        pageToken = pageToken
                    )
                )
                response.records.forEach { record ->
                    record.samples.forEach { sample ->
                        samples += TimestampedHeartRateSample(
                            timeMillis = sample.time.toEpochMilli(),
                            bpm = sample.beatsPerMinute.toInt()
                        )
                    }
                }
                pageToken = response.pageToken
            } while (pageToken != null)
        }
        return samples
    }

    private fun WorkoutMetricsInterval.toTimeRangeFilter(): TimeRangeFilter {
        return TimeRangeFilter.between(
            Instant.ofEpochMilli(startedAtMillis),
            Instant.ofEpochMilli(endedAtMillis)
        )
    }

    private data class TimestampedHeartRateSample(
        val timeMillis: Long,
        val bpm: Int
    )

    private companion object {
        const val MIN_DISPLAYABLE_STEP_COUNT = 10
    }
}
