package com.example.sporthub

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.google.firebase.Timestamp
import java.util.Date

class BirthDateSelectionActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var datePickerEditText: EditText
    private lateinit var buttonContinue: Button
    private val calendar = Calendar.getInstance()
    private val TAG = "BirthDateActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_birth_date_selection)

        // Inicializar Firebase Auth y Firestore
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Manejar el botón de retroceso
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@BirthDateSelectionActivity,
                    "Please select your birth date to continue",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // Inicializar vistas
        datePickerEditText = findViewById(R.id.date_picker_edit_text)
        buttonContinue = findViewById(R.id.button_continue)

        // Configurar el selector de fecha
        datePickerEditText.setOnClickListener {
            showDatePicker()
        }

        // Configurar el botón de continuar
        buttonContinue.setOnClickListener {
            val birthDate = datePickerEditText.text.toString()
            if (birthDate.isNotEmpty() && birthDate != "01 / 01 / 2025") {
                saveBirthDateAndProceed(birthDate)
            } else {
                Toast.makeText(this, "Please select your birth date", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            R.style.DatePickerTheme,
            { _, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Limitar la fecha máxima a la fecha actual
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        // Establecer fecha mínima (por ejemplo, 100 años atrás)
        val minDate = Calendar.getInstance()
        minDate.add(Calendar.YEAR, -100)
        datePickerDialog.datePicker.minDate = minDate.timeInMillis

        datePickerDialog.show()
    }

    private fun updateDateInView() {
        val dateFormat = SimpleDateFormat("dd / MM / yyyy", Locale.getDefault())
        datePickerEditText.setText(dateFormat.format(calendar.time))
    }

    private fun saveBirthDateAndProceed(birthDateString: String) {
        try {
            val currentUser = mAuth.currentUser ?: run {
                Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
                redirectToSignIn()
                return
            }

            val userId = currentUser.uid

            // Convertir la fecha de texto a un objeto Date
            val dateFormat = SimpleDateFormat("dd / MM / yyyy", Locale.getDefault())
            val birthDate = dateFormat.parse(birthDateString)

            // Convertir el Date a un Timestamp de Firestore
            val timestamp = birthDate?.let { com.google.firebase.Timestamp(Date(it.time)) }

            if (timestamp != null) {
                // Actualizar solo el campo birth_date con el timestamp
                db.collection("users").document(userId)
                    .update("birth_date", timestamp)
                    .addOnSuccessListener {
                        Log.d(TAG, "Birth date saved successfully as timestamp")
                        Toast.makeText(this, "Birth date updated successfully!", Toast.LENGTH_SHORT).show()

                        // Proceder a la siguiente actividad
                        val intent = Intent(this, FavoriteSportsSelectionActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving birth date: ${e.message}")
                        Toast.makeText(this, "Error saving data. Please try again", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveBirthDateAndProceed: ${e.message}")
            Toast.makeText(this, "Unexpected error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun redirectToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}