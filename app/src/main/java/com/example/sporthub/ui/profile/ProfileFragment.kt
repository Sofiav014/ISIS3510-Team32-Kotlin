package com.example.sporthub.ui.profile

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.sporthub.R
import com.example.sporthub.data.model.Sport
import com.example.sporthub.data.model.User
import com.example.sporthub.ui.login.SignInActivity
import com.example.sporthub.utils.ConnectivityHelper
import com.example.sporthub.utils.ProfilePictureManager
import com.example.sporthub.viewmodel.SharedUserViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.example.sporthub.ui.profile.edit.EditNameActivity
import com.example.sporthub.ui.profile.edit.EditGenderActivity
import com.example.sporthub.ui.profile.edit.EditBirthDateActivity
import com.example.sporthub.ui.profile.edit.EditSportsActivity
import com.example.sporthub.viewmodel.FavoriteVenuesViewModel
import com.example.sporthub.utils.LoadingTimeTracker
import com.google.android.material.snackbar.Snackbar

class ProfileFragment : Fragment() {

    private lateinit var viewModel: ProfileViewModel
    private val sharedUserViewModel: SharedUserViewModel by activityViewModels()
    private val favoriteVenuesViewModel: FavoriteVenuesViewModel by viewModels()

    private lateinit var profileName: TextView
    private lateinit var genderValue: TextView
    private lateinit var birthDateValue: TextView
    private lateinit var favoriteSportsContainer: LinearLayout
    private lateinit var favoriteVenuesRecyclerView: RecyclerView
    private lateinit var noFavoriteVenuesText: TextView
    private lateinit var settingsButton: Button
    private lateinit var logoutButton: Button
    private lateinit var profileImage: ImageView
    private lateinit var addProfilePictureIcon: ImageView

    // Theme mode UI elements
    private lateinit var themeIcon: ImageView
    private lateinit var themeLabel: TextView
    private lateinit var themeSwitch: SwitchCompat

    private lateinit var favoriteVenueAdapter: FavoriteVenueAdapter
    private lateinit var profilePictureManager: ProfilePictureManager

    // Network callback for syncing when connection is restored
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Start the loading time tracker
        LoadingTimeTracker.start()

        // Use the updated layout
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preloadThemeResources()
        // Initialize ViewModel
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application))
            .get(ProfileViewModel::class.java)

        // Initialize views
        initViews(view)

        // Initialize ProfilePictureManager
        initProfilePictureManager()

        // Setup RecyclerView
        setupRecyclerView()

        // Setup observers
        setupObservers()

        // Setup button listeners
        setupButtons()

        // Load data
        viewModel.loadUserData()

        // Sync with remote if online
        if (ConnectivityHelper.isNetworkAvailable(requireContext())) {
            favoriteVenuesViewModel.syncWithRemote()
        }
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
        profileImage = view.findViewById(R.id.profileImage)
        addProfilePictureIcon = view.findViewById(R.id.addProfilePictureIcon)

        // Theme mode UI elements
        themeIcon = view.findViewById(R.id.themeIcon)
        themeLabel = view.findViewById(R.id.themeLabel)
        themeSwitch = view.findViewById(R.id.themeSwitch)
    }

    private fun initProfilePictureManager() {
        profilePictureManager = ProfilePictureManager(
            fragment = this,
            onImageSelected = { downloadUrl ->
                // Update profile picture in Firebase and UI
                updateProfilePicture(downloadUrl)
            },
            onError = { errorMessage ->
                // Show user-friendly error messages
                showProfilePictureError(errorMessage)
            }
        )

        // Set click listener on profile image
        profileImage.setOnClickListener {
            // Check connectivity before allowing profile picture change
            if (!ConnectivityHelper.isNetworkAvailable(requireContext())) {
                showProfilePictureError("No internet connection. Profile picture changes require internet access. Please check your connection and try again.")
                return@setOnClickListener
            }
            profilePictureManager.showImagePickerDialog()
        }

        // Set click listener on add icon
        addProfilePictureIcon.setOnClickListener {
            // Check connectivity before allowing profile picture change
            if (!ConnectivityHelper.isNetworkAvailable(requireContext())) {
                showProfilePictureError("No internet connection. Profile picture changes require internet access. Please check your connection and try again.")
                return@setOnClickListener
            }
            profilePictureManager.showImagePickerDialog()
        }
    }

    private fun showProfilePictureError(errorMessage: String) {
        // Show error message with appropriate styling
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Profile Picture Error")
            .setMessage(errorMessage)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun updateProfilePicture(downloadUrl: String) {
        // Check connectivity before updating
        if (!ConnectivityHelper.isNetworkAvailable(requireContext())) {
            showProfilePictureError("Connection lost during profile picture update. Please check your internet connection and try again.")
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            Log.d("ProfileFragment", "Updating profile picture with URL: $downloadUrl")

            viewModel.updateProfilePicture(userId, downloadUrl)

            // Update UI immediately with the new image
            loadProfileImage(downloadUrl)

            Toast.makeText(requireContext(), "Profile picture updated successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("ProfileFragment", "User ID is null, cannot update profile picture")
            showProfilePictureError("User authentication error. Please try signing in again.")
        }
    }

    private fun loadProfileImage(imageUrl: String?) {
        Log.d("ProfileFragment", "Loading profile image: $imageUrl")

        if (!imageUrl.isNullOrEmpty()) {
            // Check if we have connectivity for loading image
            if (ConnectivityHelper.isNetworkAvailable(requireContext())) {
                // Load the profile picture with circular crop and border
                Glide.with(this)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_profile_outline)
                    .error(R.drawable.ic_profile_outline)
                    .circleCrop()
                    .into(profileImage)

                addProfilePictureIcon.visibility = View.VISIBLE
                Log.d("ProfileFragment", "Profile image loaded successfully")
            } else {
                // No connectivity - show cached image if available, otherwise default
                Glide.with(this)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .onlyRetrieveFromCache(true) // Only load from cache when offline
                    .placeholder(R.drawable.ic_profile_outline)
                    .error(R.drawable.ic_profile_outline)
                    .circleCrop()
                    .into(profileImage)

                addProfilePictureIcon.visibility = View.VISIBLE
                Log.d("ProfileFragment", "Loading profile image from cache (offline)")
            }
        } else {
            // Show default profile picture
            Glide.with(this)
                .load(R.drawable.ic_profile_outline)
                .circleCrop()
                .into(profileImage)

            addProfilePictureIcon.visibility = View.VISIBLE
            Log.d("ProfileFragment", "Showing default profile image")
        }
    }

    private fun updateProfilePictureUI() {
        val isOnline = ConnectivityHelper.isNetworkAvailable(requireContext())

        if (!isOnline) {
            // Show offline indicator
            addProfilePictureIcon.alpha = 0.5f // Dim the add icon

            // Add long click listener to explain why it's disabled
            profileImage.setOnLongClickListener {
                showProfilePictureError("Profile picture changes are disabled while offline. Please connect to the internet to change your profile picture.")
                true
            }

            addProfilePictureIcon.setOnLongClickListener {
                showProfilePictureError("Profile picture changes are disabled while offline. Please connect to the internet to change your profile picture.")
                true
            }
        } else {
            // Normal online state
            addProfilePictureIcon.alpha = 1.0f
            profileImage.setOnLongClickListener(null)
            addProfilePictureIcon.setOnLongClickListener(null)
        }
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
                Log.d("ProfileFragment", "User data from shared viewmodel: ${user.name}")
                updateUI(user)
            }
        }

        // Otherwise use the profile view model
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                Log.d("ProfileFragment", "User data from profile viewmodel: ${user.name}")
                updateUI(user)

                // Stop the loading time tracker after the UI is updated
                LoadingTimeTracker.stopAndRecord("Profile View", requireContext())
            }
        }

        // Observe profile picture updates
        viewModel.profilePictureUrl.observe(viewLifecycleOwner) { imageUrl ->
            Log.d("ProfileFragment", "Profile picture URL updated: $imageUrl")
            loadProfileImage(imageUrl)
        }

        // Observe favorite venues from Room database instead of Firebase
        favoriteVenuesViewModel.favoriteVenues.observe(viewLifecycleOwner) { venues ->
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
            updateThemeUI(isDarkMode)

            // Set the switch state without triggering the listener
            themeSwitch.setOnCheckedChangeListener(null)
            themeSwitch.isChecked = isDarkMode

            // Re-attach the listener after setting the state
            themeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked != viewModel.isDarkMode.value) {
                    // Disable the switch briefly to prevent multiple rapid toggles
                    buttonView.isEnabled = false

                    // Update UI immediately
                    updateThemeUI(isChecked)

                    // Apply the theme change
                    viewModel.toggleDarkMode()

                    // Re-enable after a delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        buttonView.isEnabled = true
                    }, 1000)
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

    private fun preloadThemeResources() {
        try {
            context?.let { ctx ->
                ctx.getDrawable(R.drawable.ic_light_mode)
                ctx.getDrawable(R.drawable.ic_dark_mode)
                ctx.getDrawable(R.drawable.ic_home_outline)
                ctx.getDrawable(R.drawable.ic_profile_outline)
                ctx.getDrawable(R.drawable.ic_search_outline)
                ctx.getDrawable(R.drawable.ic_calendar_outline)
                ctx.getDrawable(R.drawable.ic_create_outline)
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error preloading resources: ${e.message}")
        }
    }

    private fun updateThemeUI(isDarkMode: Boolean) {
        themeLabel.text = if (isDarkMode) "Dark Mode" else "Light Mode"

        try {
            val iconResource = if (isDarkMode) {
                R.drawable.ic_dark_mode
            } else {
                R.drawable.ic_light_mode
            }
            themeIcon.setImageResource(iconResource)
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error setting theme icon: ${e.message}")
        }
    }

    private fun updateUI(user: User) {
        profileName.text = user.name.ifEmpty { "Current User" }
        genderValue.text = user.gender.ifEmpty { "Not specified" }
        birthDateValue.text = viewModel.formatBirthDate(user.birthDate)
        updateFavoriteSports(user.sportsLiked)

        // Load profile picture if available
        Log.d("ProfileFragment", "Loading profile picture for user: ${user.id}")
        viewModel.loadProfilePicture(user.id)
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
        val options = arrayOf(
            "Edit Profile Name",
            "Change Gender",
            "Update Birth Date",
            "Update Favorite Sports"
        )

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
        intent.putExtra("EDIT_MODE", true)
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
            Log.e("ProfileFragment", "Error signing out: ${e.message}")
        }
    }

    override fun onStart() {
        super.onStart()

        // Register network callback for real-time connectivity changes
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                activity?.runOnUiThread {
                    Log.d("ProfileFragment", "Network available - enabling profile picture features")
                    updateProfilePictureUI()
                }
            }

            override fun onLost(network: Network) {
                activity?.runOnUiThread {
                    Log.d("ProfileFragment", "Network lost - disabling profile picture features")
                    updateProfilePictureUI()

                    // Show a brief message about offline state
                    Toast.makeText(requireContext(),
                        "Connection lost. Profile picture changes disabled until reconnected.",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }

        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
        } else {
            val request = NetworkRequest.Builder().build()
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        }

        // Sync favorite venues when connectivity is available
        if (ConnectivityHelper.isNetworkAvailable(requireContext())) {
            favoriteVenuesViewModel.syncWithRemote()
        }
    }

    override fun onStop() {
        super.onStop()

        networkCallback?.let {
            val connectivityManager =
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(it)
        }
    }

    override fun onResume() {
        super.onResume()

        val isThemeChanging = requireContext()
            .getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_theme_changing", false)

        if (isThemeChanging) {
            Log.d("ThemeAware", "Skipping network operations during theme change")
            return
        }

        // Update profile picture UI based on connectivity
        updateProfilePictureUI()

        // Force refresh through SharedUserViewModel
        sharedUserViewModel.refreshCurrentUser()

        // Also refresh profile picture if we have a user
        sharedUserViewModel.currentUser.value?.let { user ->
            viewModel.loadProfilePicture(user.id)
        }

        // Sync favorite venues
        if (ConnectivityHelper.isNetworkAvailable(requireContext())) {
            favoriteVenuesViewModel.syncWithRemote()
        }
    }
}