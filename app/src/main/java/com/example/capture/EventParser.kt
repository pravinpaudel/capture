package com.example.capture

import android.util.Log
import com.google.mlkit.vision.text.Text
import java.util.regex.Pattern

/**
 * Extracts raw event information from OCR text blocks
 * Single Responsibility: Text extraction and pattern matching
 */
class EventParser {

    // Static methods and variables
    companion object {
        // Date patterns
        private val DATE_PATTERNS = listOf(
            Pattern.compile(
                "\\b\\d{1,2}(st|nd|rd|th) (?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\b",
                Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                "\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s*\\d{1,2}(,)?\\s*\\d{4}\\b",
                Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                "\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]* \\d{1,2}(?:,?\\\\s*\\\\d{4})?",
                Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b"),
            Pattern.compile("\\b\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}\\b"),
            Pattern.compile(
                "\\b(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday),? (?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]* \\d{1,2}\\b",
                Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                "\\b\\d{1,2}(st|nd|rd|th)\\b",
                Pattern.CASE_INSENSITIVE
            ), // ordinal only (1st, 2nd, 3rd, 10th)
            Pattern.compile(
                "\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]* \\d{1,2}(st|nd|rd|th)\\b",
                Pattern.CASE_INSENSITIVE
            ), // month + ordinal (Jan 21st)
            Pattern.compile(
                "\\b\\d{1,2}(st|nd|rd|th) (?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\b",
                Pattern.CASE_INSENSITIVE
            ), // ordinal + month (21st JanPattern.compile)
        )

        // Time patterns
        private val TIME_PATTERNS = listOf(
            Pattern.compile(
                "\\b\\d{1,2}(?::\\d{2})?\\s*(?:AM|PM)?\\s*(?:-|to)\\s*\\d{1,2}:\\d{2}\\s*(?:AM|PM)?\\b",
                Pattern.CASE_INSENSITIVE
            ), // 9 - 11:30, 9:00AM - 11:30AM, 9:00 to 10:00 PM
            Pattern.compile(
                "\\b\\d{1,2}(?::\\d{0,2})?\\s*(?:AM|PM)\\b",
                Pattern.CASE_INSENSITIVE
            ), // 9AM, 3:30PM
        )

        // Location keywords and patterns
        private val LOCATION_KEYWORDS = listOf("at ", "venue:", "location:", "@", "address:")

        // Street number, street name, street suffix
        private val ADDRESS_PATTERN = listOf(
            Pattern.compile(
                "\\b(online|virtual|zoom|google meet|teams|discord|webinar|livestream)\\b",
                Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                "(?!\\b\\d+\\s*(?:AM|PM)\\b)\\b\\d+\\s+(?:[A-Za-z0-9]+(?:\\s+[A-Za-z0-9]+)*)\\s+(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Lane|Ln|Drive|Dr)\\b",
                Pattern.CASE_INSENSITIVE
            )
        )
    }

    fun parse(blocks: List<Text.TextBlock>): RawEventData {
        val lines = blocks.flatMap { it.lines.toList() }.map { it.text }.filter { it.isNotBlank() }
        val text = blocks.joinToString("\n") { it.text }
        val title = extractTitle(blocks)
        val date = extractDate(text)
        val time = extractTime(blocks)

        val location = extractLocation(text, lines)
        val description = extractDescription(text, title, date, time, location)
        Log.d(
            "EventParser",
            "Parsed event: Title: $title, Date: $date, Time: $time, Location: $location, Description: $description"
        )
        return RawEventData(title, date, time, location, description, text)
    }

    private fun extractTitle(blocks: List<Text.TextBlock>): String? {
        if (blocks.isEmpty()) return null

        // Sort blocks by bounding box height (largest text first)
        val sortedBlocks = blocks.sortedByDescending { block ->
            block.boundingBox?.height() ?: 0
        }

        // Title is likely in first 1-3 lines
        // Prioritize: all caps, longest line, or first substantial lines
        val candidates = sortedBlocks.take(3).map { it.text }.filter { it.length > 3 }
        if (candidates.isEmpty()) return null

        // Check for all caps line
        val allCapsLine =
            candidates.firstOrNull { line -> line.all { it.isUpperCase() || !it.isLetter() } }
        if (allCapsLine != null) return allCapsLine.trim()

        // Return the longest line from first 3
        return candidates.maxByOrNull { it.length }?.trim()
    }

    internal fun extractDate(text: String): String? {
        for (pattern in DATE_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val date = matcher.group().trim()
                return date
            }
        }
        return null
    }

    private fun extractTime(blocks: List<Text.TextBlock>): String? {
        for (block in blocks) {
            val text = block.text
            val time = extractTime(text)
            if (time != null) return time
        }
        return null
    }

    internal fun extractTime(text: String): String? {
        for (pattern in TIME_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group().trim()
            }
        }
        return null
    }

    /**
     * Uses 2 different approaches -  keyword based extraction and Regex‑based address detection
     */
    internal fun extractLocation(text: String, lines: List<String>): String? {
        val lowerText = text.lowercase()

        // Check for location keywords
        for (keyword in LOCATION_KEYWORDS) {
            val index = lowerText.indexOf(keyword)
            if (index != -1) {
                // Extract line containing the keyword
                val lineWithKeyword = lines.firstOrNull {
                    it.lowercase().contains(keyword)
                }
                if (lineWithKeyword != null) {
                    // Get text after keyword
                    val startIndex = lineWithKeyword.lowercase().indexOf(keyword) + keyword.length
                    val location = lineWithKeyword.substring(startIndex).trim()
                    if (location.isNotEmpty()) return location
                }
            }
        }

        // Check for address pattern using regex
        for (pattern in ADDRESS_PATTERN) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group().trim()
            }
        }

        return null
    }

    private fun extractDescription(
        text: String,
        title: String?,
        date: String?,
        time: String?,
        location: String?
    ): String? {
        var description = text

        // Remove extracted elements from description
        title?.let { description = description.replace(it, "", ignoreCase = true) }
        date?.let { description = description.replace(it, "", ignoreCase = true) }
        time?.let { description = description.replace(it, "", ignoreCase = true) }
        location?.let { description = description.replace(it, "", ignoreCase = true) }

        // Remove location keywords
        LOCATION_KEYWORDS.forEach { keyword ->
            description = description.replace(keyword, "", ignoreCase = true)
        }


        // Clean up: remove extra whitespace and empty lines
        description = description.lines()
            .filter { it.isNotBlank() }
            .joinToString("\n") { it.trim() }
            .trim()

        return if (description.isNotEmpty()) description else null
    }
}

