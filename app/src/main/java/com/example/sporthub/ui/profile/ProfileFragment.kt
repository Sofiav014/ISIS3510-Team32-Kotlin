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
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        )

        // Set click listener on profile image
        profileImage.setOnClickListener {
            profilePictureManager.showImagePickerDialog()
        }

        // Set click listener on add icon
        addProfilePictureIcon.setOnClickListener {
            profilePictureManager.showImagePickerDialog()
        }
    }

    private fun updateProfilePicture(downloadUrl: String) {
        // Save URL to Firebase user document
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            Log.d("ProfileFragment", "Updating profile picture with URL: $downloadUrl")

            viewModel.updateProfilePicture(userId, downloadUrl)

            // Update UI immediately with the new image
            loadProfileImage(downloadUrl)

            // Hide the add icon since we now have a profile picture
            addProfilePictureIcon.visibility = View.GONE

            Toast.makeText(requireContext(), "Profile picture updated successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("ProfileFragment", "User ID is null, cannot update profile picture")
            Toast.makeText(requireContext(), "Error: User not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfileImage(imageUrl: String?) {
        Log.d("ProfileFragment", "Loading profile image: $imageUrl")

        if (!imageUrl.isNullOrEmpty()) {
            // Load the profile picture
            Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_profile_outline)
                .error(R.drawable.ic_profile_outline)
                .circleCrop()
                .into(profileImage)

            // Hide the add icon
            addProfilePictureIcon.visibility = View.GONE
            Log.d("ProfileFragment", "Profile image loaded successfully")
        } else {
            // Show default profile picture and add icon
            profileImage.setImageResource(R.drawable.ic_profile_outline)
            addProfilePictureIcon.visibility = View.VISIBLE
            Log.d("ProfileFragment", "Showing default profile image")
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
                ctx.getDrawable(com.example.sporthub.R.drawable.ic_light_mode)
                ctx.getDrawable(com.example.sporthub.R.drawable.ic_dark_mode)
                ctx.getDrawable(com.example.sporthub.R.drawable.ic_home_outline)
                ctx.getDrawable(com.example.sporthub.R.drawable.ic_profile_outline)
                ctx.getDrawable(com.example.sporthub.R.drawable.ic_search_outline)
                ctx.getDrawable(com.example.sporthub.R.drawable.ic_calendar_outline)
                ctx.getDrawable(com.example.sporthub.R.drawable.ic_create_outline)
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error preloading resources: ${e.message}")
        }
    }

    private fun setupThemeSwitch() {
        themeSwitch.isChecked = viewModel.isDarkMode.value ?: false

        themeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked == viewModel.isDarkMode.value) return@setOnCheckedChangeListener

            buttonView.isEnabled = false

            val themeChangeText = "Applying ${if(isChecked) "dark" else "light"} theme..."
            val snackbar = Snackbar.make(requireView(), themeChangeText, Snackbar.LENGTH_SHORT)
            snackbar.show()

            updateThemeUI(isChecked)
            viewModel.toggleDarkMode()

            Handler(Looper.getMainLooper()).postDelayed({
                buttonView.isEnabled = true
            }, 1500)
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
            android.util.Log.e("ProfileFragment", "Error signing out: ${e.message}")
        }
    }

    override fun onStart() {
        super.onStart()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                favoriteVenuesViewModel.syncWithRemote()
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

        if (viewModel.userData.value == null) {
            Log.d("ProfileFragment", "User data is null, reloading")
            viewModel.loadUserData()
        } else {
            Log.d("ProfileFragment", "User data already loaded, checking profile picture")
            // Always try to load the latest profile picture
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                viewModel.loadProfilePicture(userId)
            }
        }
    }
}