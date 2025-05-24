package com.example.sporthub.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import android.graphics.Color
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.sporthub.R
import com.example.sporthub.ui.MainActivity
import com.example.sporthub.utils.ConnectivityHelper
import com.example.sporthub.utils.ConnectivityHelperExt
import com.example.sporthub.viewmodel.SportSelectionViewModel
import com.example.sporthub.utils.RegistrationTimerManager

class FavoriteSportsSelectionActivity : AppCompatActivity() {

    private lateinit var viewModel: SportSelectionViewModel
    private lateinit var networkMessageText: TextView
    private lateinit var rootView: View
    private val TAG = "FavoriteSportsSelectionActivity"
    private var hasNavigated = false // Flag to prevent double navigation

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
    private lateinit var discoverButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_favorite_sports_selection)

        // Initialize the ViewModel
        viewModel = ViewModelProvider(this).get(SportSelectionViewModel::class.java)

        // Verify authentication
        if (!viewModel.checkAuthentication()) {
            redirectToSignIn()
            return
        }

        // Initialize UI components and views
        rootView = findViewById(R.id.rootViewSports)
        networkMessageText = findViewById(R.id.networkMessageText)
        basketballCard = findViewById(R.id.button_basketball)
        footballCard = findViewById(R.id.button_football)
        volleyballCard = findViewById(R.id.button_volleyball)
        tennisCard = findViewById(R.id.button_tennis)
        discoverButton = findViewById(R.id.button_discover)

        // Check connectivity initially
        checkConnectivity()

        // Set up observers for events from the ViewModel
        setupObservers()

        // Handle back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@FavoriteSportsSelectionActivity,
                    "Going back to birthdate selector!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        })

        // Handle new back button
        val backButton = findViewById<ImageButton>(R.id.button_back_sport)
        backButton.setOnClickListener {
            finish()
        }

        try {
            // Set up listeners for the sports cards
            setupSportCardListener(basketballCard, "Basketball")
            setupSportCardListener(footballCard, "Football")
            setupSportCardListener(volleyballCard, "Volleyball")
            setupSportCardListener(tennisCard, "Tennis")

            // Set up listener for the DISCOVER button
            discoverButton.setOnClickListener {
                // Check connectivity before proceeding
                if (!ConnectivityHelper.isNetworkAvailable(this)) {
                    ConnectivityHelperExt.checkNetworkAndNotify(this, rootView)
                    return@setOnClickListener
                }

                saveSportsAndProceed()
                discoverButton.isEnabled = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up sports selection: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        discoverButton.isEnabled = true // Enable the Discover button again
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
                Toast.makeText(this, "Sports preferences saved!", Toast.LENGTH_SHORT).show()
                navigateToMainActivity()
            }
        }

        viewModel.errorEvent.observe(this) { errorMessage ->
            Log.d("SportsDebug", "Error event triggered: $errorMessage")
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            discoverButton.isEnabled = true
        }

        viewModel.userNotAuthenticatedEvent.observe(this) { notAuthenticated ->
            if (notAuthenticated) {
                Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
                redirectToSignIn()
            }
        }
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
        val selectedSportsKeys = selectedSports.filter { it.value }.keys.toList()
        Log.d("SportsDebug", "Selected sports: $selectedSportsKeys")

        if (selectedSportsKeys.isEmpty()) {
            Toast.makeText(this, "Please select at least one sport", Toast.LENGTH_SHORT).show()
            discoverButton.isEnabled = true
            return
        }

        Log.d("SportsDebug", "Calling viewModel.saveSportsPreferences()")
        viewModel.saveSportsPreferences(selectedSportsKeys)
    }

    private fun navigateToMainActivity() {
        Log.d("SportsDebug", "Navigating to MainActivity")

        // In navigateToMainActivity method before starting MainActivity:
        RegistrationTimerManager.stopTimerAndSave()
        Log.d("FavoriteSportsActivity", "Stopping registration timer and saving")
        val intent = Intent(this, MainActivity::class.java)


        // Clear the activity stack so user can't go back to registration screens
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun redirectToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}