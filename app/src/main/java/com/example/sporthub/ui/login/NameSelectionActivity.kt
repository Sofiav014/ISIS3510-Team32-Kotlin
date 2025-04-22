package com.example.sporthub.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.sporthub.R
import com.example.sporthub.viewmodel.NameSelectionViewModel
import com.google.android.material.textfield.TextInputEditText

class NameSelectionActivity : AppCompatActivity() {

    private lateinit var viewModel: NameSelectionViewModel
    private lateinit var nameEditText: TextInputEditText
    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_name_selection)

        // Inicializar el ViewModel
        viewModel = ViewModelProvider(this).get(NameSelectionViewModel::class.java)

        // Verificar autenticación
        if (!viewModel.checkAuthentication()) {
            redirectToSignIn()
            return
        }

        // Inicializar vistas
        nameEditText = findViewById(R.id.edit_text_name)
        continueButton = findViewById(R.id.button_continue)

        // Configurar filtro para solo permitir letras
        setupLettersOnlyFilter()

        // Configurar observadores para eventos del ViewModel
        setupObservers()

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

        // Configurar el botón de continuar
        continueButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveName(name)
        }
    }

    private fun setupLettersOnlyFilter() {
        // Filtro para permitir solo letras, espacios y algunos caracteres especiales para nombres compuestos
        val lettersFilter = InputFilter { source, start, end, dest, dstart, dend ->
            val regex = Regex("^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ '-]+$")
            for (i in start until end) {
                if (!regex.matches(source[i].toString())) {
                    // Si no coincide con el patrón, no permitir la entrada
                    return@InputFilter ""
                }
            }
            null // Permitir la entrada
        }

        // Filtro de longitud - limita a 30 caracteres
        val lengthFilter = InputFilter.LengthFilter(35)

        // Aplicar ambos filtros
        nameEditText.filters = arrayOf(lettersFilter, lengthFilter)

        // Opcional: Mostrar contador de caracteres
        // Primero obtener la referencia al TextInputLayout
        val nameInputLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.name_input_layout)

        nameEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                val length = s?.length ?: 0
                if (length >= 25) { // Aviso cuando se acerca al límite
                    val remaining = 30 - length
                    nameInputLayout.helperText = "$remaining characters left"
                } else {
                    nameInputLayout.helperText = null
                }
            }
        })
    }

    private fun setupObservers() {
        viewModel.saveSuccessEvent.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Name saved successfully!", Toast.LENGTH_SHORT).show()
                navigateToGenderSelection()
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

    private fun navigateToGenderSelection() {
        val intent = Intent(this, GenderSelectionActivity::class.java)
        startActivity(intent)
    }

    private fun redirectToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
}