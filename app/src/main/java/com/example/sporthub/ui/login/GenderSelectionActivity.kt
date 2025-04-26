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
import com.example.sporthub.R
import com.example.sporthub.utils.ConnectivityHelper
import com.example.sporthub.utils.ConnectivityHelperExt
import com.example.sporthub.viewmodel.GenderSelectionViewModel

class GenderSelectionActivity : AppCompatActivity() {

    private lateinit var cardMale: CardView
    private lateinit var cardFemale: CardView
    private lateinit var cardOther: CardView
    private lateinit var networkMessageText: TextView
    private lateinit var rootView: View
    private var isCardClicked = false

    private lateinit var viewModel: GenderSelectionViewModel
    private val TAG = "GenderSelectionActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_gender_selection)

        // Initialize the ViewModel
        viewModel = ViewModelProvider(this).get(GenderSelectionViewModel::class.java)

        // Verify authentication
        if (!viewModel.checkAuthentication()) {
            redirectToSignIn()
            return
        }

        // Initialize views
        rootView = findViewById(R.id.rootViewGenderSelection)
        networkMessageText = findViewById(R.id.networkMessageText)

        // Check connectivity initially
        checkConnectivity()

        // Configure observers for events from the ViewModel
        setupObservers()

        // Handle device back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@GenderSelectionActivity,
                    "Going back to name selection!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        })

        // Handle new back button
        val backButton = findViewById<ImageButton>(R.id.button_back_gender)
        backButton.setOnClickListener {
            finish()
        }

        try {
            // Configure listeners for the buttons
            cardMale = findViewById(R.id.button_male)
            cardFemale = findViewById(R.id.button_female)
            cardOther = findViewById(R.id.button_other)

            cardMale.setOnClickListener {
                if (checkNetworkBeforeAction()) {
                    handleGenderSelected("male")
                }
            }
            cardFemale.setOnClickListener {
                if (checkNetworkBeforeAction()) {
                    handleGenderSelected("female")
                }
            }
            cardOther.setOnClickListener {
                if (checkNetworkBeforeAction()) {
                    handleGenderSelected("other")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up gender selection: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        isCardClicked = false // Allow clicking cards again
        cardMale.isEnabled = true
        cardFemale.isEnabled = true
        cardOther.isEnabled = true
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

    private fun disableAllCards() {
        cardMale.isEnabled = false
        cardFemale.isEnabled = false
        cardOther.isEnabled = false
    }

    private fun handleGenderSelected(gender: String) {
        if (isCardClicked) return // Prevent multiple clicks fast
        isCardClicked = true

        disableAllCards()
        viewModel.saveGender(gender)
    }

    private fun setupObservers() {
        viewModel.saveSuccessEvent.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Gender selected successfully!", Toast.LENGTH_SHORT).show()
                navigateToBirthDateSelection()
            }
        }

        viewModel.errorEvent.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            isCardClicked = false // Reset flag to allow retrying
            cardMale.isEnabled = true
            cardFemale.isEnabled = true
            cardOther.isEnabled = true
        }

        viewModel.userNotAuthenticatedEvent.observe(this) { notAuthenticated ->
            if (notAuthenticated) {
                Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
                redirectToSignIn()
            }
        }
    }

    private fun navigateToBirthDateSelection() {
        val intent = Intent(this, BirthDateSelectionActivity::class.java)
        startActivity(intent)
        // Do not finish this activity yet, to allow proper back navigation
    }

    private fun redirectToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}