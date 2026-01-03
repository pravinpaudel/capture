package com.example.capture

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Helper class for direct calendar operations using ContentResolver.
 * Handles event insertion and multiple reminder creation according to CalendarContract.
 */
class CalendarHelper(private val context: Context) {

    companion object {
        private const val TAG = "CalendarHelper"
    }

    /**
     * Data class representing a single reminder
     */
    data class Reminder(
        val minutes: Int,
        val method: Int = CalendarContract.Reminders.METHOD_ALERT
    )

    /**
     * Result of calendar event insertion
     */
    data class InsertResult(
        val success: Boolean,
        val eventId: Long? = null,
        val message: String = ""
    )

    /**
     * Check if calendar permissions are granted
     */
    fun hasCalendarPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get the primary calendar ID for the device
     * Returns null if no calendar is found
     */
    private fun getPrimaryCalendarId(): Long? {
        if (!hasCalendarPermissions()) {
            Log.e(TAG, "Calendar permissions not granted")
            return null
        }

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.IS_PRIMARY
        )

        val contentResolver: ContentResolver = context.contentResolver
        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )

            cursor?.use {
                // First, try to find the primary calendar
                while (it.moveToNext()) {
                    val calendarId = it.getLong(0)
                    val isPrimary = if (it.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY) != -1) {
                        it.getInt(it.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY))
                    } else {
                        0
                    }

                    if (isPrimary == 1) {
                        Log.d(TAG, "Found primary calendar with ID: $calendarId")
                        return calendarId
                    }
                }

                // If no primary calendar found, return the first one
                it.moveToFirst()
                if (it.count > 0) {
                    val calendarId = it.getLong(0)
                    Log.d(TAG, "Using first available calendar with ID: $calendarId")
                    return calendarId
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting calendar ID", e)
        } finally {
            cursor?.close()
        }

        return null
    }

    /**
     * Insert an event into the calendar with multiple reminders
     */
    fun insertEvent(
        title: String,
        description: String?,
        location: String?,
        structuredDateTime: StructuredDateTime,
        reminders: List<Reminder> = emptyList()
    ): InsertResult {
        if (!hasCalendarPermissions()) {
            return InsertResult(
                success = false,
                message = "Calendar permissions not granted"
            )
        }

        val calendarId = getPrimaryCalendarId()
        if (calendarId == null) {
            return InsertResult(
                success = false,
                message = "No calendar found on device"
            )
        }

        val contentResolver: ContentResolver = context.contentResolver

        // Prepare event values
        val eventValues = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description ?: "")
            put(CalendarContract.Events.EVENT_LOCATION, location ?: "")
            
            // Date and time
            structuredDateTime.startDateTime?.let {
                put(CalendarContract.Events.DTSTART, it)
            }
            
            structuredDateTime.endDateTime?.let {
                put(CalendarContract.Events.DTEND, it)
            }
            
            put(CalendarContract.Events.ALL_DAY, if (structuredDateTime.allDay) 1 else 0)
            
            // Set timezone
            put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            
            // Event status
            put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
            
            // Has alarm flag (set to 1 if we have reminders)
            put(CalendarContract.Events.HAS_ALARM, if (reminders.isNotEmpty()) 1 else 0)
        }

        try {
            // Insert the event
            val eventUri: Uri? = contentResolver.insert(
                CalendarContract.Events.CONTENT_URI,
                eventValues
            )

            if (eventUri == null) {
                return InsertResult(
                    success = false,
                    message = "Failed to insert event"
                )
            }

            val eventId = eventUri.lastPathSegment?.toLongOrNull()
            if (eventId == null) {
                return InsertResult(
                    success = false,
                    message = "Invalid event ID returned"
                )
            }

            Log.d(TAG, "Event inserted with ID: $eventId")

            // Insert reminders
            val reminderResults = mutableListOf<Boolean>()
            reminders.forEach { reminder ->
                val reminderInserted = insertReminder(contentResolver, eventId, reminder)
                reminderResults.add(reminderInserted)
            }

            val allRemindersInserted = reminderResults.all { it }
            val message = if (allRemindersInserted || reminders.isEmpty()) {
                "Event created successfully with ${reminders.size} reminder(s)"
            } else {
                "Event created but some reminders failed to insert"
            }

            return InsertResult(
                success = true,
                eventId = eventId,
                message = message
            )

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception inserting event", e)
            return InsertResult(
                success = false,
                message = "Permission denied: ${e.message}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting event", e)
            return InsertResult(
                success = false,
                message = "Error: ${e.message}"
            )
        }
    }

    /**
     * Insert a single reminder for an event
     * Returns true if successful, false otherwise
     */
    private fun insertReminder(
        contentResolver: ContentResolver,
        eventId: Long,
        reminder: Reminder
    ): Boolean {
        val reminderValues = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, reminder.minutes)
            put(CalendarContract.Reminders.METHOD, reminder.method)
        }

        return try {
            val reminderUri = contentResolver.insert(
                CalendarContract.Reminders.CONTENT_URI,
                reminderValues
            )
            
            if (reminderUri != null) {
                Log.d(TAG, "Reminder inserted: ${reminder.minutes} minutes before")
                true
            } else {
                Log.e(TAG, "Failed to insert reminder: ${reminder.minutes} minutes")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting reminder", e)
            false
        }
    }

    /**
     * Query all reminders for a specific event
     */
    fun getRemindersForEvent(eventId: Long): List<Reminder> {
        if (!hasCalendarPermissions()) {
            return emptyList()
        }

        val reminders = mutableListOf<Reminder>()
        val projection = arrayOf(
            CalendarContract.Reminders.MINUTES,
            CalendarContract.Reminders.METHOD
        )

        val selection = "${CalendarContract.Reminders.EVENT_ID} = ?"
        val selectionArgs = arrayOf(eventId.toString())

        val contentResolver: ContentResolver = context.contentResolver
        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(
                CalendarContract.Reminders.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val minutes = it.getInt(0)
                    val method = it.getInt(1)
                    reminders.add(Reminder(minutes, method))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying reminders", e)
        } finally {
            cursor?.close()
        }

        return reminders
    }
}
