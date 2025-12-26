package com.example.capture

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Unit tests for EventParser class focusing on regex pattern matching
 * for dates, times, and locations.
 * 
 * These tests call the actual parsing methods from EventParser.
 */
class EventParserTest {

    private lateinit var eventParser: EventParser

    @Before
    fun setUp() {
        eventParser = EventParser()
    }

    // ==================== DATE REGEX TESTS ====================

    @Test
    fun testDatePattern_OrdinalWithMonthName() {
        // Pattern: "1st January", "21st Dec", "3rd March"
        val text = "Event on 21st January"
        val result = eventParser.extractDate(text)
        
        assertEquals("21st January", result)
    }

    @Test
    fun testDatePattern_OrdinalWithFullMonthName() {
        val text = "Conference on 15th December"
        val result = eventParser.extractDate(text)
        
        assertEquals("15th December", result)
    }

    @Test
    fun testDatePattern_MonthAndDay() {
        // Pattern: "Jan 21", "December 31"
        val text = "Meeting on Jan 21"
        val result = eventParser.extractDate(text)
        
        assertEquals("Jan 21", result)
    }

    @Test
    fun testDatePattern_MonthDayYear() {
        // Pattern: "Jan 21, 2024"
        val text = "Event on December 31, 2024"
        val result = eventParser.extractDate(text)
        
        assertEquals("December 31, 2024", result)
    }

    @Test
    fun testDatePattern_NumericSlashFormat() {
        // Pattern: "12/31/2024", "1/1/24"
        val text = "Date: 12/31/2024"
        val result = eventParser.extractDate(text)
        
        assertEquals("12/31/2024", result)
    }

    @Test
    fun testDatePattern_NumericHyphenFormat() {
        // Pattern: "12-31-2024"
        val text = "Event: 01-15-2024"
        val result = eventParser.extractDate(text)
        
        assertEquals("01-15-2024", result)
    }

    @Test
    fun testDatePattern_ISOFormat() {
        // Pattern: "2024-12-31"
        val text = "Date: 2024-12-31"
        val result = eventParser.extractDate(text)
        
        assertEquals("2024-12-31", result)
    }

    @Test
    fun testDatePattern_WithDayOfWeek() {
        // Pattern: "Monday, Jan 15"
        val text = "Event on Friday, December 25"
        val result = eventParser.extractDate(text)
        
        assertNotNull(result)
        assertTrue(result!!.contains("December 25") || result.contains("Friday"))
    }

    @Test
    fun testDatePattern_OrdinalOnly() {
        // Pattern: "21st", "1st", "3rd"
        val text = "Event on the 21st"
        val result = eventParser.extractDate(text)
        
        assertEquals("21st", result)
    }

    @Test
    fun testDatePattern_MonthWithOrdinal() {
        // Pattern: "Jan 21st"
        val text = "Meeting Jan 21st"
        val result = eventParser.extractDate(text)
        
        assertEquals("Jan 21", result)
    }

    @Test
    fun testDatePattern_EdgeCase_SingleDigitDay() {
        val text = "Event on 1st Jan"
        val result = eventParser.extractDate(text)
        
        assertEquals("1st Jan", result)
    }

    @Test
    fun testDatePattern_EdgeCase_DoubleDigitDay() {
        val text = "Event on 31st December"
        val result = eventParser.extractDate(text)
        
        assertEquals("31st December", result)
    }

    @Test
    fun testDatePattern_CaseInsensitive() {
        val text = "Event on 15TH DECEMBER"
        val result = eventParser.extractDate(text)
        
        assertNotNull(result)
    }

    @Test
    fun testDatePattern_NoDatePresent() {
        val text = "Annual Conference"
        val result = eventParser.extractDate(text)
        
        assertNull(result)
    }

    @Test
    fun testDatePattern_MultipleFormats() {
        // Should return the first match
        val text = "Event on 21st Jan or 12/25/2024"
        val result = eventParser.extractDate(text)
        
        assertNotNull(result)
        // Should match first pattern found
        assertTrue(result == "21st Jan" || result == "12/25/2024")
    }

    // ==================== TIME REGEX TESTS ====================

    @Test
    fun testTimePattern_SimpleAM() {
        val text = "Meeting at 9:00 AM"
        val result = eventParser.extractTime(text)
        
        assertEquals("9:00 AM", result)
    }

    @Test
    fun testTimePattern_SimplePM() {
        val text = "Event at 3:30 PM"
        val result = eventParser.extractTime(text)
        
        assertEquals("3:30 PM", result)
    }

    @Test
    fun testTimePattern_TimeRange_WithAMPM() {
        // Pattern: "9:00 AM - 11:30 AM"
        val text = "Conference 9:00 AM - 11:30 AM"
        val result = eventParser.extractTime(text)
        
        assertEquals("9:00 AM - 11:30 AM", result)
    }

    @Test
    fun testTimePattern_TimeRange_WithTo() {
        // Pattern: "9:00 to 10:00 PM"
        val text = "Event 9:00 to 10:00 PM"
        val result = eventParser.extractTime(text)
        
        assertEquals("9:00 to 10:00 PM", result)
    }

    @Test
    fun testTimePattern_TimeRange_MixedFormat() {
        // Pattern: "9 - 11:30 PM"
        val text = "Meeting 9 - 11:30 PM"
        val result = eventParser.extractTime(text)
        
        assertEquals("9 - 11:30 PM", result)
    }

    @Test
    fun testTimePattern_WithoutMinutes() {
        // Pattern: "9AM"
        val text = "Event at 9AM"
        val result = eventParser.extractTime(text)
        
        assertNotNull(result)
    }

    @Test
    fun testTimePattern_24HourFormat_NotSupported() {
        val text = "Meeting at 14:30"
        val result = eventParser.extractTime(text)
        
        // Current regex doesn't support 24-hour format without AM/PM
        assertNull(result)
    }

    @Test
    fun testTimePattern_CaseInsensitive() {
        val text = "Event at 3:30 pm"
        val result = eventParser.extractTime(text)
        
        assertNotNull(result)
    }

    @Test
    fun testTimePattern_NoTimePresent() {
        val text = "Annual Conference"
        val result = eventParser.extractTime(text)
        
        assertNull(result)
    }

    @Test
    fun testTimePattern_EdgeCase_Noon() {
        val text = "Event at 12:00 PM"
        val result = eventParser.extractTime(text)
        
        assertEquals("12:00 PM", result)
    }

    @Test
    fun testTimePattern_EdgeCase_Midnight() {
        val text = "Event at 12:00 AM"
        val result = eventParser.extractTime(text)
        
        assertEquals("12:00 AM", result)
    }

    @Test
    fun testTimePattern_WithSpaces() {
        val text = "Meeting at 9:30  AM"
        val result = eventParser.extractTime(text)
        
        assertNotNull(result)
    }

    // ==================== LOCATION REGEX TESTS ====================

    @Test
    fun testLocationPattern_WithAtKeyword() {
        val text = "Event at Central Park"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertEquals("Central Park", result)
    }

    @Test
    fun testLocationPattern_WithVenueKeyword() {
        val text = "Conference\nVenue: Grand Hotel"
        val lines = text.split("\n")
        val result = eventParser.extractLocation(text, lines)
        
        assertEquals("Grand Hotel", result)
    }

    @Test
    fun testLocationPattern_WithLocationKeyword() {
        val text = "Meeting\nLocation: Room 101"
        val lines = text.split("\n")
        val result = eventParser.extractLocation(text, lines)
        
        assertEquals("Room 101", result)
    }

    @Test
    fun testLocationPattern_WithAtSymbol() {
        val text = "Event @ Tech Hub"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertEquals("Tech Hub", result)
    }

    @Test
    fun testLocationPattern_WithAddressKeyword() {
        val text = "Conference\nAddress: 123 Main St"
        val lines = text.split("\n")
        val result = eventParser.extractLocation(text, lines)
        
        assertNotNull(result)
    }

    @Test
    fun testLocationPattern_StreetAddress() {
        // Pattern: "123 Main Street"
        val text = "Event at 123 Main Street"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertNotNull(result)
        assertTrue(result!!.contains("Main Street") || result.contains("Main St"))
    }

    @Test
    fun testLocationPattern_Avenue() {
        val text = "Conference at 456 Park Avenue"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertNotNull(result)
        assertTrue(result!!.contains("Avenue") || result.contains("Ave"))
    }

    @Test
    fun testLocationPattern_Boulevard() {
        val text = "Meeting at 789 Sunset Boulevard"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertNotNull(result)
    }

    @Test
    fun testLocationPattern_Road() {
        val text = "Event at 321 Oak Road"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertNotNull(result)
    }

    @Test
    fun testLocationPattern_OnlineVenue() {
        val text = "Event will be online"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertEquals("online", result)
    }

    @Test
    fun testLocationPattern_VirtualVenue() {
        val text = "This is a virtual event"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertEquals("virtual", result)
    }

    @Test
    fun testLocationPattern_Zoom() {
        val text = "Join us on Zoom"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertEquals("Zoom", result)
    }

    @Test
    fun testLocationPattern_GoogleMeet() {
        val text = "Meeting via Google Meet"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertEquals("Google Meet", result)
    }

    @Test
    fun testLocationPattern_Teams() {
        val text = "Conference on Teams"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertEquals("Teams", result)
    }

    @Test
    fun testLocationPattern_CaseInsensitive() {
        val text = "Event at 123 MAIN STREET"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertNotNull(result)
    }

    @Test
    fun testLocationPattern_NoLocationPresent() {
        val text = "Annual Conference"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertNull(result)
    }

    @Test
    fun testLocationPattern_TimeNotConfusedWithAddress() {
        // Ensure times like "9 AM" don't match street address pattern
        val text = "Event at 9 AM downtown"
        val result = eventParser.extractLocation(text, listOf(text))
        println(result)
        // Should not match "9 AM" as a street address
        if (result != null) {
            assertFalse(result.contains("9 AM"))
        }
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    fun testMultipleDatesInText_ReturnsFirst() {
        val text = "Event on 21st Jan and 15th Feb"
        val result = eventParser.extractDate(text)
        
        assertEquals("21st Jan", result)
    }

    @Test
    fun testMultipleTimesInText_ReturnsFirst() {
        val text = "Meeting at 9:00 AM or 2:00 PM"
        val result = eventParser.extractTime(text)
        
        assertEquals("9:00 AM", result)
    }

    @Test
    fun testShortMonthAbbreviations() {
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        months.forEach { month ->
            val text = "Event on $month 15"
            val result = eventParser.extractDate(text)
            assertNotNull("Failed to match $month", result)
        }
    }

    @Test
    fun testOrdinalSuffixes() {
        val ordinals = listOf("1st", "2nd", "3rd", "4th", "21st", "22nd", "23rd", "31st")
        ordinals.forEach { ordinal ->
            val text = "Event on the $ordinal"
            val result = eventParser.extractDate(text)
            assertEquals("Failed to match $ordinal", ordinal, result)
        }
    }

    @Test
    fun testTimeWithNoSpaceBeforeAMPM() {
        val text = "Meeting at 9:00AM"
        val result = eventParser.extractTime(text)
        
        assertNotNull(result)
    }

    @Test
    fun testTimeWithMultipleSpaces() {
        val text = "Event at 3:30   PM"
        val result = eventParser.extractTime(text)
        
        assertNotNull(result)
    }

    @Test
    fun testLocationWithMultipleKeywords() {
        val text = "Event at Main Hall, Location: Building A"
        val result = eventParser.extractLocation(text, listOf(text))
        
        // Should match first keyword
        assertNotNull(result)
    }

    @Test
    fun testDateWithSurroundingText() {
        val text = "Please join us on 25th December for our celebration"
        val result = eventParser.extractDate(text)
        
        assertEquals("25th December", result)
    }

    @Test
    fun testTimeRange_DifferentSeparators() {
        val text1 = "Event 9:00 AM - 11:00 AM"
        val text2 = "Event 9:00 AM to 11:00 AM"
        
        assertNotNull(eventParser.extractTime(text1))
        assertNotNull(eventParser.extractTime(text2))
    }

    @Test
    fun testLocationPattern_DriveAndLane() {
        val text1 = "Event at 123 Oak Drive"
        val text2 = "Conference at 456 Park Lane"
        
        assertNotNull(eventParser.extractLocation(text1, listOf(text1)))
        assertNotNull(eventParser.extractLocation(text2, listOf(text2)))
    }

    @Test
    fun testDatePattern_YearWithSlash() {
        val text = "Meeting on 3/15/2024"
        val result = eventParser.extractDate(text)
        
        assertEquals("3/15/2024", result)
    }

    @Test
    fun testDatePattern_TwoDigitYear() {
        val text = "Event on 12/25/24"
        val result = eventParser.extractDate(text)
        
        assertEquals("12/25/24", result)
    }

    @Test
    fun testTimePattern_NoMinutesWithAMPM() {
        val text = "Conference at 9 AM"
        val result = eventParser.extractTime(text)
        
        // Current regex requires colon for AM/PM times
        // This tests actual behavior
        assertNull(result)
    }

    @Test
    fun testLocationPattern_Discord() {
        val text = "Join us on Discord"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertEquals("Discord", result)
    }

    @Test
    fun testLocationPattern_Webinar() {
        val text = "This is a webinar event"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertEquals("webinar", result)
    }

    @Test
    fun testLocationPattern_Livestream() {
        val text = "Watch the livestream"
        val result = eventParser.extractLocation(text, listOf(text))
        
        assertEquals("livestream", result)
    }
}
