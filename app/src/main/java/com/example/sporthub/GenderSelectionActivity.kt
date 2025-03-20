package com.example.sporthub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.ArrayList

class GenderSelectionActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val TAG = "GenderSelectionActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gender_selection)

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Manejar botón de retroceso
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@GenderSelectionActivity,
                    "Please select your gender to continue",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        try {
            // Configurar listeners para los botones
            val buttonMale = findViewById<CardView>(R.id.button_male)
            val buttonFemale = findViewById<CardView>(R.id.button_female)
            val buttonOther = findViewById<CardView>(R.id.button_other)

            buttonMale.setOnClickListener {
                saveGenderAndProceed("Male")
            }

            buttonFemale.setOnClickListener {
                saveGenderAndProceed("Female")
            }

            buttonOther.setOnClickListener {
                saveGenderAndProceed("Other")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up gender selection: ${e.message}")
        }
    }

    private fun saveGenderAndProceed(gender: String) {
        try {
            val currentUser = mAuth.currentUser

            if (currentUser != null) {
                val userId = currentUser.uid

                // Crear un objeto con los datos del usuario
                val userData = hashMapOf(
                    "gender" to gender,
                    "bookings" to ArrayList<String>(),  // Lista vacía para bookings
                    "sports_liked" to ArrayList<String>(),  // Lista vacía para deportes preferidos
                    "venues_liked" to ArrayList<String>(),  // Lista vacía para lugares preferidos
                    "birth_date" to ""  // Fecha de nacimiento vacía por ahora
                )

                // Añadir nombre si está disponible
                currentUser.displayName?.let {
                    userData["name"] = it
                } ?: run {
                    userData["name"] = ""  // Si no hay nombre, guardar un string vacío
                }

                // Añadir email como dato adicional si está disponible
                currentUser.email?.let {
                    userData["email"] = it
                }

                // Guardar en Firestore
                db.collection("users").document(userId)
                    .set(userData)
                    .addOnSuccessListener {
                        Log.d(TAG, "User data saved successfully to Firestore")
                        Toast.makeText(this, "Gender selected successfully!", Toast.LENGTH_SHORT).show()

                        // Redirigir a la selección de fecha de nacimiento
                        val intent = Intent(this@GenderSelectionActivity, BirthDateSelectionActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving user data to Firestore: ${e.message}")
                        Toast.makeText(this, "Error saving data. Please try again", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Usuario no autenticado
                Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
                redirectToSignIn()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveGenderAndProceed: ${e.message}")
            Toast.makeText(this, "Unexpected error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun redirectToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}