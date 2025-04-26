package com.example.sporthub.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.sporthub.R
import com.example.sporthub.databinding.ActivityMainBinding
import com.example.sporthub.data.model.User
import com.example.sporthub.data.repository.UserRepository
import com.example.sporthub.ui.login.SignInActivity
import com.example.sporthub.viewmodel.SharedUserViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.activity.viewModels

import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.sporthub.ui.bookings.BookingsFragment
import com.example.sporthub.ui.createBooking.CreateBookingFragment
import com.example.sporthub.ui.findVenues.FindVenuesFragment
import com.example.sporthub.ui.profile.ProfileFragment
import com.example.sporthub.utils.LocalThemeManager
import com.google.android.material.appbar.MaterialToolbar
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var userRepository: UserRepository
    private val sharedUserViewModel: SharedUserViewModel by viewModels()
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navController: NavController

    var currentUser: User? = null
        private set

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        userRepository = UserRepository()

        val authUser = mAuth.currentUser
        if (authUser == null) {
            goToSignIn()
            return
        }

        val uid = authUser.uid

        // Apply saved theme preference immediately on startup
        applyUserThemePreference(uid)

        userRepository.getUserModel(uid).observe(this) { user ->
            if (user != null && (user.id != "")) {
                currentUser = user
                Log.d(TAG, "Usuario cargado: ${currentUser?.name}")
                sharedUserViewModel.setUser(user)

                // Inflar la UI solo si tenemos al usuario
                binding = ActivityMainBinding.inflate(layoutInflater)
                setContentView(binding.root)

                setupNavigation()

                // Setup back button handling
                setupBackHandling()
            } else {
                Log.e(TAG, "Usuario no encontrado o error al obtener usuario")
                goToSignIn()
            }
        }
    }

    /**
     * Setup proper back button handling
     */
    private fun setupBackHandling() {
        // Add a callback for handling the back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Check if we're at the start destination
                if (navController.currentDestination?.id == navController.graph.startDestinationId) {
                    // If at home/start destination, minimize the app instead of exiting
                    moveTaskToBack(true)
                } else {
                    // If not at the start destination, do normal navigation
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    /**
     * Apply the user's saved theme preference
     */
    private fun applyUserThemePreference(userId: String) {
        try {
            // Get the user's theme preference
            val isDarkMode = LocalThemeManager.getUserTheme(this, userId)

            Log.d(TAG, "User theme preference: isDarkMode=$isDarkMode")

            // Apply the theme based on the preference
            if (isDarkMode != null) {
                val currentMode = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) true else false

                // Only apply if different from current to avoid unnecessary recreation
                if (isDarkMode != currentMode) {
                    Log.d(TAG, "Applying theme change: isDarkMode=$isDarkMode, current=$currentMode")

                    if (isDarkMode) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                }
            } else {
                // Default to light mode if no preference set
                Log.d(TAG, "No theme preference, defaulting to light mode")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying theme preference: ${e.message}")
            // Default to light mode in case of error
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    // Snippet from MainActivity.kt showing toolbar setup
    private fun setupNavigation() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        // Get NavController from NavHostFragment
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val toolbar: MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)
        // Remove default title to use our custom title TextView
        supportActionBar?.setDisplayShowTitleEnabled(false)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val titleView: TextView? = findViewById(R.id.toolbarTitle)

            if (destination.id != R.id.venueListFragment) {
                titleView?.text = when (destination.id) {
                    R.id.findVenuesFragment -> "Find Venues"
                    R.id.navigation_home -> "SportHub"
                    R.id.navigation_profile -> "Profile"
                    R.id.navigation_booking -> "Bookings"
                    R.id.navigation_create -> "Create Booking"
                    R.id.venueDetailFragment -> "Venue Detail"
                    else -> "SportHub"
                }
            }

            // Show or hide the back button manually
            val showBackButton = destination.id == R.id.venueDetailFragment || destination.id == R.id.venueListFragment
            toolbar.navigationIcon = if (showBackButton) {
                AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back)?.apply {
                    // Tint the back arrow to match the primary color in light mode
                    setTint(ContextCompat.getColor(this@MainActivity, R.color.primary))
                }
            } else {
                null
            }

            // Set what the back button does
            toolbar.setNavigationOnClickListener {
                if (showBackButton) onBackPressedDispatcher.onBackPressed()
            }
        }

        // Set up Bottom Navigation with NavController
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.nav_view)
        bottomNavigationView.setupWithNavController(navController)
    }

    fun signOutAndGoToLogin() {
        try {
            // Save current user ID before signing out
            val userId = mAuth.currentUser?.uid

            mAuth.signOut()
            mGoogleSignInClient.signOut().addOnCompleteListener(this) {
                // Clear theme preference on logout (optional)
                // userId?.let { LocalThemeManager.clearUserTheme(this, it) }

                goToSignIn()
            }
        } catch (e: Exception) {
            Log.e("SignOut", "Error durante logout: ${e.message}")
        }
    }

    private fun goToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        // Clear the task stack so users can't navigate back
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}