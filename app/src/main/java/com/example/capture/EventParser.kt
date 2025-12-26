package com.example.capture

import com.google.mlkit.vision.text.Text
import java.util.regex.Pattern

data class EventData(
    val title: String?,
    val date: String?,
    val time: String?,
    val location: String?,
    val description: String?,
    val rawText: String?
)

class EventParser {

    // Static methods and variables
    companion object {
        // Date patterns
        private val DATE_PATTERNS = listOf(
            Pattern.compile("\\b\\d{1,2}(st|nd|rd|th) (?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]* \\d{1,2}(?:,?\\\\s*\\\\d{4})?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b"),
            Pattern.compile("\\b\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}\\b"),
            Pattern.compile("\\b(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday),? (?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]* \\d{1,2}\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b\\d{1,2}(st|nd|rd|th)\\b", Pattern.CASE_INSENSITIVE), // ordinal only (1st, 2nd, 3rd, 10th)
            Pattern.compile( "\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]* \\d{1,2}(st|nd|rd|th)\\b", Pattern.CASE_INSENSITIVE ), // month + ordinal (Jan 21st)
            Pattern.compile( "\\b\\d{1,2}(st|nd|rd|th) (?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\b", Pattern.CASE_INSENSITIVE ), // ordinal + month (21st JanPattern.compile(
            )
        // Time patterns
        private val TIME_PATTERNS = listOf(
            Pattern.compile("\\b\\d{1,2}:\\d{2}\\s*(?:AM|PM)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b\\d{1,2}\\s*(?:AM|PM)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b\\d{1,2}:\\d{2}\\b"),
            Pattern.compile("\\b\\d{1,2}:\\d{2}\\s*(?:AM|PM)?\\s*-\\s*\\d{1,2}:\\d{2}\\s*(?:AM|PM)?\\b", Pattern.CASE_INSENSITIVE)
        )

        // Location keywords and patterns
        private val LOCATION_KEYWORDS = listOf("at ", "venue:", "location:", "@", "address:")
        private val ADDRESS_PATTERN = Pattern.compile("\\b\\d+\\s+[A-Za-z0-9\\s,]+(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Lane|Ln|Drive|Dr)\\b", Pattern.CASE_INSENSITIVE)
    }

    fun parse(blocks: List<Text.TextBlock>): EventData {
        val lines = blocks.flatMap { it.lines.toList() }.map { it.text }.filter { it.isNotBlank() }
        val text = blocks.joinToString("\n") { it.text }
        val title = extractTitle(blocks)
        val date = extractDate(text)
        val time = extractTime(blocks)

        val location = extractLocation(text, lines)
        val description = extractDescription(text, title, date, time, location)

        return EventData(title, date, time, location, description, text)
    }

    private fun extractTitle(blocks: List<Text.TextBlock>): String? {
        if(blocks.isEmpty()) return null

        // Sort blocks by bounding box height (largest text first)
        val sortedBlocks = blocks.sortedByDescending { block ->
            block.boundingBox?.height() ?: 0
        }

        // Title is likely in first 1-3 lines
        // Prioritize: all caps, longest line, or first substantial lines
        val candidates = sortedBlocks.take(3).map { it.text }.filter { it.length > 3}
        if(candidates.isEmpty()) return null

        // Check for all caps line
        val allCapsLine = candidates.firstOrNull { line -> line.all { it.isUpperCase() || !it.isLetter()} }
        if(allCapsLine != null) return allCapsLine.trim()

        // Return the longest line from first 3
        return candidates.maxByOrNull { it.length }?.trim()
    }

    private fun extractDate(text: String): String? {
//        val candidates = mutableListOf<Pair<String, Int>>() // Text + score

//        for (block in blocks) {
//            val text = block.text
            for(pattern in DATE_PATTERNS) {
                val matcher = pattern.matcher(text) // Create matcher object
                if(matcher.find()) { // .find() return boolean
                    val date = matcher.group().trim() // Returns the exact text that matches the pattern
                    return date
                    // Score based on bounding box height (font size)
//                    val score = block.boundingBox?.height() ?: 0
//
//                    candidates.add(date to score)
                }
            }
//        }
        return null
//        // Return the date with highest score
//        return candidates.maxByOrNull { it.second }?.first

    }

    private fun extractTime(blocks: List<Text.TextBlock>): String? {
        for (block in blocks) {
            val text = block.text
            for (pattern in TIME_PATTERNS) {
                val matcher = pattern.matcher(text) // Create matcher object
                if (matcher.find()) {
                    return matcher.group().trim()
                }
            }

        }
        return null
    }

    /**
     * Uses 2 different approaches -  keyword based extraction and Regex‑based address detection
     */
    private fun extractLocation(text: String, lines: List<String>): String? {
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
        // Check for address pattern (number + street name)
        val matcher = ADDRESS_PATTERN.matcher(text)
        if (matcher.find()) {
            return matcher.group().trim()
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