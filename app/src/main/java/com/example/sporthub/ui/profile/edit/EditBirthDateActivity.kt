package com.example.sporthub.ui.profile.edit

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.sporthub.R
import com.example.sporthub.data.repository.UserRepository
import com.example.sporthub.viewmodel.BirthDateViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditBirthDateActivity : AppCompatActivity() {

    private val TAG = "EditBirthDateActivity"
    private lateinit var viewModel: BirthDateViewModel
    private lateinit var datePickerEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var backButton: ImageButton
    private val calendar = Calendar.getInstance()
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_birth_date_selection)

        // Get edit mode from intent
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)

        // Initialize the ViewModel
        viewModel = ViewModelProvider(this).get(BirthDateViewModel::class.java)

        // Check authentication
        if (!viewModel.checkAuthentication()) {
            finish()
            return
        }

        // Initialize views
        initViews()

        // Set up observers
        setupObservers()

        // Configure the UI for edit mode
        setupEditMode()

        // Set up back button
        setupBackButton()

        // Set up date picker
        setupDatePicker()

        // Set up save button
        saveButton.setOnClickListener {
            val birthDate = datePickerEditText.text.toString()
            if (birthDate.isNotEmpty() && birthDate != "01 / 01 / 2025") {
                saveBirthDate(birthDate)
            } else {
                Toast.makeText(this, "Please select your birth date", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initViews() {
        datePickerEditText = findViewById(R.id.date_picker_edit_text)
        saveButton = findViewById(R.id.button_continue)
        titleText = findViewById(R.id.textview_title)
        subtitleText = findViewById(R.id.textview_subtitle)
        backButton = findViewById(R.id.button_back_birth)
    }

    private fun setupObservers() {
        viewModel.saveSuccessEvent.observe(this) { success ->
            if (success) {
                if (isEditMode) {
                    Toast.makeText(this, "Birth date updated successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Return to profile in edit mode
                } else {
                    // Continue with registration flow for new users
                    Toast.makeText(this, "Birth date saved successfully!", Toast.LENGTH_SHORT).show()
                    navigateToSportsSelection()
                }
            }
        }

        viewModel.errorEvent.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupEditMode() {
        if (isEditMode) {
            // Change text for edit mode
            titleText.text = "Edit Your Birth Date"
            subtitleText.text = "Update your birth date"
            saveButton.text = "Save"

            // Try to load current birth date for user
            try {
                val repository = UserRepository()
                val userId = repository.getCurrentUser()?.uid
                if (userId != null) {
                    repository.getUserData(userId).addOnSuccessListener { document ->
                        if (document.exists()) {
                            // First check if birth_date exists in the document
                            if (document.contains("birth_date")) {
                                try {
                                    // Try to get it as a Timestamp first
                                    val birthTimestamp = document.getTimestamp("birth_date")
                                    if (birthTimestamp != null) {
                                        val dateFormat = SimpleDateFormat("dd / MM / yyyy", Locale.getDefault())
                                        calendar.time = birthTimestamp.toDate()
                                        datePickerEditText.setText(dateFormat.format(calendar.time))
                                    } else {
                                        // If it's not a Timestamp, check if it's a string
                                        val birthDateString = document.getString("birth_date")
                                        if (birthDateString != null && birthDateString.isNotEmpty()) {
                                            try {
                                                // Try to parse string as date
                                                val dateFormat = SimpleDateFormat("dd / MM / yyyy", Locale.getDefault())
                                                val date = dateFormat.parse(birthDateString)
                                                if (date != null) {
                                                    calendar.time = date
                                                    datePickerEditText.setText(birthDateString)
                                                }
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error parsing birth date string: ${e.message}")
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error retrieving birth date: ${e.message}")
                                    // Don't need to show an error to the user - they can just select a new date
                                }
                            }
                        }
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Error getting user data: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "General error in setupEditMode: ${e.message}")
            }
        }
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun setupDatePicker() {
        datePickerEditText.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            R.style.DatePickerTheme,
            { _, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set max date (Age requirement - Users must be at least 14 years old)
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.YEAR, -14)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

        // Set min date (e.g., 100 years ago)
        val minDate = Calendar.getInstance()
        minDate.add(Calendar.YEAR, -100)
        datePickerDialog.datePicker.minDate = minDate.timeInMillis

        datePickerDialog.show()
    }

    private fun updateDateInView() {
        val dateFormat = SimpleDateFormat("dd / MM / yyyy", Locale.getDefault())
        datePickerEditText.setText(dateFormat.format(calendar.time))
    }

    private fun saveBirthDate(birthDateString: String) {
        try {
            val dateFormat = SimpleDateFormat("dd / MM / yyyy", Locale.getDefault())
            val birthDate = dateFormat.parse(birthDateString)

            if (birthDate != null) {
                viewModel.saveBirthDate(birthDate)
            } else {
                Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unexpected error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToSportsSelection() {
        // Only navigate to sports selection in registration flow (non-edit mode)
        if (!isEditMode) {
            val intent = Intent(this, com.example.sporthub.ui.login.FavoriteSportsSelectionActivity::class.java)
            startActivity(intent)
        }
        finish()
    }
}