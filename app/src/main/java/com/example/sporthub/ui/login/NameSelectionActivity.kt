package com.example.sporthub.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.sporthub.R
import com.example.sporthub.utils.ConnectivityHelper
import com.example.sporthub.utils.ConnectivityHelperExt
import com.example.sporthub.viewmodel.NameSelectionViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.example.sporthub.utils.RegistrationTimerManager

class NameSelectionActivity : AppCompatActivity() {

    private lateinit var viewModel: NameSelectionViewModel
    private lateinit var nameEditText: TextInputEditText
    private lateinit var continueButton: Button
    private lateinit var networkMessageText: TextView
    private lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_sign_in_name_selection)
        RegistrationTimerManager.startTimer()
        Log.d("NameSelectionActivity", "Starting registration timer")

        // Initialize the ViewModel
        viewModel = ViewModelProvider(this).get(NameSelectionViewModel::class.java)

        // Verify authentication
        if (!viewModel.checkAuthentication()) {
            redirectToSignIn()
            return
        }

        // Initialize views
        rootView = findViewById(R.id.rootViewNameSelection)
        nameEditText = findViewById(R.id.edit_text_name)
        continueButton = findViewById(R.id.button_continue)
        networkMessageText = findViewById(R.id.networkMessageText)

        // Check connectivity initially
        checkConnectivity()

        // Configure filter to only allow letters
        setupLettersOnlyFilter()

        // Configure observers for events from the ViewModel
        setupObservers()

        // Handle back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@NameSelectionActivity,
                    "Please enter your name to continue",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // Configure the continue button
        continueButton.setOnClickListener {
            // Check connectivity before proceeding
            if (!ConnectivityHelper.isNetworkAvailable(this)) {
                ConnectivityHelperExt.checkNetworkAndNotify(this, rootView)
                return@setOnClickListener
            }

            val name = nameEditText.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            continueButton.isEnabled = false
            viewModel.saveName(name)
        }
    }

    override fun onResume() {
        super.onResume()
        continueButton.isEnabled = true // Enable the Continue button again
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

    private fun setupLettersOnlyFilter() {
        // Filter to allow only letters, spaces and some special characters for compound names
        val lettersFilter = InputFilter { source, start, end, dest, dstart, dend ->
            val regex = Regex("^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ '-]+$")
            for (i in start until end) {
                if (!regex.matches(source[i].toString())) {
                    // If it doesn't match the pattern, don't allow the input
                    return@InputFilter ""
                }
            }
            null // Allow the input
        }

        // Length filter - limit to 30 characters
        val lengthFilter = InputFilter.LengthFilter(35)

        // Apply both filters
        nameEditText.filters = arrayOf(lettersFilter, lengthFilter)

        // Optional: Show character counter
        // First get the reference to the TextInputLayout
        val nameInputLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.name_input_layout)

        nameEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                val length = s?.length ?: 0
                if (length >= 25) { // Warning when approaching the limit
                    val remaining = 30 - length
                    nameInputLayout.helperText = "$remaining characters left"
                } else {
                    nameInputLayout.helperText = null
                }
            }
        })
    }

    private fun setupObservers() {
        viewModel.saveSuccessEvent.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Name saved successfully!", Toast.LENGTH_SHORT).show()
                navigateToGenderSelection()
            }
        }

        viewModel.errorEvent.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            continueButton.isEnabled = true
        }

        viewModel.userNotAuthenticatedEvent.observe(this) { notAuthenticated ->
            if (notAuthenticated) {
                Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
                redirectToSignIn()
            }
        }
    }

    private fun navigateToGenderSelection() {
        val intent = Intent(this, GenderSelectionActivity::class.java)
        startActivity(intent)
    }

    private fun redirectToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}