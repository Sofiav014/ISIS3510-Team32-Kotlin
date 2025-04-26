package com.example.sporthub.ui.login

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.sporthub.R
import com.example.sporthub.utils.ConnectivityHelper
import com.example.sporthub.utils.ConnectivityHelperExt
import com.example.sporthub.viewmodel.BirthDateViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BirthDateSelectionActivity : AppCompatActivity() {

    private lateinit var viewModel: BirthDateViewModel
    private lateinit var datePickerEditText: EditText
    private lateinit var buttonContinue: Button
    private lateinit var networkMessageText: TextView
    private lateinit var rootView: View
    private val calendar = Calendar.getInstance()
    private var hasNavigated = false // Flag to prevent double navigation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_birth_date_selection)

        // Initialize the ViewModel
        viewModel = ViewModelProvider(this).get(BirthDateViewModel::class.java)

        // Verify authentication
        if (!viewModel.checkAuthentication()) {
            redirectToSignIn()
            return
        }

        // Initialize views
        rootView = findViewById(R.id.rootViewBirthDate)
        networkMessageText = findViewById(R.id.networkMessageText)
        datePickerEditText = findViewById(R.id.date_picker_edit_text)
        buttonContinue = findViewById(R.id.button_continue)

        // Check connectivity initially
        checkConnectivity()

        // Handle the back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@BirthDateSelectionActivity,
                    "Going back to gender selection!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        })

        // Handle new back button
        val backButton = findViewById<ImageButton>(R.id.button_back_birth)
        backButton.setOnClickListener {
            finish()
        }

        // Set up observers for ViewModel events
        setupObservers()

        // Set up the date picker
        datePickerEditText.setOnClickListener {
            showDatePicker()
        }

        // Set up the continue button
        buttonContinue.setOnClickListener {
            // Check connectivity before proceeding
            if (!ConnectivityHelper.isNetworkAvailable(this)) {
                ConnectivityHelperExt.checkNetworkAndNotify(this, rootView)
                return@setOnClickListener
            }

            val birthDate = datePickerEditText.text.toString()
            if (birthDate.isNotEmpty() && birthDate != "01 / 01 / 2025") {
                buttonContinue.isEnabled = false
                saveBirthDate(birthDate)
            } else {
                Toast.makeText(this, "Please select your birth date", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        buttonContinue.isEnabled = true // Enable the Continue button again
        hasNavigated = false // Reset navigation flag
        // Check connectivity when resuming
        checkConnectivity()
    }

    private fun checkConnectivity() {
        if (!ConnectivityHelper.isNetworkAvailable(this)) {
            networkMessageText.visibility = View.VISIBLE
        } else {
            networkMessageText.visibility = View.GONE
        }
    }

    private fun setupObservers() {
        viewModel.saveSuccessEvent.observe(this) { success ->
            if (success && !hasNavigated) {
                hasNavigated = true // Set flag to prevent double navigation
                Toast.makeText(this, "Birth date updated successfully!", Toast.LENGTH_SHORT).show()
                navigateToSportsSelection()
            }
        }

        viewModel.errorEvent.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            buttonContinue.isEnabled = true
        }

        viewModel.userNotAuthenticatedEvent.observe(this) { notAuthenticated ->
            if (notAuthenticated) {
                Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
                redirectToSignIn()
            }
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

        // Set maximum date (Age requirement - Users must be at least 14)
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.YEAR, -14)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

        // Set minimum date (e.g., 100 years back)
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
                buttonContinue.isEnabled = true
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unexpected error", Toast.LENGTH_SHORT).show()
            buttonContinue.isEnabled = true
        }
    }

    private fun navigateToSportsSelection() {
        val intent = Intent(this, FavoriteSportsSelectionActivity::class.java)
        startActivity(intent)
        // Do not finish this activity yet, to allow proper back navigation
    }

    private fun redirectToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}