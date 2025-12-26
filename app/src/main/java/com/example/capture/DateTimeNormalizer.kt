package com.example.capture

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Data class representing raw extracted event information from OCR
 */
data class RawEventData(
    val title: String?,
    val date: String?,
    val time: String?,
    val location: String?,
    val description: String?,
    val rawText: String?
)

/**
 * Data class representing structured date/time information for calendar systems
 */
data class StructuredDateTime(
    val startDateTime: Long?,  // Unix timestamp in milliseconds
    val endDateTime: Long?,    // Unix timestamp in milliseconds (for ranges)
    val allDay: Boolean = false,
    val year: Int?,
    val month: Int?,           // 1-12 (Calendar.MONTH + 1)
    val day: Int?,
    val hour: Int?,            // 0-23 (24-hour format)
    val minute: Int?
)

/**
 * Normalizes parsed date/time strings into structured format
 * Single Responsibility: Date/time format conversion and validation
 */
class DateTimeNormalizer {

    private data class DateComponents(val year: Int, val month: Int, val day: Int)
    private data class TimeComponents(val startHour: Int, val startMinute: Int, val endHour: Int?, val endMinute: Int?)

    /**
     * Converts parsed date and time strings into structured format for calendar integration
     */
    fun normalize(dateStr: String?, timeStr: String?): StructuredDateTime? {
        if (dateStr == null) return null

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Parse date
        val dateComponents = parseDateString(dateStr, currentYear) ?: return null
        
        // Parse time (if available)
        val timeComponents = if (timeStr != null) parseTimeString(timeStr) else null
        
        // Build calendar with date
        calendar.set(Calendar.YEAR, dateComponents.year)
        calendar.set(Calendar.MONTH, dateComponents.month - 1)  // Calendar.MONTH is 0-based
        calendar.set(Calendar.DAY_OF_MONTH, dateComponents.day)
        
        val allDay = timeComponents == null
        
        if (timeComponents != null) {
            calendar.set(Calendar.HOUR_OF_DAY, timeComponents.startHour)
            calendar.set(Calendar.MINUTE, timeComponents.startMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        } else {
            // For all-day events, set to start of day
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
        
        val startTimestamp = calendar.timeInMillis
        
        // Calculate end time if time range exists
        val endTimestamp = if (timeComponents?.endHour != null && timeComponents.endMinute != null) {
            calendar.set(Calendar.HOUR_OF_DAY, timeComponents.endHour)
            calendar.set(Calendar.MINUTE, timeComponents.endMinute)
            calendar.timeInMillis
        } else null

        Log.d("DateTimeNormalizer", "Normalized date: $dateComponents, time: $timeComponents")

        return StructuredDateTime(
            startDateTime = startTimestamp,
            endDateTime = endTimestamp,
            allDay = allDay,
            year = dateComponents.year,
            month = dateComponents.month,
            day = dateComponents.day,
            hour = timeComponents?.startHour,
            minute = timeComponents?.startMinute
        )
    }

    /**
     * Parses various date string formats into structured components
     */
    private fun parseDateString(dateStr: String, defaultYear: Int): DateComponents? {
        val dateStrTrimmed = dateStr.trim()
        
        // Try ISO format: 2024-12-31
        val isoPattern = """(\d{4})[-/](\d{1,2})[-/](\d{1,2})""".toRegex()
        isoPattern.find(dateStrTrimmed)?.let {
            val (year, month, day) = it.destructured
            return DateComponents(year.toInt(), month.toInt(), day.toInt())
        }
        
        // Try numeric format: 12/31/2024 or 12-31-2024
        val numericPattern = """(\d{1,2})[-/](\d{1,2})[-/](\d{2,4})""".toRegex()
        numericPattern.find(dateStrTrimmed)?.let {
            val (month, day, year) = it.destructured
            val fullYear = if (year.length == 2) 2000 + year.toInt() else year.toInt()
            return DateComponents(fullYear, month.toInt(), day.toInt())
        }
        
        // Try month name format: "21st January", "Jan 21", "December 31, 2024"
        val monthMap = mapOf(
            "jan" to 1, "january" to 1,
            "feb" to 2, "february" to 2,
            "mar" to 3, "march" to 3,
            "apr" to 4, "april" to 4,
            "may" to 5,
            "jun" to 6, "june" to 6,
            "jul" to 7, "july" to 7,
            "aug" to 8, "august" to 8,
            "sep" to 9, "sept" to 9, "september" to 9,
            "oct" to 10, "october" to 10,
            "nov" to 11, "november" to 11,
            "dec" to 12, "december" to 12
        )
        
        // Pattern: "21st January" or "Jan 21st" or "January 21, 2024"
        val monthNamePattern = """(\d{1,2})(?:st|nd|rd|th)?\s+(\w+)(?:,?\s+(\d{4}))?""".toRegex(RegexOption.IGNORE_CASE)
        monthNamePattern.find(dateStrTrimmed)?.let {
            val day = it.groupValues[1].toInt()
            val monthStr = it.groupValues[2].lowercase()
            val year = it.groupValues[3].toIntOrNull() ?: defaultYear
            val month = monthMap[monthStr] ?: return null
            return DateComponents(year, month, day)
        }
        
        // Pattern: "January 21" or "Dec 31, 2024"
        val monthFirstPattern = """(\w+)\s+(\d{1,2})(?:st|nd|rd|th)?(?:,?\s+(\d{4}))?""".toRegex(RegexOption.IGNORE_CASE)
        monthFirstPattern.find(dateStrTrimmed)?.let {
            val monthStr = it.groupValues[1].lowercase()
            val day = it.groupValues[2].toInt()
            val year = it.groupValues[3].toIntOrNull() ?: defaultYear
            val month = monthMap[monthStr] ?: return null
            return DateComponents(year, month, day)
        }
        
        // Pattern: Just ordinal "21st" - assume current month/year
        val ordinalPattern = """^(\d{1,2})(?:st|nd|rd|th)$""".toRegex()
        ordinalPattern.find(dateStrTrimmed)?.let {
            val day = it.groupValues[1].toInt()
            val calendar = Calendar.getInstance()
            return DateComponents(defaultYear, calendar.get(Calendar.MONTH) + 1, day)
        }
        
        return null
    }

    /**
     * Parses time strings into structured components
     * Handles formats like "9:00 AM", "3:30 PM", "9:00 AM - 11:30 AM"
     */
    private fun parseTimeString(timeStr: String): TimeComponents? {
        val timeStrTrimmed = timeStr.trim()
        
        // Check for time range
        val rangePattern = """(\d{1,2})(?::(\d{2}))?\s*(AM|PM)?\s*(?:-|to)\s*(\d{1,2}):(\d{2})\s*(AM|PM)?""".toRegex(RegexOption.IGNORE_CASE)
        rangePattern.find(timeStrTrimmed)?.let {
            val startHour = it.groupValues[1].toInt()
            val startMinute = it.groupValues[2].toIntOrNull() ?: 0
            val startAmPm = it.groupValues[3].uppercase()
            val endHour = it.groupValues[4].toInt()
            val endMinute = it.groupValues[5].toInt()
            val endAmPm = it.groupValues[6].uppercase()
            
            val start24Hour = convertTo24Hour(startHour, startMinute, startAmPm.ifEmpty { endAmPm })
            val end24Hour = convertTo24Hour(endHour, endMinute, endAmPm)
            
            return TimeComponents(start24Hour.first, start24Hour.second, end24Hour.first, end24Hour.second)
        }
        
        // Single time: "9:00 AM" or "3:30 PM"
        val singlePattern = """(\d{1,2})(?::(\d{1,2}))?\s*(AM|PM)""".toRegex(RegexOption.IGNORE_CASE)
        singlePattern.find(timeStrTrimmed)?.let {
            val hour = it.groupValues[1].toInt()
            val minute = it.groupValues[2].toIntOrNull() ?: 0
            val amPm = it.groupValues[3].uppercase()
            
            val (hour24, minute24) = convertTo24Hour(hour, minute, amPm)
            return TimeComponents(hour24, minute24, null, null)
        }
        
        return null
    }

    /**
     * Converts 12-hour time to 24-hour format
     */
    private fun convertTo24Hour(hour: Int, minute: Int, amPm: String): Pair<Int, Int> {
        var hour24 = hour
        if (amPm == "PM" && hour != 12) {
            hour24 += 12
        } else if (amPm == "AM" && hour == 12) {
            hour24 = 0
        }
        return Pair(hour24, minute)
    }
}
