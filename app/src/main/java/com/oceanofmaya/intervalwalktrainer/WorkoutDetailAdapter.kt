package com.oceanofmaya.intervalwalktrainer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * RecyclerView adapter for displaying individual workout details in the workout detail bottom sheet.
 *
 * @param sessions List of workout sessions for the date (ordered by timestamp descending)
 * @param onDeleteClick Optional callback when user requests delete for a session
 */
class WorkoutDetailAdapter(
    private val sessions: List<WorkoutSession>,
    private val metricsEnabled: Boolean = false,
    private val onDeleteClick: ((WorkoutSession) -> Unit)? = null
) : RecyclerView.Adapter<WorkoutDetailAdapter.WorkoutDetailViewHolder>() {

    class WorkoutDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val workoutNumber: TextView = itemView.findViewById(R.id.workoutDetailItemNumber)
        val workoutType: TextView = itemView.findViewById(R.id.workoutDetailItemType)
        val workoutMeta: TextView = itemView.findViewById(R.id.workoutDetailItemMeta)
        val workoutMetricsContainer: LinearLayout = itemView.findViewById(R.id.workoutDetailItemMetricsContainer)
        val workoutMetrics: TextView = itemView.findViewById(R.id.workoutDetailItemMetrics)
        val deleteButton: ImageButton = itemView.findViewById(R.id.workoutDetailItemDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutDetailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_detail, parent, false)
        return WorkoutDetailViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutDetailViewHolder, position: Int) {
        val context = holder.itemView.context
        val session = sessions[position]
        val workoutNumber = position + 1

        holder.workoutNumber.text = context.getString(R.string.format_workout_number, workoutNumber)

        val displayType = session.workoutType.replace("1 Rounds", "1 Round")
        holder.workoutType.text = displayType

        holder.workoutMeta.text = formatMetaLine(context, session)
        bindMetaIcon(holder)

        val showMetricPlaceholders = WorkoutPhaseMetricsDisplay.shouldShowMetricPlaceholders(
            metricsEnabled = metricsEnabled,
            session = session
        )
        val metricsText = WorkoutMetricsUiFormatter(context).detailedMetricsRichText(
            session = session,
            showMetricPlaceholders = showMetricPlaceholders
        )
        holder.workoutMetrics.text = metricsText ?: ""
        holder.workoutMetricsContainer.visibility = if (metricsText != null) View.VISIBLE else View.GONE

        holder.deleteButton.visibility = if (onDeleteClick != null) View.VISIBLE else View.GONE
        holder.deleteButton.setOnClickListener {
            onDeleteClick?.invoke(session)
        }
    }

    override fun getItemCount() = sessions.size

    private fun formatMetaLine(context: android.content.Context, session: WorkoutSession): String {
        val minutesText = formatMinutes(context, session.minutes)
        val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
        val timeText = timeFormat.format(Date(session.timestamp))
        return context.getString(
            R.string.format_workout_detail_meta,
            minutesText,
            timeText
        )
    }

    private fun bindMetaIcon(holder: WorkoutDetailViewHolder) {
        val context = holder.itemView.context
        val density = context.resources.displayMetrics.density
        val iconSize = (META_ICON_SIZE_DP * density).roundToInt()
        val clockIcon = ContextCompat.getDrawable(context, R.drawable.outline_timer_24)
            ?.mutate()
            ?.apply {
                setBounds(0, 0, iconSize, iconSize)
                setTint(ContextCompat.getColor(context, R.color.text_secondary))
            }
        holder.workoutMeta.includeFontPadding = false
        holder.workoutMeta.setCompoundDrawables(clockIcon, null, null, null)
        holder.workoutMeta.compoundDrawablePadding = (META_ICON_PADDING_DP * density).roundToInt()
    }

    private fun formatMinutes(context: android.content.Context, minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) {
            context.getString(R.string.format_time_hr_min, hours, mins)
        } else {
            context.getString(R.string.format_time_min, mins)
        }
    }

    private companion object {
        const val META_ICON_SIZE_DP = 14
        const val META_ICON_PADDING_DP = 4
    }
}
