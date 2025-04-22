package com.example.sporthub.ui.login

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.sporthub.R
import com.example.sporthub.viewmodel.BirthDateViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BirthDateSelectionActivity : AppCompatActivity() {

    private lateinit var viewModel: BirthDateViewModel
    private lateinit var datePickerEditText: EditText
    private lateinit var buttonContinue: Button
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_birth_date_selection)

        // Inicializar el ViewModel
        viewModel = ViewModelProvider(this).get(BirthDateViewModel::class.java)

        // Verificar autenticación
        if (!viewModel.checkAuthentication()) {
            redirectToSignIn()
            return
        }

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

        // Configurar observadores para eventos del ViewModel
        setupObservers()

        // Configurar el selector de fecha
        datePickerEditText.setOnClickListener {
            showDatePicker()
        }

        // Configurar el botón de continuar
        buttonContinue.setOnClickListener {
            val birthDate = datePickerEditText.text.toString()
            if (birthDate.isNotEmpty() && birthDate != "01 / 01 / 2025") {
                saveBirthDate(birthDate)
            } else {
                Toast.makeText(this, "Please select your birth date", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        viewModel.saveSuccessEvent.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Birth date updated successfully!", Toast.LENGTH_SHORT).show()
                navigateToSportsSelection()
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

        // Establecer fecha máxima (Requerimiento de edad - Los usuarios deben tener, por lo menos, 14)
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.YEAR, -14)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

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

    private fun saveBirthDate(birthDateString: String) {
        try {
            val dateFormat = SimpleDateFormat("dd / MM / yyyy", Locale.getDefault())
            val birthDate = dateFormat.parse(birthDateString)

            if (birthDate != null) {
                viewModel.saveBirthDate(birthDate)
            } else {
                Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unexpected error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToSportsSelection() {
        val intent = Intent(this, FavoriteSportsSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun redirectToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}