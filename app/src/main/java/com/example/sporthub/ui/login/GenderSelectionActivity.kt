package com.example.sporthub.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import com.example.sporthub.R
import com.example.sporthub.viewmodel.GenderSelectionViewModel

class GenderSelectionActivity : AppCompatActivity() {

    private lateinit var viewModel: GenderSelectionViewModel
    private val TAG = "GenderSelectionActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_gender_selection)

        // Inicializar el ViewModel
        viewModel = ViewModelProvider(this).get(GenderSelectionViewModel::class.java)

        // Verificar autenticación
        if (!viewModel.checkAuthentication()) {
            redirectToSignIn()
            return
        }

        // Configurar observadores para eventos del ViewModel
        setupObservers()

        // Manejar botón de retroceso del dispositivo
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

        // Manejar botón de retroceso nuevo
        val backButton = findViewById<ImageButton>(R.id.button_back_gender)
        backButton.setOnClickListener {
            finish()
        }

        try {
            // Configurar listeners para los botones
            val buttonMale = findViewById<CardView>(R.id.button_male)
            val buttonFemale = findViewById<CardView>(R.id.button_female)
            val buttonOther = findViewById<CardView>(R.id.button_other)

            buttonMale.setOnClickListener {
                viewModel.saveGender("Male")
            }

            buttonFemale.setOnClickListener {
                viewModel.saveGender("Female")
            }

            buttonOther.setOnClickListener {
                viewModel.saveGender("Other")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up gender selection: ${e.message}")
        }
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
    }

    private fun redirectToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }

}