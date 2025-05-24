package com.example.sporthub.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.sporthub.R
import com.example.sporthub.databinding.ActivityMainBinding
import com.example.sporthub.data.model.User
import com.example.sporthub.data.repository.UserRepository
import com.example.sporthub.ui.login.SignInActivity
import com.example.sporthub.viewmodel.SharedUserViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.NavController
import com.example.sporthub.utils.LocalThemeManager
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.activity.OnBackPressedCallback
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var userRepository: UserRepository
    private val sharedUserViewModel: SharedUserViewModel by viewModels()
    private lateinit var navController: NavController

    var currentUser: User? = null
        private set

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        userRepository = UserRepository()

        val authUser = mAuth.currentUser
        if (authUser == null) {
            goToSignIn()
            return
        }

        val uid = authUser.uid
        applyUserThemePreference(uid)

        val existingUser = sharedUserViewModel.currentUser.value
        if (existingUser != null) {
            currentUser = existingUser
            finishMainSetup()
        } else {
            if (isNetworkAvailable()) {
                userRepository.getUserModel(uid).observe(this) { user ->
                    if (user != null && user.id != "") {
                        currentUser = user
                        sharedUserViewModel.setUser(user)
                        finishMainSetup()
                    } else {
                        goToSignIn()
                    }
                }
            } else {
                Log.e(TAG, "No network and no cached user – cannot proceed")
                // Aquí podrías mostrar un mensaje de error amigable
            }
        }
    }

    private fun finishMainSetup() {
        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController

        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        setupToolbarTitles()

        // Fixed navigation with explicit navOptions DSL:
        // In MainActivity.kt, replace the setOnItemSelectedListener with this:

        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    val options = navOptions {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                    navController.navigate(R.id.navigation_home, null, options)
                    true
                }
                R.id.findVenuesFragment -> {
                    val options = navOptions {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                    navController.navigate(R.id.findVenuesFragment, null, options)
                    true
                }
                R.id.navigation_booking -> {
                    val options = navOptions {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                    navController.navigate(R.id.navigation_booking, null, options)
                    true
                }
                R.id.navigation_profile -> {
                    val options = navOptions {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                    navController.navigate(R.id.navigation_profile, null, options)
                    true
                }
                else -> false
            }
        }



        // Optionally handle reselection to pop back stack
        binding.navView.setOnItemReselectedListener { item ->
            navController.popBackStack(item.itemId, false)
        }

        setupBackHandling()
    }

    private fun setupToolbarTitles() {
        navController.addOnDestinationChangedListener { _, dest, _ ->
            binding.toolbarTitle.text = when (dest.id) {
                R.id.findVenuesFragment      -> "Find Venues"
                R.id.navigation_home         -> "SportHub"
                R.id.navigation_profile      -> "Profile"
                R.id.navigation_booking      -> "Bookings"
                R.id.navigation_create       -> "Create Booking"
                R.id.venueDetailFragment     -> "Venue Detail"
                R.id.venueListFragment       -> "Venue List"
                else                         -> "SportHub"
            }
            val showBack = dest.id in setOf(
                R.id.venueDetailFragment,
                R.id.venueListFragment
            )
            binding.topAppBar.navigationIcon = if (showBack) {
                AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back)
                    ?.apply { setTint(ContextCompat.getColor(this@MainActivity, android.R.color.white)) }
            } else null
            binding.topAppBar.setNavigationOnClickListener {
                if (showBack) onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
    }

    override fun onResume() {
        super.onResume()

        val isThemeChanging = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_theme_changing", false)

        if (isThemeChanging) {
            Log.d(TAG, "Resuming during theme transition - skipping operations")
            return
        }
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navController.currentDestination?.id == navController.graph.startDestinationId) {
                    moveTaskToBack(true)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun applyUserThemePreference(userId: String) {
        try {
            val isDarkMode = LocalThemeManager.getUserTheme(this, userId)
            Log.d(TAG, "User theme preference: isDarkMode=$isDarkMode")

            if (isDarkMode != null) {
                val currentMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
                if (isDarkMode != currentMode) {
                    Log.d(TAG, "Applying theme change: isDarkMode=$isDarkMode, current=$currentMode")
                    AppCompatDelegate.setDefaultNightMode(
                        if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                    )
                }
            } else {
                Log.d(TAG, "No theme preference, defaulting to light mode")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying theme preference: ${e.message}")
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun setupNavigation() {
        // IMPORTANT:
        // Make sure R.string.default_web_client_id exists in your strings.xml
        // This ID is generated by google-services.json (Firebase config)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val toolbar: MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)
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

            val showBackButton = destination.id == R.id.venueDetailFragment || destination.id == R.id.venueListFragment
            toolbar.navigationIcon = if (showBackButton) {
                AppCompatResources.getDrawable(this, R.drawable.ic_arrow_back)?.apply {
                    setTint(ContextCompat.getColor(this@MainActivity, R.color.primary))
                }
            } else null

            toolbar.setNavigationOnClickListener {
                if (showBackButton) onBackPressedDispatcher.onBackPressed()
            }
        }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.nav_view)
        bottomNavigationView.setupWithNavController(navController)
    }

    fun signOutAndGoToLogin() {
        try {
            val userId = mAuth.currentUser?.uid
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
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
