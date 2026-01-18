package com.oceanofmaya.intervalwalktrainer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView adapter for displaying workout records in a list.
 * Used in the StatsActivity to show workouts for the selected month.
 * 
 * @param workouts List of workout records to display (should be in descending order by date)
 * @param onItemClick Optional callback invoked when a workout item is clicked
 */
class WorkoutListAdapter(
    private val workouts: List<WorkoutRecord>,
    private val onItemClick: ((WorkoutRecord) -> Unit)? = null
) : RecyclerView.Adapter<WorkoutListAdapter.WorkoutViewHolder>() {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
    
    /**
     * ViewHolder for workout items.
     * 
     * @param itemView The root view of the item layout
     */
    class WorkoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val workoutDate: TextView = itemView.findViewById(R.id.workoutDate)
        val workoutCount: TextView = itemView.findViewById(R.id.workoutCount)
        val workoutMinutes: TextView = itemView.findViewById(R.id.workoutMinutes)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout, parent, false)
        return WorkoutViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = workouts[position]
        val context = holder.itemView.context
        
        // Format date
        try {
            val date = dateFormat.parse(workout.date)
            if (date != null) {
                holder.workoutDate.text = displayDateFormat.format(date)
            } else {
                holder.workoutDate.text = workout.date
            }
        } catch (e: Exception) {
            holder.workoutDate.text = workout.date
        }
        
        // Format workout count
        val workoutText = if (workout.completedWorkouts == 1) {
            context.getString(R.string.workout_singular)
        } else {
            context.getString(R.string.workout_plural, workout.completedWorkouts)
        }
        holder.workoutCount.text = workoutText
        
        // Format minutes
        val minutesText = formatMinutes(context, workout.totalMinutes)
        holder.workoutMinutes.text = minutesText
        
        // Set click listener if provided
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(workout)
        }
    }
    
    override fun getItemCount() = workouts.size
    
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

