package com.example.capture

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class EventFormActivity : AppCompatActivity() {
    private val calendarFormatter = CalendarFormatter()

    private lateinit var imagePreview: ImageView
    private lateinit var eventTitle: TextInputEditText
    private lateinit var eventDescription: TextInputEditText
    private lateinit var eventLocation: TextInputEditText
    private lateinit var eventDate: TextInputEditText
    private lateinit var eventStartTime: TextInputEditText
    private lateinit var eventEndTime: TextInputEditText
    private lateinit var allDaySwitch: SwitchMaterial
    private lateinit var eventReminder: AutoCompleteTextView
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    // Color views
    private lateinit var colorRed: View
    private lateinit var colorBlue: View
    private lateinit var colorGreen: View
    private lateinit var colorOrange: View
    private lateinit var colorPurple: View

    private var photoUri: Uri? = null
    private var selectedDate: Calendar = Calendar.getInstance()
    private var startTime: Calendar = Calendar.getInstance()
    private var endTime: Calendar = Calendar.getInstance()
    private var selectedColor: Int = 0 // Default color

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.event_form_temp)

        // Initialize views
        initViews()

        // Get the photo URI from intent
        @Suppress("DEPRECATION")
        photoUri = intent.getParcelableExtra("photo_uri")
        photoUri?.let {
            imagePreview.setImageURI(it)
        }

        // Get extracted event data from intent
        val extractedTitle = intent.getStringExtra("event_title")
        val extractedDate = intent.getStringExtra("event_date")
        val extractedTime = intent.getStringExtra("event_time")
        val extractedLocation = intent.getStringExtra("event_location")
        val extractedDescription = intent.getStringExtra("event_description")

        val dateTimeNormalizer = DateTimeNormalizer()
        // Normalize date/time to structured format
        val structuredDateTime = dateTimeNormalizer.normalize(extractedDate, extractedTime)

        // Pre-populate form fields with normalized data
        extractedTitle?.let { eventTitle.setText(it) }
        extractedLocation?.let { eventLocation.setText(it) }
        extractedDescription?.let { eventDescription.setText(it) }

        // Populate date and time from structured data
        if (structuredDateTime != null) {
            // Set date
            selectedDate.set(
                structuredDateTime.year ?: Calendar.getInstance().get(Calendar.YEAR),
                (structuredDateTime.month ?: 1) - 1,  // Calendar.MONTH is 0-based
                structuredDateTime.day ?: 1
            )
            updateDateDisplay()

            // Set times if available
            if (!structuredDateTime.allDay && structuredDateTime.hour != null) {
                // Set start time
                startTime.set(Calendar.HOUR_OF_DAY, structuredDateTime.hour)
                startTime.set(Calendar.MINUTE, structuredDateTime.minute ?: 0)
                startTime.set(Calendar.YEAR, structuredDateTime.year ?: Calendar.getInstance().get(Calendar.YEAR))
                startTime.set(Calendar.MONTH, (structuredDateTime.month ?: 1) - 1)
                startTime.set(Calendar.DAY_OF_MONTH, structuredDateTime.day ?: 1)
                updateTimeDisplay(true)

                // Set end time (if available, otherwise default to 1 hour after start)
                if (structuredDateTime.endDateTime != null) {
                    endTime.timeInMillis = structuredDateTime.endDateTime
                } else {
                    endTime.timeInMillis = startTime.timeInMillis + 3600000 // +1 hour
                }
                updateTimeDisplay(false)
            } else {
                // All-day event or no time specified - set default times
                allDaySwitch.isChecked = structuredDateTime.allDay
                initializeDefaultTimes()
            }
        } else {
            // No valid date found - initialize defaults
            initializeDefaultTimes()
        }

        // Setup listeners
        setupListeners()

        // Setup reminder dropdown
        setupReminderDropdown()
    }

    private fun initViews() {
        imagePreview = findViewById(R.id.event_image_preview)
        eventTitle = findViewById(R.id.event_title)
        eventDescription = findViewById(R.id.event_description)
        eventLocation = findViewById(R.id.event_location)
        eventDate = findViewById(R.id.event_date)
        eventStartTime = findViewById(R.id.event_start_time)
        eventEndTime = findViewById(R.id.event_end_time)
        allDaySwitch = findViewById(R.id.all_day_switch)
        eventReminder = findViewById(R.id.event_reminder)
        saveButton = findViewById(R.id.save_button)
        cancelButton = findViewById(R.id.cancel_button)

        // Color views
        colorRed = findViewById(R.id.color_red)
        colorBlue = findViewById(R.id.color_blue)
        colorGreen = findViewById(R.id.color_green)
        colorOrange = findViewById(R.id.color_orange)
        colorPurple = findViewById(R.id.color_purple)
    }

    private fun setupListeners() {
        // Date picker
        eventDate.setOnClickListener {
            showDatePicker()
        }

        // Time pickers
        eventStartTime.setOnClickListener {
            showTimePicker(true)
        }

        eventEndTime.setOnClickListener {
            showTimePicker(false)
        }

        // All-day switch
        allDaySwitch.setOnCheckedChangeListener { _, isChecked ->
            eventStartTime.isEnabled = !isChecked
            eventEndTime.isEnabled = !isChecked
        }

        // Color selection
        colorRed.setOnClickListener { selectColor(0, colorRed) }
        colorBlue.setOnClickListener { selectColor(1, colorBlue) }
        colorGreen.setOnClickListener { selectColor(2, colorGreen) }
        colorOrange.setOnClickListener { selectColor(3, colorOrange) }
        colorPurple.setOnClickListener { selectColor(4, colorPurple) }

        // Save button
        saveButton.setOnClickListener {
            saveEventToCalendar()
        }

        // Cancel button
        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun initializeDefaultTimes() {
        // Initialize default times (1 hour from now)
        startTime.add(Calendar.HOUR_OF_DAY, 1)
        startTime.set(Calendar.MINUTE, 0)
        endTime.add(Calendar.HOUR_OF_DAY, 2)
        endTime.set(Calendar.MINUTE, 0)
    }

    private fun setupReminderDropdown() {
        val reminderOptions = arrayOf(
            "None",
            "At time of event",
            "5 minutes before",
            "10 minutes before",
            "15 minutes before",
            "30 minutes before",
            "1 hour before",
            "1 day before"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, reminderOptions)
        eventReminder.setAdapter(adapter)
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                updateDateDisplay()
            },
            // Set initial date
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val timeCalendar = if (isStartTime) startTime else endTime
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                timeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                timeCalendar.set(Calendar.MINUTE, minute)
                updateTimeDisplay(isStartTime)
            },
            // Set initial time
            timeCalendar.get(Calendar.HOUR_OF_DAY),
            timeCalendar.get(Calendar.MINUTE),
            false // 12-hour format
        )
        timePickerDialog.show()
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
        eventDate.setText(dateFormat.format(selectedDate.time))
    }

    private fun updateTimeDisplay(isStartTime: Boolean) {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        if (isStartTime) {
            eventStartTime.setText(timeFormat.format(startTime.time))
        } else {
            eventEndTime.setText(timeFormat.format(endTime.time))
        }
    }

    private fun selectColor(color: Int, selectedView: View) {
        selectedColor = color
        // Reset all color borders
        listOf(colorRed, colorBlue, colorGreen, colorOrange, colorPurple).forEach {
            it.alpha = 0.5f
        }
        // Highlight selected color
        selectedView.alpha = 1.0f
    }

    private fun saveEventToCalendar() {
        // Validate required fields
        if (eventTitle.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter an event title", Toast.LENGTH_SHORT).show()
            return
        }

        if (eventDate.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Build structured date/time
            val structuredDateTime = if (allDaySwitch.isChecked) {
                // All-day event
                StructuredDateTime(
                    startDateTime = selectedDate.timeInMillis,
                    endDateTime = selectedDate.timeInMillis + 24 * 60 * 60 * 1000,
                    allDay = true,
                    year = selectedDate.get(Calendar.YEAR),
                    month = selectedDate.get(Calendar.MONTH) + 1,
                    day = selectedDate.get(Calendar.DAY_OF_MONTH),
                    hour = null,
                    minute = null
                )
            } else {
                // Event with specific time
                val startDateTime = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
                    set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, startTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, startTime.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val endDateTime = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
                    set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, endTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, endTime.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                StructuredDateTime(
                    startDateTime = startDateTime.timeInMillis,
                    endDateTime = endDateTime.timeInMillis,
                    allDay = false,
                    year = selectedDate.get(Calendar.YEAR),
                    month = selectedDate.get(Calendar.MONTH) + 1,
                    day = selectedDate.get(Calendar.DAY_OF_MONTH),
                    hour = startTime.get(Calendar.HOUR_OF_DAY),
                    minute = startTime.get(Calendar.MINUTE)
                )
            }

            // Parse reminder
            val reminderMinutes = calendarFormatter.parseReminderText(eventReminder.text.toString())

            // Create calendar intent using formatter
            val intent = calendarFormatter.createCalendarIntent(
                title = eventTitle.text.toString(),
                description = eventDescription.text.toString(),
                location = eventLocation.text.toString(),
                structuredDateTime = structuredDateTime,
                reminderMinutes = reminderMinutes
            )

            startActivity(intent)
            Toast.makeText(this, "Opening calendar app...", Toast.LENGTH_SHORT).show()
            finish()

        } catch (e: Exception) {
            Log.e("EventFormActivity", "Error saving event", e)
            Toast.makeText(this, "Error saving event: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

