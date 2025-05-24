package com.example.sporthub.ui.profile.edit

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.sporthub.R
import com.example.sporthub.data.repository.UserRepository
import com.example.sporthub.utils.ConnectivityHelper
import com.example.sporthub.utils.ConnectivityHelperExt
import com.example.sporthub.viewmodel.NameSelectionViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class EditNameActivity : AppCompatActivity() {

    private lateinit var viewModel: NameSelectionViewModel
    private lateinit var nameEditText: TextInputEditText
    private lateinit var saveButton: Button
    private lateinit var titleText: TextView
    private lateinit var backButton: ImageButton
    private lateinit var networkMessageText: TextView
    private lateinit var rootView: View
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val isThemeChanging = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_theme_changing", false)

        if (isThemeChanging) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_edit_name)
            initViews()
            return
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_name)

        // Get edit mode from intent
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)

        // Initialize the ViewModel
        viewModel = ViewModelProvider(this).get(NameSelectionViewModel::class.java)

        // Verify authentication
        if (!viewModel.checkAuthentication()) {
            finish()
            return
        }

        // Initialize views
        initViews()

        // Check connectivity initially
        checkConnectivity()

        // Set up observers
        setupObservers()

        // Configure the UI for edit mode
        setupEditMode()

        // Setup back button
        setupBackButton()
    }

    override fun onResume() {
        super.onResume()
        saveButton.isEnabled = true // Enable the Save button again
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

    private fun initViews() {
        rootView = findViewById(R.id.rootViewNameSelection)
        networkMessageText = findViewById(R.id.networkMessageText)
        nameEditText = findViewById(R.id.edit_text_name)
        saveButton = findViewById(R.id.button_continue)
        titleText = findViewById(R.id.textview_title)
        backButton = findViewById(R.id.button_back_name)

        // Set up letter filter for the name input
        setupLettersOnlyFilter()
    }

    private fun setupObservers() {
        viewModel.saveSuccessEvent.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Name updated successfully!", Toast.LENGTH_SHORT).show()
                finish() // Return to profile
            }
        }

        viewModel.errorEvent.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            saveButton.isEnabled = true
        }

        viewModel.userNotAuthenticatedEvent.observe(this) { notAuthenticated ->
            if (notAuthenticated) {
                Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupEditMode() {
        if (isEditMode) {
            // Change text for edit mode
            saveButton.text = "Save"
            titleText.text = "Edit Your Name"

            // Try to get the current user name to display in the edit text
            try {
                val repository = UserRepository()
                val currentUser = repository.getCurrentUser()
                if (currentUser != null) {
                    val displayName = currentUser.displayName
                    if (!displayName.isNullOrEmpty()) {
                        nameEditText.setText(displayName)
                    }
                }
            } catch (e: Exception) {
                // Just continue if we can't get the current name
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

    private fun setupLettersOnlyFilter() {
        // Filter to allow only letters, spaces and some special characters for compound names
        val lettersFilter = InputFilter { source, start, end, dest, dstart, dend ->
            val regex = Regex("^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ '-]+$")
            for (i in start until end) {
                if (!regex.matches(source[i].toString())) {
                    return@InputFilter ""
                }
            }
            null // Allow the input
        }

        // Length filter - limit to 35 characters
        val lengthFilter = InputFilter.LengthFilter(35)

        // Apply both filters
        nameEditText.filters = arrayOf(lettersFilter, lengthFilter)

        // Optional: Show character counter
        val nameInputLayout = findViewById<TextInputLayout>(R.id.name_input_layout)
        nameEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val length = s?.length ?: 0
                if (length >= 25) { // Warning when approaching the limit
                    val remaining = 35 - length
                    nameInputLayout.helperText = "$remaining characters left"
                } else {
                    nameInputLayout.helperText = null
                }
            }
        })

        // Set up the save button
        saveButton.setOnClickListener {
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

            saveButton.isEnabled = false
            viewModel.saveName(name)
        }
    }
}