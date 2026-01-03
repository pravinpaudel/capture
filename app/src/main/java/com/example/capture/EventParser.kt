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
        
        // Extract fields and identify which blocks contain them
        val titleBlock = extractTitleBlock(blocks)
        val title = titleBlock?.text
        val date = extractDate(text)
        val time = extractTime(blocks)
        val timeBlocks = extractTimeBlocks(blocks)
        val location = extractLocation(text, lines)

        // Find blocks to exclude from description
        val blocksToExclude = mutableListOf<Text.TextBlock>()
        titleBlock?.let { blocksToExclude.add(it) }
        
        // Add blocks containing date
        date?.let { dateStr ->
            blocks.forEach { block ->
                if (block.text.contains(dateStr, ignoreCase = true)) {
                    blocksToExclude.add(block)
                }
            }
        }
        
        // Add blocks containing time
        blocksToExclude.addAll(timeBlocks)
        
        // Add blocks containing location
        location?.let { locStr ->
            blocks.forEach { block ->
                if (block.text.contains(locStr, ignoreCase = true)) {
                    blocksToExclude.add(block)
                }
            }
        }

        val description = extractDescription(blocks, blocksToExclude)
        Log.d(
            "EventParser",
            "Parsed event: Title: $title, Date: $date, Time: $time, Location: $location, Description: $description"
        )
        return RawEventData(title, date, time, location, description, text)
    }

    private fun extractTitleBlock(blocks: List<Text.TextBlock>): Text.TextBlock? {
        if (blocks.isEmpty()) return null

        // Get image dimensions to calculate center alignment and vertical position
        val imageWidth = blocks.maxOfOrNull { it.boundingBox?.right ?: 0 } ?: 0
        val imageHeight = blocks.maxOfOrNull { it.boundingBox?.bottom ?: 0 } ?: 0
        val centerX = imageWidth / 2f

        // Sort blocks by bounding box height (largest text first)
        val sortedBlocks = blocks.sortedByDescending { block ->
            block.boundingBox?.height() ?: 0
        }

//        Log.d("EventParser", "Sorted blocks: ${sortedBlocks.map { it.text + ", Height: " + it.boundingBox?.height() }}")

        // Title is likely in first 1-3 lines
        // Prioritize: all caps, centered text, top position, and appropriate length
        val candidateBlocks = sortedBlocks.take(3).filter { it.text.length > 3 }
        if (candidateBlocks.isEmpty()) return null

//        Log.d("EventParser", "Candidate blocks: ${candidateBlocks.map { it.text }}")

        // Get max height for font size comparison
        val maxHeight = sortedBlocks.firstOrNull()?.boundingBox?.height() ?: 1
        
        // Get top-most block position for first block bonus
        val topMostPosition = blocks.minOfOrNull { it.boundingBox?.top ?: Int.MAX_VALUE } ?: 0

        // Score each candidate based on multiple factors
        data class TitleCandidate(val text: String, val score: Float)
        val scoredCandidates = candidateBlocks.map { block ->
            var score = 0f
            val text = block.text
            val bbox = block.boundingBox

            // Factor 1: All caps (high priority)
            if (text.all { it.isUpperCase() || !it.isLetter() }) {
                score += 3f
            }

            // Factor 2: Center alignment
            if (bbox != null && imageWidth > 0) {
                val blockCenterX = (bbox.left + bbox.right) / 2f
                val distanceFromCenter = Math.abs(blockCenterX - centerX)
                val centerRatio = 1f - (distanceFromCenter / centerX).coerceIn(0f, 1f)
                // Give up to 2 points for being centered
                score += centerRatio * 2f
            }

            // Factor 3: Vertical position (titles are usually at the top)
            if (bbox != null && imageHeight > 0) {
                val blockTop = bbox.top.toFloat()
                // Normalize position: 0 = top, 1 = bottom
                val normalizedPosition = (blockTop / imageHeight).coerceIn(0f, 1f)
                // Give up to 1.5 points for being near the top
                // Score decreases as we go down the image
                val positionScore = (1f - normalizedPosition) * 1.5f
                score += positionScore
            }

            // Factor 4: Text length (titles are typically concise, not too long or short)
            // Ideal title length is around 10-40 characters
            val lengthScore = when {
                text.length < 5 -> 0f // Too short
                text.length in 10..40 -> 1f // Ideal title length
                text.length in 5..60 -> 0.5f // Acceptable but not ideal
                else -> 0f // Too long, likely description
            }
            score += lengthScore

            // Factor 5: Font size (larger text is more likely to be title)
            if (bbox != null && maxHeight > 0) {
                val heightRatio = (bbox.height().toFloat() / maxHeight).coerceIn(0f, 1f)
                // Give up to 1 point for being large text
                score += heightRatio * 1f
            }

            // Factor 6: Word count (titles are typically 2-8 words)
            val wordCount = text.split("\\s+".toRegex()).size
            val wordCountScore = when (wordCount) {
                in 2..8 -> 0.5f // Ideal word count
                1 -> 0.2f // Could be a single-word title
                in 9..12 -> 0.3f // Acceptable but long
                else -> 0f // Too many or too few words
            }
            score += wordCountScore

            // Factor 7: Punctuation patterns (avoid full sentences)
            if (text.endsWith(".") || text.count { it == '.' } > 1) {
                score -= 1f // Penalize sentence-like text
            }

            // Factor 8: Avoid date/time text
            val containsDate = DATE_PATTERNS.any { pattern -> 
                pattern.matcher(text).find() 
            }
            val containsTime = TIME_PATTERNS.any { pattern -> 
                pattern.matcher(text).find() 
            }
            if (containsDate || containsTime) {
                score -= 2f // Strong penalty for date/time blocks
            }

            // Factor 9: First substantial block bonus
            if (bbox != null && bbox.top <= topMostPosition + 50) {
                score += 0.5f // Bonus for being at the very top
            }

            // Factor 10: Aspect ratio (titles are horizontal/wide)
            if (bbox != null) {
                val aspectRatio = bbox.width().toFloat() / bbox.height().toFloat()
                if (aspectRatio > 2f) {
                    score += 0.5f // Bonus for wide, horizontal text
                }
            }

            // Factor 11: Title case detection
            val words = text.split("\\s+".toRegex())
            val titleCaseWords = words.count { word ->
                word.isNotEmpty() && word[0].isUpperCase() && 
                word.drop(1).any { it.isLowerCase() }
            }
            if (titleCaseWords >= words.size / 2 && words.size > 1) {
                score += 0.5f // Bonus for Title Case
            }

            // Factor 12: Avoid numeric-heavy text
            val digitCount = text.count { it.isDigit() }
            val letterCount = text.count { it.isLetter() }
            if (letterCount > 0) {
                val digitRatio = digitCount.toFloat() / letterCount
                if (digitRatio > 0.5f) {
                    score -= 1f // Penalize number-heavy text
                }
            }

            TitleCandidate(text, score)
        }

        // Return the block with highest scoring candidate
        val bestCandidate = scoredCandidates.maxByOrNull { it.score }
        return if (bestCandidate != null) {
            candidateBlocks.find { it.text == bestCandidate.text }
        } else null
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

    private fun extractTimeBlocks(blocks: List<Text.TextBlock>): List<Text.TextBlock> {
        val timeBlocks = mutableListOf<Text.TextBlock>()
        for (block in blocks) {
            val text = block.text
            val time = extractTime(text)
            if (time != null) {
                timeBlocks.add(block)
            }
        }
        return timeBlocks
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
        allBlocks: List<Text.TextBlock>,
        excludeBlocks: List<Text.TextBlock>
    ): String? {
        // Filter out blocks we've identified as title, date, time, location
        val remainingBlocks = allBlocks.filter { block ->
            !excludeBlocks.contains(block)
        }

        if (remainingBlocks.isEmpty()) return null

        // Sort blocks in reading order: top-to-bottom, then left-to-right
        val sortedBlocks = remainingBlocks.sortedWith(
            compareBy<Text.TextBlock> { it.boundingBox?.top ?: 0 }
                .thenBy { it.boundingBox?.left ?: 0 }
        )

        // Group blocks that are vertically close together (within 20 pixels)
        val paragraphs = mutableListOf<MutableList<Text.TextBlock>>()
        var currentParagraph = mutableListOf<Text.TextBlock>()
        var lastBottom = 0

        for (block in sortedBlocks) {
            val blockTop = block.boundingBox?.top ?: 0
            val blockBottom = block.boundingBox?.bottom ?: 0

            // If this block is far below the previous one, start a new paragraph
            if (currentParagraph.isNotEmpty() && blockTop > lastBottom + 20) {
                paragraphs.add(currentParagraph)
                currentParagraph = mutableListOf()
            }

            currentParagraph.add(block)
            lastBottom = blockBottom
        }

        // Don't forget the last paragraph
        if (currentParagraph.isNotEmpty()) {
            paragraphs.add(currentParagraph)
        }

        // Concatenate paragraphs with line breaks between them
        val description = paragraphs.joinToString("\n") { paragraph ->
            paragraph.joinToString(" ") { it.text.trim() }
        }.trim()

        return if (description.isNotEmpty()) description else null
    }
}

