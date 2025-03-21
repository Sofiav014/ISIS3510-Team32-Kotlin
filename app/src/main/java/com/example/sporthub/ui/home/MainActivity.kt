package com.example.sporthub.ui.home

import android.os.Bundle
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.sporthub.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import android.util.Log
import androidx.fragment.app.Fragment
import com.example.sporthub.R
import com.example.sporthub.ui.bookings.BookingsFragment
import com.example.sporthub.ui.createBooking.CreateBookingFragment
import com.example.sporthub.ui.findVenues.FindVenuesFragment
import com.example.sporthub.ui.login.SignInActivity
import com.example.sporthub.ui.profile.ProfileFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Initialize binding first
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Initialize Firebase Auth
            mAuth = FirebaseAuth.getInstance()

            // Setup Google Sign In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

            // Try-catch for finding the TextView
            try {
                val textView = findViewById<TextView>(R.id.name)

                // Use FirebaseAuth directly
                val auth = FirebaseAuth.getInstance()
                val user = auth.currentUser

                if (user != null) {
                    val userName = user.displayName ?: "User" // Provide default if null
                    textView.text = "Welcome, $userName"
                } else {
                    textView.text = "Welcome, Guest"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting text view: ${e.message}")
            }

            // Navigation setup ->
            //val navView: BottomNavigationView = binding.navView

            //val navController = findNavController(R.id.nav_host_fragment_activity_main)
            //val appBarConfiguration = AppBarConfiguration(
            //    setOf(
            //        R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            //    )
            //)

            bottomNavigationView = findViewById(R.id.nav_view)

            bottomNavigationView.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.navigation_home -> {
                        replaceFragment(HomeFragment())
                        true
                    }
                    R.id.navigation_search -> {
                        replaceFragment(FindVenuesFragment())
                        true
                    }
                    R.id.navigation_booking -> {
                        replaceFragment(BookingsFragment())
                        true
                    }
                    R.id.navigation_create -> {
                        replaceFragment(CreateBookingFragment())
                        true
                    }
                    R.id.navigation_profile -> {
                        replaceFragment(ProfileFragment())
                        true
                    }
                    else -> false
                }
            }
            replaceFragment(HomeFragment())

            // Try-catch for finding the button
            try {
                val sign_out_button = findViewById<Button>(R.id.logout_button)
                sign_out_button.setOnClickListener {
                    signOutAndStartSignInActivity()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting button: ${e.message}")
            }

            //setupActionBarWithNavController(navController, appBarConfiguration)
            //navView.setupWithNavController(navController)

        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate: ${e.message}")
            e.printStackTrace()
        }
    }

    fun sendData(view: View) {
        try {
            val database = Firebase.database
            val myRef = database.getReference("message")
            myRef.setValue("Hello, World!")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending data: ${e.message}")
        }
    }

    private fun replaceFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction().replace(R.id.frame_container,fragment).commit()
    }

    private fun signOutAndStartSignInActivity() {
        try {
            mAuth.signOut()

            mGoogleSignInClient.signOut().addOnCompleteListener(this) {
                val intent = Intent(this@MainActivity, SignInActivity::class.java)
                startActivity(intent)
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out: ${e.message}")
        }
    }
}