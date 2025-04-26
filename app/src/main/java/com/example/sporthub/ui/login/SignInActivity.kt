package com.example.sporthub.ui.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.sporthub.R
import com.example.sporthub.ui.MainActivity
import com.example.sporthub.utils.ConnectivityHelper
import com.example.sporthub.utils.ConnectivityHelperExt
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.example.sporthub.utils.LocalThemeManager
import androidx.appcompat.app.AppCompatDelegate

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import kotlinx.coroutines.tasks.await



class SignInActivity : AppCompatActivity() {

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "SignInActivity"

        var preferencesAlreadyChecked = false
    }

    // to make event connectivity message
    private lateinit var rootView: View
    private lateinit var networkMessageText: TextView
    private lateinit var signInButton: LinearLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var preferencesAlreadyChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        rootView = findViewById(R.id.rootView) // Make sure to add this ID to your layout
        networkMessageText = findViewById(R.id.networkMessageText) // Add this to your layout
        signInButton = findViewById(R.id.signInButton)

        val currentUser = auth.currentUser

        if (currentUser != null && !preferencesAlreadyChecked) {
            // Only check user if there's network connectivity
            if (ConnectivityHelper.isNetworkAvailable(this)) {
                checkUserExistsInFirestore(currentUser.uid)
            } else {
                // Show offline message and enable sign-in button
                showOfflineMessage()
            }
        }

        signInButton.setOnClickListener {
            Log.d(TAG, "Sign In button clicked")
            // Check network before proceeding
            if (ConnectivityHelperExt.checkNetworkAndNotify(this, rootView)) {
                signIn()
            }
        }
    }

    // Block back button to prevent app closure during sign-in
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // Either do nothing, or minimize the app
        moveTaskToBack(true)
    }

    private fun signIn() {
        Log.d(TAG, "Starting Google sign-in intent")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Signed in as ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    // Only proceed if we have network connectivity
                    if (ConnectivityHelper.isNetworkAvailable(this)) {
                        if (user != null) {
                            // Launch a coroutine to fetch user data from Firestore
                            checkUserExistsInFirestore(user.uid)
                        }
                    } else {
                        showOfflineMessage()
                    }
                } else {
                    Log.e(TAG, "Authentication failed", task.exception)
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showOfflineMessage() {
        networkMessageText.visibility = View.VISIBLE
        networkMessageText.text = "You're offline. Please check your internet connection and try again."
    }

    private fun hideOfflineMessage() {
        networkMessageText.visibility = View.GONE
    }

    /**
     * Check if user exists and has all required profile data
     * If profile is incomplete, redirect to the appropriate step
     */
    private fun checkUserExistsInFirestore(userId: String) {
        // First, check for internet connectivity
        if (!ConnectivityHelper.isNetworkAvailable(this)) {
            showOfflineMessage()
            return
        }
        hideOfflineMessage()  // we're online, so hide any offline indicator

        // Launch a coroutine on a background thread for Firestore operations
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Fetch the user's document from Firestore (suspend until the task completes)
                val document = db.collection("users").document(userId).get().await()
                // Switch to the Main thread to work with UI and navigate
                withContext(Dispatchers.Main) {
                    // 1. Check if the document exists in Firestore
                    if (!document.exists()) {
                        // No user document found – start profile setup from the beginning (Name)
                        navigateToNameSelectionActivity()
                        return@withContext  // exit the coroutine block early
                    }
                    // 2. Check if Name is provided
                    val name = document.getString("name").orEmpty()
                    if (name.isEmpty()) {
                        navigateToNameSelectionActivity()
                        return@withContext
                    }
                    // 3. Check if Gender is provided
                    val gender = document.getString("gender").orEmpty()
                    if (gender.isEmpty()) {
                        navigateToGenderSelectionActivity()
                        return@withContext
                    }
                    // 4. Check if Birth Date is provided
                    val birthDate = document.get("birth_date")
                    if (birthDate == null || (birthDate is String && birthDate.isEmpty())) {
                        navigateToBirthDateSelectionActivity()
                        return@withContext
                    }
                    // 5. Check if at least one favorite sport is selected
                    val sportsLiked = document.get("sports_liked")
                    if (sportsLiked == null || (sportsLiked is List<*> && sportsLiked.isEmpty())) {
                        navigateToSportsSelectionActivity()
                        return@withContext
                    }

                    // If we've reached here, all required fields are present and profile is complete.
                    preferencesAlreadyChecked = true  // mark that we've verified the profile

                    // Apply the user's theme preference (if available)
                    val isDarkMode = LocalThemeManager.getUserTheme(this@SignInActivity, userId)
                    if (isDarkMode != null) {
                        // Set dark or light mode based on saved preference
                        AppCompatDelegate.setDefaultNightMode(
                            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                            else AppCompatDelegate.MODE_NIGHT_NO
                        )
                    } else {
                        // If no preference saved, default to light mode
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }

                    // Navigate to MainActivity since the user’s profile is complete
                    navigateToMainActivity()
                }
            } catch (e: Exception) {
                // Handle any errors during Firestore fetch (e.g., network issues)
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error checking user data: ${e.message}")
                    // Show a friendly error message
                    Snackbar.make(rootView, "Failed to fetch your profile. Please try again.", Snackbar.LENGTH_LONG).show()
                    // On failure, treat as a new user (ensure light mode and start profile setup from Name)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    navigateToNameSelectionActivity()
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        // Clear the task stack so users can't navigate back
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToNameSelectionActivity() {
        val intent = Intent(this, NameSelectionActivity::class.java)
        // Don't clear stack here, as we need the registration flow to work
        startActivity(intent)
        finish()
    }

    private fun navigateToGenderSelectionActivity() {
        val intent = Intent(this, GenderSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToBirthDateSelectionActivity() {
        val intent = Intent(this, BirthDateSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToSportsSelectionActivity() {
        val intent = Intent(this, FavoriteSportsSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }
}