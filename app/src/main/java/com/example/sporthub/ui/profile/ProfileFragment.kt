package com.example.sporthub.ui.profile

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sporthub.R
import com.example.sporthub.data.model.Sport
import com.example.sporthub.data.model.User
import com.example.sporthub.ui.login.SignInActivity
import com.example.sporthub.viewmodel.SharedUserViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.example.sporthub.ui.profile.edit.EditNameActivity
import com.example.sporthub.ui.profile.edit.EditGenderActivity
import com.example.sporthub.ui.profile.edit.EditBirthDateActivity
import com.example.sporthub.ui.profile.edit.EditSportsActivity
import com.example.sporthub.utils.ThemeSwitcher

class ProfileFragment : Fragment() {

    private lateinit var viewModel: ProfileViewModel
    private val sharedUserViewModel: SharedUserViewModel by activityViewModels()

    private lateinit var profileName: TextView
    private lateinit var genderValue: TextView
    private lateinit var birthDateValue: TextView
    private lateinit var favoriteSportsContainer: LinearLayout
    private lateinit var favoriteVenuesRecyclerView: RecyclerView
    private lateinit var noFavoriteVenuesText: TextView
    private lateinit var settingsButton: Button
    private lateinit var logoutButton: Button

    // Theme mode UI elements
    private lateinit var themeIcon: ImageView
    private lateinit var themeLabel: TextView
    private lateinit var themeSwitch: SwitchCompat

    private lateinit var favoriteVenueAdapter: FavoriteVenueAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Use the updated layout
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application))
            .get(ProfileViewModel::class.java)

        // Initialize views
        initViews(view)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup observers
        setupObservers()

        // Setup button listeners
        setupButtons()

        // Initialize theme switch - Important: defer until we observe isDarkMode LiveData
        // DO NOT call initThemeSwitch() here

        // Load data
        viewModel.loadUserData()

        fun setupThemeSwitch() {
            // Get the current theme state (completely independent of any network operations)
            val isDarkMode = ThemeSwitcher.isDarkMode(requireContext())

            // Update UI accordingly
            themeSwitch.isChecked = isDarkMode
            themeLabel.text = if (isDarkMode) "Dark Mode" else "Light Mode"
            themeIcon.setImageResource(
                if (isDarkMode) R.drawable.ic_dark_mode else R.drawable.ic_light_mode
            )

            // Set up the switch listener
            themeSwitch.setOnCheckedChangeListener { _, isChecked ->
                // Disable the switch temporarily to prevent multiple toggles
                themeSwitch.isEnabled = false

                // Toggle the theme using our simplified implementation
                ThemeSwitcher.toggleTheme(requireContext())

                // Re-enable the switch after a short delay
                Handler(Looper.getMainLooper()).postDelayed({
                    themeSwitch.isEnabled = true
                }, 1000)
            }
        }

        fun updateThemeUI(isDarkMode: Boolean) {
            // Update theme label
            themeLabel.text = if (isDarkMode) "Dark Mode" else "Light Mode"

            // Update theme icon
            val iconResourceId = if (isDarkMode) {
                R.drawable.ic_dark_mode
            } else {
                R.drawable.ic_light_mode
            }

            try {
                themeIcon.setImageResource(iconResourceId)
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error setting theme icon: ${e.message}")
            }
        }

        fun observeThemeChanges() {
            // Observe theme mode changes
            viewModel.isDarkMode.observe(viewLifecycleOwner) { isDarkMode ->
                Log.d("ProfileFragment", "Theme changed. isDarkMode=$isDarkMode")

                // Update UI
                updateThemeUI(isDarkMode)

                // Update switch state without triggering listener
                if (themeSwitch.isChecked != isDarkMode) {
                    themeSwitch.setOnCheckedChangeListener(null)
                    themeSwitch.isChecked = isDarkMode
                    setupThemeSwitch()
                }
            }
        }


        setupThemeSwitch()
        observeThemeChanges()
    }

    private fun initViews(view: View) {
        profileName = view.findViewById(R.id.profileName)
        genderValue = view.findViewById(R.id.genderValue)
        birthDateValue = view.findViewById(R.id.birthDateValue)
        favoriteSportsContainer = view.findViewById(R.id.favoriteSportsContainer)
        favoriteVenuesRecyclerView = view.findViewById(R.id.favoriteVenuesRecyclerView)
        noFavoriteVenuesText = view.findViewById(R.id.noFavoriteVenuesText)
        settingsButton = view.findViewById(R.id.buttonSettings)
        logoutButton = view.findViewById(R.id.button_logout)

        // Theme mode UI elements - make sure these IDs match what's in your layout
        themeIcon = view.findViewById(R.id.themeIcon)
        themeLabel = view.findViewById(R.id.themeLabel)
        themeSwitch = view.findViewById(R.id.themeSwitch)
    }

    private fun setupRecyclerView() {
        favoriteVenueAdapter = FavoriteVenueAdapter()
        favoriteVenuesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = favoriteVenueAdapter
        }
    }

    private fun setupObservers() {
        // Get user data from shared view model if available
        sharedUserViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                updateUI(user)
            }
        }

        // Otherwise use the profile view model
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            updateUI(user)
        }

        viewModel.favoriteVenues.observe(viewLifecycleOwner) { venues ->
            favoriteVenueAdapter.submitList(venues)

            // Show or hide the no venues message
            if (venues.isNullOrEmpty()) {
                noFavoriteVenuesText.visibility = View.VISIBLE
                favoriteVenuesRecyclerView.visibility = View.GONE
            } else {
                noFavoriteVenuesText.visibility = View.GONE
                favoriteVenuesRecyclerView.visibility = View.VISIBLE
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        // Observe dark mode changes
        viewModel.isDarkMode.observe(viewLifecycleOwner) { isDarkMode ->
            Log.d("ProfileFragment", "Theme changed. isDarkMode=$isDarkMode")
            // This is important - first update UI, THEN initialize the switch
            updateThemeUI(isDarkMode)

            // Set the switch state without triggering the listener
            themeSwitch.setOnCheckedChangeListener(null)
            themeSwitch.isChecked = isDarkMode

            // Re-attach the listener after setting the state
            themeSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != viewModel.isDarkMode.value) {
                    viewModel.toggleDarkMode()
                }
            }
        }
    }

    private fun setupButtons() {
        settingsButton.setOnClickListener {
            // Open settings dialog
            showSettingsDialog()
        }

        logoutButton.setOnClickListener {
            signOutAndStartSignInActivity()
        }

        // Setup theme switch listener
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != viewModel.isDarkMode.value) {
                viewModel.toggleDarkMode()
            }
        }
    }

    private fun updateThemeUI(isDarkMode: Boolean) {
        // Update theme label
        themeLabel.text = if (isDarkMode) "Dark Mode" else "Light Mode"

        // Ensure we set the correct icon
        try {
            val iconResource = if (isDarkMode) {
                R.drawable.ic_dark_mode
            } else {
                R.drawable.ic_light_mode
            }
            themeIcon.setImageResource(iconResource)
        } catch (e: Exception) {
            // Log error but don't crash
            Log.e("ProfileFragment", "Error setting theme icon: ${e.message}")
        }
    }

    private fun updateUI(user: User) {
        // Update profile name
        profileName.text = user.name.ifEmpty { "Current User" }

        // Update gender
        genderValue.text = user.gender.ifEmpty { "Not specified" }

        // Update birth date
        birthDateValue.text = viewModel.formatBirthDate(user.birthDate)

        // Update favorite sports
        updateFavoriteSports(user.sportsLiked)

        // Display favorite venues
        favoriteVenueAdapter.submitList(user.venuesLiked)

        // Show or hide the no venues message
        if (user.venuesLiked.isNullOrEmpty()) {
            noFavoriteVenuesText.visibility = View.VISIBLE
            favoriteVenuesRecyclerView.visibility = View.GONE
        } else {
            noFavoriteVenuesText.visibility = View.GONE
            favoriteVenuesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateFavoriteSports(sports: List<Sport>) {
        favoriteSportsContainer.removeAllViews()

        if (sports.isEmpty()) {
            val textView = TextView(context)
            textView.text = "No favorite sports yet"
            textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            favoriteSportsContainer.addView(textView)
            return
        }

        for (sport in sports) {
            val sportIcon = ImageView(context)
            sportIcon.layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size),
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
            ).apply {
                marginEnd = resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin) / 2
            }

            sportIcon.background = ContextCompat.getDrawable(requireContext(), R.drawable.circle_purple_background)
            sportIcon.setPadding(8, 8, 8, 8)

            // Set the appropriate icon based on sport type
            val sportDrawable: Drawable? = when (sport.name.toLowerCase()) {
                "basketball" -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_basketball_logo)
                "football" -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_football_logo)
                "volleyball" -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_volleyball_logo)
                "tennis" -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_tennis_logo)
                else -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_sport_venue_card)
            }

            sportIcon.setImageDrawable(sportDrawable)
            favoriteSportsContainer.addView(sportIcon)
        }
    }

    private fun showSettingsDialog() {
        // Create the options
        val options = arrayOf(
            "Edit Profile Name",
            "Change Gender",
            "Update Birth Date",
            "Update Favorite Sports"
        )

        // Create and show the dialog
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startEditActivity(EditNameActivity::class.java)
                    1 -> startEditActivity(EditGenderActivity::class.java)
                    2 -> startEditActivity(EditBirthDateActivity::class.java)
                    3 -> startEditActivity(EditSportsActivity::class.java)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun <T> startEditActivity(activityClass: Class<T>) {
        val intent = Intent(requireContext(), activityClass)
        intent.putExtra("EDIT_MODE", true) // Flag to indicate edit mode vs new user registration
        viewModel.getCurrentUserId()?.let { userId ->
            intent.putExtra("USER_ID", userId)
        }
        startActivity(intent)
    }

    private fun signOutAndStartSignInActivity() {
        try {
            val mAuth = FirebaseAuth.getInstance()
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

            mAuth.signOut()

            mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity()) {
                val intent = Intent(requireActivity(), SignInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "Error signing out: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload data when returning to this fragment
        viewModel.loadUserData()
    }
}