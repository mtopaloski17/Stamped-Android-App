package com.example.stamped.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.stamped.R
import com.example.stamped.databinding.ActivityRegisterBinding
import com.example.stamped.ui.base.BaseActivity
import com.example.stamped.ui.map.MapActivity
import com.example.stamped.util.LocaleHelper
import com.example.stamped.util.UserProfileHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterActivity : BaseActivity() {

    private var _binding: ActivityRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.btnBack.setOnClickListener { finish() }
        setupLanguageToggle()
    }

    private fun setupLanguageToggle() {
        updateLanguageLabel()
        binding.tvLanguageToggle.setOnClickListener {
            val current = LocaleHelper.getSavedLanguage(this)
            val newLang = if (current == "mk") "en" else "mk"
            LocaleHelper.saveLanguage(this, newLang)
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    private fun updateLanguageLabel() {
        val lang = LocaleHelper.getSavedLanguage(this)
        binding.tvLanguageToggle.text = if (lang == "mk") "🌐 МК" else "🌐 EN"
    }

    private fun attemptRegister() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.register_error_name_empty)
            return
        }
        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_empty_fields))
            return
        }
        if (password.length < 6) {
            binding.etPassword.error = getString(R.string.register_error_password_short)
            return
        }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = getString(R.string.register_error_passwords_mismatch)
            return
        }

        showLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                val request = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user?.updateProfile(request)
                    ?.addOnCompleteListener {
                        // Зачувај име локално за моментален приказ
                        UserProfileHelper.setDisplayName(this, name)
                        Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show()
                        goToMap()
                    } ?: goToMap()
            }
            .addOnFailureListener { e ->
                showError(e.message ?: getString(R.string.error_login_failed))
            }
    }

    private fun goToMap() {
        startActivity(
            Intent(this, MapActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !show
        binding.btnBack.isEnabled = !show
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
