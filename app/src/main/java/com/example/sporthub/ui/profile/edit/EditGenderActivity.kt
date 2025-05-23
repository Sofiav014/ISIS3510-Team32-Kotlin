package com.example.sporthub.ui.profile.edit

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import com.example.sporthub.R
import com.example.sporthub.data.repository.UserRepository
import com.example.sporthub.utils.ConnectivityHelper
import com.example.sporthub.utils.ConnectivityHelperExt
import com.example.sporthub.viewmodel.GenderSelectionViewModel

class EditGenderActivity : AppCompatActivity() {

    private lateinit var viewModel: GenderSelectionViewModel
    private val TAG = "EditGenderActivity"
    private var isEditMode = false
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var backButton: ImageButton
    private lateinit var networkMessageText: TextView
    private lateinit var rootView: View
    private var isCardClicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val isThemeChanging = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_theme_changing", false)

        if (isThemeChanging) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_edit_gender)
            initViews()
            return
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_gender)

        // Get edit mode from intent
        isEditMode = intent.getBooleanExtra("EDIT_MODE", true)

        // Initialize the ViewModel
        viewModel = ViewModelProvider(this).get(GenderSelectionViewModel::class.java)

        // Check authentication
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

        // Set up back button
        setupBackButton()

        try {
            // Set up listeners for the buttons
            val buttonMale = findViewById<CardView>(R.id.button_male)
            val buttonFemale = findViewById<CardView>(R.id.button_female)
            val buttonOther = findViewById<CardView>(R.id.button_other)

            buttonMale.setOnClickListener {
                if (checkNetworkBeforeAction()) {
                    handleGenderSelected("Male")
                }
            }

            buttonFemale.setOnClickListener {
                if (checkNetworkBeforeAction()) {
                    handleGenderSelected("Female")
                }
            }

            buttonOther.setOnClickListener {
                if (checkNetworkBeforeAction()) {
                    handleGenderSelected("Other")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up gender selection: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        isCardClicked = false // Allow clicking cards again
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

    private fun checkNetworkBeforeAction(): Boolean {
        if (!ConnectivityHelper.isNetworkAvailable(this)) {
            ConnectivityHelperExt.checkNetworkAndNotify(this, rootView)
            return false
        }
        return true
    }

    private fun handleGenderSelected(gender: String) {
        if (isCardClicked) return // Prevent multiple clicks
        isCardClicked = true

        viewModel.saveGender(gender)
    }

    private fun initViews() {
        rootView = findViewById(R.id.rootViewGenderSelection)
        networkMessageText = findViewById(R.id.networkMessageText)
        titleText = findViewById(R.id.textview_title)
        subtitleText = findViewById(R.id.textview_subtitle)
        backButton = findViewById(R.id.button_back_gender)
    }

    private fun setupObservers() {
        viewModel.saveSuccessEvent.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Gender updated successfully!", Toast.LENGTH_SHORT).show()
                finish() // Return to profile in edit mode
            }
        }

        viewModel.errorEvent.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            isCardClicked = false // Reset flag to allow retrying
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
}