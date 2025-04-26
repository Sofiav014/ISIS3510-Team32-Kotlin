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


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var userRepository: UserRepository
    private val sharedUserViewModel: SharedUserViewModel by viewModels()
    private lateinit var bottomNavigationView: BottomNavigationView

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

        userRepository.getUserModel(uid).observe(this) { user ->
            if (user != null && (user.id != "")) {
                currentUser = user
                Log.d(TAG, "Usuario cargado: ${currentUser?.name}")
                sharedUserViewModel.setUser(user)

                // Inflar la UI solo si tenemos al usuario
                binding = ActivityMainBinding.inflate(layoutInflater)
                setContentView(binding.root)

                setupNavigation()
            } else {
                Log.e(TAG, "Usuario no encontrado o error al obtener usuario")
                goToSignIn()
            }
        }

        // After loading the user and setting it in the shared view model
        val isDarkMode = LocalThemeManager.getUserTheme(this, uid) ?: false
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES && !isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO && isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
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
        val navController = navHostFragment.navController

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
            mAuth.signOut()
            mGoogleSignInClient.signOut().addOnCompleteListener(this) {
                goToSignIn()
            }
        } catch (e: Exception) {
            Log.e("SignOut", "Error durante logout: ${e.message}")
        }
    }

    private fun goToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}