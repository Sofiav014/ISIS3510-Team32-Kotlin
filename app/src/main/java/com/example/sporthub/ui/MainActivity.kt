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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.activity.viewModels


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var userRepository: UserRepository
    private val sharedUserViewModel: SharedUserViewModel by viewModels()

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


    }

    private fun setupNavigation() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment

        val navController = navHostFragment.navController

        val bottomNavigationView: BottomNavigationView = binding.navView
        NavigationUI.setupWithNavController(bottomNavigationView, navController)
        bottomNavigationView.labelVisibilityMode =
            NavigationBarView.LABEL_VISIBILITY_UNLABELED
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
