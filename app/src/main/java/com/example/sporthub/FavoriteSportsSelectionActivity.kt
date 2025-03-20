package com.example.sporthub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.graphics.Color

class FavoriteSportsSelectionActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val TAG = "FavoriteSportsSelectionActivity"

    // Map to track selected sports with their data
    private val sportsData = mapOf(
        "Basketball" to mapOf(
            "id" to "basketball",
            "name" to "Basketball",
            "logo" to "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Fbasketball-logo.png?alt=media&token=fa52fa07-44ea-4465-b33b-cb07fa2fb228"
        ),
        "Football" to mapOf(
            "id" to "football",
            "name" to "Football",
            "logo" to "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Ffootball-logo.png?alt=media&token=3c8d8b50-b926-4a0a-8b7b-224a8e3b352c"
        ),
        "Volleyball" to mapOf(
            "id" to "volleyball",
            "name" to "Volleyball",
            "logo" to "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Fvolleyball-logo.png?alt=media&token=b51de9d4-f1b4-4ede-a3a0-5777523b2cb9"
        ),
        "Tennis" to mapOf(
            "id" to "tennis",
            "name" to "Tennis",
            "logo" to "https://firebasestorage.googleapis.com/v0/b/moviles-isis3510.firebasestorage.app/o/icons%2Fsports%2Ftennis-logo.png?alt=media&token=84fde031-9c77-4cc5-b4d3-dd785e203b99"
        )
    )
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
        setContentView(R.layout.activity_favorite_sports_selection)

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inicializar UI components
        basketballCard = findViewById(R.id.button_basketball)
        footballCard = findViewById(R.id.button_football)
        volleyballCard = findViewById(R.id.button_volleyball)
        tennisCard = findViewById(R.id.button_tennis)
        discoverButton = findViewById(R.id.button_discover)

        // Manejar botón de retroceso
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@FavoriteSportsSelectionActivity,
                    "Please select at least one sport to continue",
                    Toast.LENGTH_SHORT
                ).show()
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
        try {
            // Get list of selected sports keys
            val selectedSportsKeys = selectedSports.filter { it.value }.keys.toList()

            // Check if at least one sport is selected
            if (selectedSportsKeys.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please select at least one sport to continue",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            // Create list of sports data objects for selected sports
            val sportsList = selectedSportsKeys.map { sportName ->
                sportsData[sportName]
            }

            val currentUser = mAuth.currentUser

            if (currentUser != null) {
                val userId = currentUser.uid

                // Primero obtenemos el documento actual del usuario para verificar los datos existentes
                db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            // Obtenemos la lista actual de deportes (si existe)
                            val existingSports = document.get("sports_liked") as? ArrayList<String> ?: ArrayList()

                            // Limpiamos la lista actual y añadimos los deportes seleccionados
                            // Esto es equivalente a reemplazar la lista completa
                            db.collection("users").document(userId)
                                .update("sports_liked", sportsList)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Sports preferences saved successfully to Firestore")
                                    Toast.makeText(this, "Sports preferences saved!", Toast.LENGTH_SHORT).show()

                                    // Redirect to the main activity or next screen
                                    val intent = Intent(this@FavoriteSportsSelectionActivity, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error saving sports preferences to Firestore: ${e.message}")
                                    Toast.makeText(this, "Error saving data. Please try again", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Log.e(TAG, "User document does not exist")
                            Toast.makeText(this, "Error: User profile not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error fetching user document: ${e.message}")
                        Toast.makeText(this, "Error retrieving user data. Please try again", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // User not authenticated
                Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
                redirectToSignIn()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveSportsAndProceed: ${e.message}")
            Toast.makeText(this, "Unexpected error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun redirectToSignIn() {
        val intent = Intent(this@FavoriteSportsSelectionActivity, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}