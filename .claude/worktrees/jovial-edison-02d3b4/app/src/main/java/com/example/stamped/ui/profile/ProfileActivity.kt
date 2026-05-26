package com.example.stamped.ui.profile

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.stamped.databinding.ActivityProfileBinding
import com.example.stamped.repository.CountryRepository
import com.example.stamped.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var repository: CountryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        repository = CountryRepository(this)

        setupUserInfo()
        setupStats()
        setupLanguage()
        setupLogout()
    }

    private fun setupUserInfo() {
        val user = auth.currentUser
        binding.tvUserEmail.text = when {
            user == null -> getString(com.example.stamped.R.string.anonymous_user)
            user.isAnonymous -> getString(com.example.stamped.R.string.anonymous_user)
            else -> user.email ?: user.displayName ?: getString(com.example.stamped.R.string.anonymous_user)
        }
    }

    private fun setupStats() {
        repository.visitedCountries.observe(this) { countries ->
            val count = countries.size
            val percentage = (count / 195.0 * 100).toInt()
            val continents = countries.map { it.continent }.toSet().size

            binding.tvStatVisited.text = count.toString()
            binding.tvStatPercentage.text = "$percentage%"
            binding.tvStatContinents.text = continents.toString()
        }
    }

    private fun setupLanguage() {
        // Провери моментален јазик
        val currentLang = resources.configuration.locales[0].language
        if (currentLang == "mk") {
            binding.rgLanguage.check(binding.rbMacedonian.id)
        } else {
            binding.rgLanguage.check(binding.rbEnglish.id)
        }

        binding.rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            val langCode = if (checkedId == binding.rbMacedonian.id) "mk" else "en"
            setAppLanguage(langCode)
        }
    }

    private fun setAppLanguage(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Рестартирај ја апликацијата за да се примени јазикот
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }
}