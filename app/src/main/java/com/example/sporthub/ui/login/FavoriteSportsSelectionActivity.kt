package com.example.sporthub.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import android.graphics.Color
import com.example.sporthub.R
import com.example.sporthub.ui.MainActivity
import com.example.sporthub.viewmodel.SportSelectionViewModel

class FavoriteSportsSelectionActivity : AppCompatActivity() {

    private lateinit var viewModel: SportSelectionViewModel
    private val TAG = "FavoriteSportsSelectionActivity"

    // Map to track selected sports
    private val selectedSports = mutableMapOf(
        "Basketball" to false,
        "Football" to false,
        "Volleyball" to false,
        "Tennis" to false
    )

    private lateinit var basketballCard: CardView
    private lateinit var footballCard: CardView
    private lateinit var volleyballCard: CardView
    private lateinit var tennisCard: CardView
    private lateinit var discoverButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_favorite_sports_selection)

        // Inicializar el ViewModel
        viewModel = ViewModelProvider(this).get(SportSelectionViewModel::class.java)

        // Verificar autenticación
        if (!viewModel.checkAuthentication()) {
            redirectToSignIn()
            return
        }

        // Inicializar UI components
        basketballCard = findViewById(R.id.button_basketball)
        footballCard = findViewById(R.id.button_football)
        volleyballCard = findViewById(R.id.button_volleyball)
        tennisCard = findViewById(R.id.button_tennis)
        discoverButton = findViewById(R.id.button_discover)

        // Configurar observadores para eventos del ViewModel
        setupObservers()

        // Manejar botón de retroceso
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@FavoriteSportsSelectionActivity,
                    "Going back to birthdate selector!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        })

        try {
            // Configurar listeners para las tarjetas de deportes
            setupSportCardListener(basketballCard, "Basketball")
            setupSportCardListener(footballCard, "Football")
            setupSportCardListener(volleyballCard, "Volleyball")
            setupSportCardListener(tennisCard, "Tennis")

            // Configurar listener para el botón DISCOVER
            discoverButton.setOnClickListener {
                saveSportsAndProceed()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up sports selection: ${e.message}")
        }
    }

    private fun setupObservers() {
        viewModel.saveSuccessEvent.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Sports preferences saved!", Toast.LENGTH_SHORT).show()
                navigateToMainActivity()
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

    private fun setupSportCardListener(cardView: CardView, sportName: String) {
        cardView.setOnClickListener {
            // Toggle selection state
            selectedSports[sportName] = !selectedSports[sportName]!!

            // Update card appearance based on selection state
            updateCardAppearance(cardView, selectedSports[sportName]!!)
        }
    }

    private fun updateCardAppearance(cardView: CardView, isSelected: Boolean) {
        if (isSelected) {
            // Usar un fondo lavanda muy claro para indicar selección
            cardView.setCardBackgroundColor(Color.parseColor("#EDE7F6"))
            // Aumentar elevación para dar efecto "levantado"
            cardView.cardElevation = 8f
        } else {
            // Restaurar a blanco cuando no está seleccionado
            cardView.setCardBackgroundColor(Color.WHITE)
            // Restaurar elevación original
            cardView.cardElevation = 2f
        }
    }

    private fun saveSportsAndProceed() {
        // Get list of selected sports keys
        val selectedSportsKeys = selectedSports.filter { it.value }.keys.toList()

        // Delegar la lógica al ViewModel
        viewModel.saveSportsPreferences(selectedSportsKeys)
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun redirectToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}