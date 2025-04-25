package com.example.sporthub.ui.profile.edit

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import com.example.sporthub.R
import com.example.sporthub.data.repository.UserRepository
import com.example.sporthub.viewmodel.GenderSelectionViewModel

class EditGenderActivity : AppCompatActivity() {

    private lateinit var viewModel: GenderSelectionViewModel
    private val TAG = "EditGenderActivity"
    private var isEditMode = false
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_gender_selection)

        // Get edit mode from intent
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)

        // Initialize the ViewModel
        viewModel = ViewModelProvider(this).get(GenderSelectionViewModel::class.java)

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

        try {
            // Set up listeners for the buttons
            val buttonMale = findViewById<CardView>(R.id.button_male)
            val buttonFemale = findViewById<CardView>(R.id.button_female)
            val buttonOther = findViewById<CardView>(R.id.button_other)

            buttonMale.setOnClickListener {
                viewModel.saveGender("Male")
            }

            buttonFemale.setOnClickListener {
                viewModel.saveGender("Female")
            }

            buttonOther.setOnClickListener {
                viewModel.saveGender("Other")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up gender selection: ${e.message}")
        }
    }

    private fun initViews() {
        titleText = findViewById(R.id.textview_title)
        subtitleText = findViewById(R.id.textview_subtitle)
        backButton = findViewById(R.id.button_back_gender)
    }

    private fun setupObservers() {
        viewModel.saveSuccessEvent.observe(this) { success ->
            if (success) {
                if (isEditMode) {
                    Toast.makeText(this, "Gender updated successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Return to profile in edit mode
                } else {
                    // Continue with registration flow for new users
                    Toast.makeText(this, "Gender selected successfully!", Toast.LENGTH_SHORT).show()
                    navigateToBirthDateSelection()
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
            titleText.text = "Edit Your Gender"
            subtitleText.text = "Update your gender preference"

            // Try to load current gender for user
            try {
                val repository = UserRepository()
                val userId = repository.getCurrentUser()?.uid
                if (userId != null) {
                    repository.getUserData(userId).addOnSuccessListener { document ->
                        if (document.exists() && document.contains("gender")) {
                            val currentGender = document.getString("gender") ?: ""
                            // We could highlight the current selection if needed
                            Log.d(TAG, "Current gender: $currentGender")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting current gender: ${e.message}")
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

    private fun navigateToBirthDateSelection() {
        // Only navigate to birth date selection in registration flow (non-edit mode)
        if (!isEditMode) {
            val intent = android.content.Intent(this, com.example.sporthub.ui.login.BirthDateSelectionActivity::class.java)
            startActivity(intent)
        }
        finish()
    }
}