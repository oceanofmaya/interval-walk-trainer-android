@file:Suppress("MatchingDeclarationName")

package com.oceanofmaya.intervalwalktrainer

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class WearSyncManager(context: Context) {
    private val dataClient = Wearable.getDataClient(context.applicationContext)
    private var lastPublishedFormula: IntervalFormula? = null
    private var lastPublishedPresetCatalog: List<IntervalFormula>? = null

    fun publishActiveFormula(formula: IntervalFormula) {
        if (formula == lastPublishedFormula) return
        val request = PutDataMapRequest.create(WearSyncContract.ACTIVE_FORMULA_PATH).apply {
            dataMap.putFormula(formula)
        }.asPutDataRequest().setUrgent()

        dataClient.putDataItem(request)
            .addOnSuccessListener { lastPublishedFormula = formula }
            .addOnFailureListener { error ->
                Log.w(TAG, "Unable to publish active formula to Wear OS", error)
            }
    }

    fun publishPresetCatalog(presets: List<IntervalFormula>) {
        if (presets == lastPublishedPresetCatalog) return
        val presetMaps = ArrayList(presets.map { formula ->
            DataMap().apply { putFormula(formula) }
        })
        val request = PutDataMapRequest.create(WearSyncContract.PRESET_CATALOG_PATH).apply {
            dataMap.putDataMapArrayList(WearSyncContract.KEY_PRESETS, presetMaps)
        }.asPutDataRequest().setUrgent()

        dataClient.putDataItem(request)
            .addOnSuccessListener { lastPublishedPresetCatalog = presets.toList() }
            .addOnFailureListener { error ->
                Log.w(TAG, "Unable to publish preset catalog to Wear OS", error)
            }
    }

    private fun DataMap.putFormula(formula: IntervalFormula) {
        putString(WearSyncContract.KEY_FORMULA_NAME, formula.name)
        putInt(WearSyncContract.KEY_SLOW_SECONDS, formula.slowDurationSeconds)
        putInt(WearSyncContract.KEY_FAST_SECONDS, formula.fastDurationSeconds)
        putInt(WearSyncContract.KEY_TOTAL_INTERVALS, formula.totalIntervals)
        putBoolean(WearSyncContract.KEY_STARTS_WITH_FAST, formula.startsWithFast)
        putBoolean(WearSyncContract.KEY_IS_CIRCUIT, formula.isCircuit)
    }

    private companion object {
        const val TAG = "WearSyncManager"
    }
}
