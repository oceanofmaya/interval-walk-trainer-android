@file:Suppress("MatchingDeclarationName")

package com.oceanofmaya.intervalwalktrainer

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.google.android.material.button.MaterialButton

internal class WearCompanionSettingsController(
    private val activity: AppCompatActivity,
    private val statusView: TextView,
    private val actionButton: MaterialButton,
    accentColor: Int,
    private val onAction: (View) -> Unit
) {
    init {
        actionButton.setTextColor(accentColor)
    }

    fun refresh() {
        statusView.setText(R.string.wear_status_checking)
        actionButton.visibility = View.INVISIBLE
        actionButton.isEnabled = false
        Wearable.getNodeClient(activity).connectedNodes
            .addOnSuccessListener(::showConnectedNodeStatus)
            .addOnFailureListener { showNoConnectedWatch() }
    }

    private fun showConnectedNodeStatus(connectedNodes: List<Node>) {
        if (connectedNodes.isEmpty()) {
            showNoConnectedWatch()
            return
        }
        Wearable.getCapabilityClient(activity)
            .getCapability(WEAR_APP_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .addOnSuccessListener { capability ->
                val installedNodeIds = capability.nodes.mapTo(mutableSetOf()) { it.id }
                val nodeWithoutApp = connectedNodes.firstOrNull { it.id !in installedNodeIds }
                if (nodeWithoutApp == null) {
                    statusView.setText(R.string.wear_status_installed)
                    actionButton.visibility = View.GONE
                } else {
                    statusView.setText(R.string.wear_status_available)
                    configureAction(R.string.action_install_on_watch) {
                        openStoreOnWatch(nodeWithoutApp)
                    }
                }
            }
            .addOnFailureListener {
                showRetryStatus(R.string.wear_status_check_failed, ::refresh)
            }
    }

    private fun showNoConnectedWatch() {
        statusView.setText(R.string.wear_status_no_watch)
        configureAction(R.string.action_refresh, ::refresh)
    }

    private fun openStoreOnWatch(node: Node) {
        statusView.setText(R.string.wear_status_opening_store)
        actionButton.isEnabled = false
        val storeIntent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse(PLAY_STORE_APP_URI))
        val request = RemoteActivityHelper(activity)
            .startRemoteActivity(storeIntent, node.id)
        request.addListener(
            {
                try {
                    request.get()
                    statusView.setText(R.string.wear_status_store_opened)
                    actionButton.visibility = View.GONE
                } catch (_: Exception) {
                    showRetryStatus(R.string.wear_status_store_failed) {
                        openStoreOnWatch(node)
                    }
                }
            },
            ContextCompat.getMainExecutor(activity)
        )
    }

    private fun showRetryStatus(messageRes: Int, retryAction: () -> Unit) {
        statusView.setText(messageRes)
        configureAction(R.string.action_retry, retryAction)
    }

    private fun configureAction(labelRes: Int, action: () -> Unit) {
        actionButton.visibility = View.VISIBLE
        actionButton.isEnabled = true
        actionButton.setText(labelRes)
        actionButton.setOnClickListener {
            onAction(it)
            action()
        }
    }

    private companion object {
        const val WEAR_APP_CAPABILITY = "interval_walk_trainer_wear_app"
        const val PLAY_STORE_APP_URI =
            "market://details?id=com.oceanofmaya.intervalwalktrainer"
    }
}
