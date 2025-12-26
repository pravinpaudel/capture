package com.example.capture

import android.content.Intent
import android.provider.CalendarContract

/**
 * Formats event data for Android Calendar integration.
 * Responsibility: Calendar intent creation
 */
class CalendarFormatter {

    /**
     * Creates an Intent to add event to Android Calendar using CalendarContract
     */
    fun createCalendarIntent(
        title: String?,
        description: String?,
        location: String?,
        structuredDateTime: StructuredDateTime?,
        reminderMinutes: Int? = null
    ): Intent {
        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            
            // Basic event info
            title?.let { putExtra(CalendarContract.Events.TITLE, it) }
            description?.let { putExtra(CalendarContract.Events.DESCRIPTION, it) }
            location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
            
            // Date and time
            if (structuredDateTime != null) {
                structuredDateTime.startDateTime?.let {
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it)
                }
                
                structuredDateTime.endDateTime?.let {
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it)
                } ?: run {
                    // If no end time, default to 1 hour after start
                    structuredDateTime.startDateTime?.let { start ->
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 3600000) // +1 hour
                    }
                }
                
                putExtra(CalendarContract.Events.ALL_DAY, structuredDateTime.allDay)
            }
            
            // Reminder
            reminderMinutes?.let {
                if (it >= 0) {
                    putExtra(CalendarContract.Reminders.MINUTES, it)
                    putExtra(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
            }
        }
    }

    /**
     * Converts reminder text to minutes
     */
    fun parseReminderText(reminderText: String): Int {
        return when (reminderText) {
            "At time of event" -> 0
            "5 minutes before" -> 5
            "10 minutes before" -> 10
            "15 minutes before" -> 15
            "30 minutes before" -> 30
            "1 hour before" -> 60
            "1 day before" -> 1440
            "None" -> -1
            else -> -1
        }
    }
}
