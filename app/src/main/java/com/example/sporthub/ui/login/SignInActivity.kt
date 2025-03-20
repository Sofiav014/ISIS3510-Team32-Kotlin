package com.example.sporthub.ui.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.example.sporthub.R
import com.example.sporthub.ui.home.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "SignInActivity"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser

        if (currentUser != null) {
            // El usuario ya inició sesión, verificar si necesita seleccionar género
            checkUserExistsInFirestore(currentUser.uid)
        }

        val signInButton = findViewById<Button>(R.id.signInButton)
        signInButton.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Signed in as ${user?.displayName}", Toast.LENGTH_SHORT).show()

                    // Verificar si el usuario necesita seleccionar género
                    if (user != null) {
                        checkUserExistsInFirestore(user.uid)
                    }
                } else {
                    Log.e(TAG, "Authentication failed", task.exception)
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkUserExistsInFirestore(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.contains("gender") && document.contains("name") &&
                    document.contains("birth_date") && document.contains("sports_liked")) {
                    // Usuario existente con todos los datos registrados - ir a MainActivity
                    navigateToMainActivity()
                } else {
                    // Usuario nuevo o con datos incompletos - iniciar flujo de registro
                    navigateToNameSelectionActivity()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking user data: ${e.message}")
                // Por defecto, ir a selección de nombre por seguridad
                navigateToNameSelectionActivity()
            }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Cambiamos esta función para navegar a NameSelectionActivity
    private fun navigateToNameSelectionActivity() {
        val intent = Intent(this, NameSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Mantenemos esta función por si necesitamos usarla en algún momento
    private fun navigateToGenderSelectionActivity() {
        val intent = Intent(this, GenderSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }
}