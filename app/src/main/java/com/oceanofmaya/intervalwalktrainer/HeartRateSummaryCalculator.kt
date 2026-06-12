package com.oceanofmaya.intervalwalktrainer

object HeartRateSummaryCalculator {
    fun summarize(samples: List<Int>): HeartRateSummary? {
        val validSamples = samples.filter { it > 0 }
        if (validSamples.isEmpty()) return null
        return HeartRateSummary(
            averageBpm = validSamples.average().toInt(),
            minBpm = validSamples.min(),
            maxBpm = validSamples.max()
        )
    }
}

data class HeartRateSummary(
    val averageBpm: Int,
    val minBpm: Int,
    val maxBpm: Int
)
