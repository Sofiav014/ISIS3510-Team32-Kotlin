package com.example.sporthub

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class NameSelectionActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var nameEditText: TextInputEditText
    private lateinit var continueButton: Button
    private val TAG = "NameSelectionActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_name_selection)

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inicializar vistas
        nameEditText = findViewById(R.id.edit_text_name)
        continueButton = findViewById(R.id.button_continue)

        // Rellenar el campo con el nombre de Google si está disponible
        val currentUser = mAuth.currentUser
        currentUser?.displayName?.let {
            if (it.isNotEmpty()) {
                nameEditText.setText(it)
            }
        }

        // Manejar botón de retroceso
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@NameSelectionActivity,
                    "Please enter your name to continue",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // Configurar el botón continuar
        continueButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveNameAndProceed(name)
        }
    }

    private fun saveNameAndProceed(name: String) {
        try {
            val currentUser = mAuth.currentUser

            if (currentUser != null) {
                val userId = currentUser.uid

                // Actualizar el perfil de Firebase Authentication
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                currentUser.updateProfile(profileUpdates)
                    .addOnSuccessListener {
                        Log.d(TAG, "User profile updated with new name")

                        // Verificar si el documento existe antes de intentar actualizarlo
                        db.collection("users").document(userId)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    // El documento existe, actualizamos solo el nombre
                                    db.collection("users").document(userId)
                                        .update("name", name)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "User name updated in Firestore")
                                            Toast.makeText(this, "Name saved successfully!", Toast.LENGTH_SHORT).show()
                                            proceedToNextScreen()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Error updating user name in Firestore: ${e.message}")
                                            Toast.makeText(this, "Error saving data. Please try again", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    // El documento no existe, lo creamos
                                    val userData = hashMapOf(
                                        "name" to name,
                                        "email" to currentUser.email,
                                        "bookings" to ArrayList<String>(),
                                        "sports_liked" to ArrayList<String>(),
                                        "venues_liked" to ArrayList<String>(),
                                        "birth_date" to "",
                                        "gender" to ""
                                    )

                                    db.collection("users").document(userId)
                                        .set(userData)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "User document created in Firestore")
                                            Toast.makeText(this, "Profile created successfully!", Toast.LENGTH_SHORT).show()
                                            proceedToNextScreen()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Error creating user document: ${e.message}")
                                            Toast.makeText(this, "Error creating profile. Please try again", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error checking if user document exists: ${e.message}")
                                Toast.makeText(this, "Error checking profile. Please try again", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating user profile: ${e.message}")
                        Toast.makeText(this, "Error updating profile. Please try again", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Usuario no autenticado
                Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
                redirectToSignIn()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveNameAndProceed: ${e.message}")
            Toast.makeText(this, "Unexpected error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun proceedToNextScreen() {
        // Redirigir a la selección de género
        val intent = Intent(this@NameSelectionActivity, GenderSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun redirectToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}