package com.example.stamped.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.example.stamped.R
import com.example.stamped.databinding.ActivityLoginBinding
import com.example.stamped.ui.map.MapActivity

class LoginActivity : AppCompatActivity() {

    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { firebaseAuthWithGoogle(it) }
        } catch (e: ApiException) {
            showError("Google sign-in failed: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            goToMap()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {

        binding.btnEmailLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                showError(getString(R.string.error_empty_fields))
                return@setOnClickListener
            }
            showLoading(true)
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { goToMap() }
                .addOnFailureListener { e ->
                    showError(e.message ?: getString(R.string.error_login_failed))
                }
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                showError(getString(R.string.error_empty_fields))
                return@setOnClickListener
            }
            showLoading(true)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { goToMap() }
                .addOnFailureListener { e ->
                    showError(e.message ?: getString(R.string.error_login_failed))
                }
        }

        binding.btnGoogleLogin.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.btnAnonymousLogin.setOnClickListener {
            showLoading(true)
            auth.signInAnonymously()
                .addOnSuccessListener { goToMap() }
                .addOnFailureListener { e ->
                    showError(e.message ?: getString(R.string.error_login_failed))
                }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { goToMap() }
            .addOnFailureListener { e ->
                showError(e.message ?: "Google auth failed")
            }
    }

    private fun goToMap() {
        startActivity(Intent(this, MapActivity::class.java))
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnEmailLogin.isEnabled = !show
        binding.btnRegister.isEnabled = !show
        binding.btnGoogleLogin.isEnabled = !show
        binding.btnAnonymousLogin.isEnabled = !show
    }

    private fun showError(message: String) {
        showLoading(false)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}