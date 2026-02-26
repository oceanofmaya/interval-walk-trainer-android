package com.oceanofmaya.intervalwalktrainer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for displaying individual workout details in the workout detail bottom sheet.
 *
 * @param sessions List of workout sessions for the date (ordered by timestamp descending)
 * @param onDeleteClick Optional callback when user requests delete for a session
 */
class WorkoutDetailAdapter(
    private val sessions: List<WorkoutSession>,
    private val onDeleteClick: ((WorkoutSession) -> Unit)? = null
) : RecyclerView.Adapter<WorkoutDetailAdapter.WorkoutDetailViewHolder>() {

    /**
     * ViewHolder for workout detail items.
     */
    class WorkoutDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val workoutNumber: TextView = itemView.findViewById(R.id.workoutDetailItemNumber)
        val workoutType: TextView = itemView.findViewById(R.id.workoutDetailItemType)
        val workoutTime: TextView = itemView.findViewById(R.id.workoutDetailItemTime)
        val workoutMinutes: TextView = itemView.findViewById(R.id.workoutDetailItemMinutes)
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
        
        // Format workout number
        holder.workoutNumber.text = context.getString(R.string.workout_number, workoutNumber)
        
        // Display actual workout type, fixing singular form for "1 Rounds" -> "1 Round"
        val displayType = session.workoutType.replace("1 Rounds", "1 Round")
        holder.workoutType.text = displayType
        
        // Display session time (completion timestamp)
        val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
        holder.workoutTime.text = timeFormat.format(Date(session.timestamp))
        holder.workoutTime.visibility = View.VISIBLE

        // Format minutes
        val minutesText = formatMinutes(context, session.minutes)
        holder.workoutMinutes.text = minutesText

        holder.deleteButton.visibility = if (onDeleteClick != null) View.VISIBLE else View.GONE
        holder.deleteButton.setOnClickListener {
            onDeleteClick?.invoke(session)
        }
    }
    
    override fun getItemCount() = sessions.size
    
    /**
     * Formats minutes into a readable string.
     */
    private fun formatMinutes(context: android.content.Context, minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) {
            context.getString(R.string.time_format_hr_min, hours, mins)
        } else {
            context.getString(R.string.time_format_min, mins)
        }
    }
}

