package com.example.sporthub.ui.profile.edit

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import com.example.sporthub.R
import com.example.sporthub.data.repository.UserRepository
import com.example.sporthub.ui.MainActivity
import com.example.sporthub.viewmodel.SportSelectionViewModel

class EditSportsActivity : AppCompatActivity() {

    private lateinit var viewModel: SportSelectionViewModel
    private val TAG = "EditSportsActivity"
    private var isEditMode = false

    // Map to track selected sports
    private val selectedSports = mutableMapOf(
        "Basketball" to false,
        "Football" to false,
        "Volleyball" to false,
        "Tennis" to false
    )

    private lateinit var basketballCard: CardView
    private lateinit var footballCard: CardView
    private lateinit var volleyballCard: CardView
    private lateinit var tennisCard: CardView
    private lateinit var saveButton: Button
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_favorite_sports_selection)

        // Get edit mode from intent
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false)

        // Initialize the ViewModel
        viewModel = ViewModelProvider(this).get(SportSelectionViewModel::class.java)

        // Check authentication
        if (!viewModel.checkAuthentication()) {
            finish()
            return
        }

        // Initialize UI components
        initViews()

        // Set up observers
        setupObservers()

        // Configure the UI for edit mode
        setupEditMode()

        // Set up back button
        setupBackButton()

        try {
            // Set up listeners for the sports cards
            setupSportCardListener(basketballCard, "Basketball")
            setupSportCardListener(footballCard, "Football")
            setupSportCardListener(volleyballCard, "Volleyball")
            setupSportCardListener(tennisCard, "Tennis")

            // Set up listener for the save button
            saveButton.setOnClickListener {
                saveSportsAndProceed()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up sports selection: ${e.message}")
        }
    }

    private fun initViews() {
        basketballCard = findViewById(R.id.button_basketball)
        footballCard = findViewById(R.id.button_football)
        volleyballCard = findViewById(R.id.button_volleyball)
        tennisCard = findViewById(R.id.button_tennis)
        saveButton = findViewById(R.id.button_discover)
        titleText = findViewById(R.id.textview_title)
        subtitleText = findViewById(R.id.textview_subtitle)
        backButton = findViewById(R.id.button_back_sport)
    }

    private fun setupObservers() {
        viewModel.saveSuccessEvent.observe(this) { success ->
            if (success) {
                if (isEditMode) {
                    Toast.makeText(this, "Sports preferences updated!", Toast.LENGTH_SHORT).show()
                    finish() // Return to profile in edit mode
                } else {
                    // Continue to main activity for new users
                    Toast.makeText(this, "Sports preferences saved!", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
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
            titleText.text = "Edit Your Favorite Sports"
            subtitleText.text = "Update your sport preferences"
            saveButton.text = "Save Changes"

            // Try to load current sports for user
            try {
                val repository = UserRepository()
                val userId = repository.getCurrentUser()?.uid
                if (userId != null) {
                    repository.getUserModel(userId).observe(this) { user ->
                        // Pre-select the user's current sports
                        user.sportsLiked.forEach { sport ->
                            when (sport.name) {
                                "Basketball" -> {
                                    selectedSports["Basketball"] = true
                                    updateCardAppearance(basketballCard, true)
                                }
                                "Football" -> {
                                    selectedSports["Football"] = true
                                    updateCardAppearance(footballCard, true)
                                }
                                "Volleyball" -> {
                                    selectedSports["Volleyball"] = true
                                    updateCardAppearance(volleyballCard, true)
                                }
                                "Tennis" -> {
                                    selectedSports["Tennis"] = true
                                    updateCardAppearance(tennisCard, true)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading current sports: ${e.message}")
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

    private fun setupSportCardListener(cardView: CardView, sportName: String) {
        cardView.setOnClickListener {
            // Toggle selection state
            selectedSports[sportName] = !selectedSports[sportName]!!

            // Update card appearance based on selection state
            updateCardAppearance(cardView, selectedSports[sportName]!!)
        }
    }

    private fun updateCardAppearance(cardView: CardView, isSelected: Boolean) {
        if (isSelected) {
            // Use a very light lavender background to indicate selection
            cardView.setCardBackgroundColor(Color.parseColor("#EDE7F6"))
            // Increase elevation for "raised" effect
            cardView.cardElevation = 8f
        } else {
            // Restore to white when not selected
            cardView.setCardBackgroundColor(Color.WHITE)
            // Restore original elevation
            cardView.cardElevation = 2f
        }
    }

    private fun saveSportsAndProceed() {
        // Get list of selected sports keys
        val selectedSportsKeys = selectedSports.filter { it.value }.keys.toList()

        // Delegate the logic to the ViewModel
        viewModel.saveSportsPreferences(selectedSportsKeys)
    }

    private fun navigateToMainActivity() {
        // Only navigate to main activity in registration flow (non-edit mode)
        if (!isEditMode) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        finish()
    }
}