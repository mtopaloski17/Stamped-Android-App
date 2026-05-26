package com.example.stamped.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.example.stamped.R
import com.example.stamped.databinding.ActivityProfileBinding
import com.example.stamped.repository.CountryRepository
import com.example.stamped.ui.auth.LoginActivity
import com.example.stamped.ui.base.BaseActivity
import com.example.stamped.ui.countries.CountriesActivity
import com.example.stamped.ui.map.MapActivity
import com.example.stamped.data.model.City
import com.example.stamped.data.model.Country
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.stamped.util.Achievement
import com.example.stamped.util.Achievements
import com.example.stamped.util.DailyTipWorker
import com.example.stamped.util.LocaleHelper
import com.example.stamped.util.ThemeHelper
import com.example.stamped.util.UserProfileHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class ProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var repository: CountryRepository
    private var currentAchievements: List<Achievement> = emptyList()
    private var lastVisited: List<Country> = emptyList()
    private var lastCities: List<City> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        repository = CountryRepository(this)

        setupUserName()
        setupStats()
        setupLanguage()
        setupTheme()
        setupNotifications()
        setupLogout()
        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_profile
    }

    private fun setupUserName() {
        val user = auth.currentUser
        val savedName = UserProfileHelper.getDisplayName(this)

        val displayName = when {
            savedName.isNotBlank() -> savedName
            user == null || user.isAnonymous -> getString(R.string.anonymous_user)
            !user.displayName.isNullOrBlank() -> user.displayName!!
            !user.email.isNullOrBlank() -> {
                val derived = UserProfileHelper.deriveFromEmail(user.email!!)
                UserProfileHelper.setDisplayName(this, derived)
                derived
            }
            else -> getString(R.string.anonymous_user)
        }

        binding.tvUserName.text = displayName

        // Промена на име — само за неанонимни
        if (user != null && !user.isAnonymous) {
            binding.tvUserName.setOnClickListener { showNameEditDialog() }
        }

        // Покажи notice ако корисникот е анонимен (само portrait)
        try {
            val noticeCard = binding.root.findViewById<android.view.View>(R.id.cardAnonymousNotice)
            noticeCard?.visibility = if (user?.isAnonymous == true) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        } catch (_: Exception) {}

        // Поставување титула (од достигнување или почетник)
        updateTitle()
        binding.tvUserTitle.setOnClickListener { showTitleSelector() }
    }

    private fun showNameEditDialog() {
        val editText = EditText(this).apply {
            setText(binding.tvUserName.text)
            setSelection(text.length)
            setHint(R.string.edit_name_hint)
        }
        val container = android.widget.FrameLayout(this).apply {
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
            addView(editText)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_name_title)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = editText.text?.toString()?.trim().orEmpty()
                if (newName.isNotEmpty()) {
                    UserProfileHelper.setDisplayName(this, newName)
                    binding.tvUserName.text = newName
                    syncNameToFirebase(newName)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun syncNameToFirebase(name: String) {
        val user = auth.currentUser ?: return
        if (user.isAnonymous) return

        // Firebase Auth profile (displayName)
        val request = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        user.updateProfile(request)

        // Firestore users колекција
        db.collection("users").document(user.uid)
            .update("displayName", name)
            .addOnFailureListener { e -> e.printStackTrace() }
    }

    private fun showTitleSelector() {
        val lang = resources.configuration.locales[0].language
        val unlocked = currentAchievements.filter { it.unlocked }

        if (unlocked.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.choose_title)
                .setMessage(R.string.title_locked_hint)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        val beginner = getString(R.string.title_beginner)
        val items = mutableListOf("🌱 $beginner")
        items.addAll(unlocked.map { "${it.emoji} ${it.title(lang)}" })

        AlertDialog.Builder(this)
            .setTitle(R.string.choose_title)
            .setItems(items.toTypedArray()) { _, which ->
                if (which == 0) {
                    UserProfileHelper.setTitleAchievementId(this, null)
                } else {
                    UserProfileHelper.setTitleAchievementId(this, unlocked[which - 1].id)
                }
                updateTitle()
            }
            .show()
    }

    private fun updateTitle() {
        val lang = resources.configuration.locales[0].language
        val savedId = UserProfileHelper.getTitleAchievementId(this)
        val unlocked = currentAchievements.filter { it.unlocked }

        val achievement = if (savedId != null) {
            unlocked.find { it.id == savedId }
        } else null

        val title = achievement?.title(lang) ?: getString(R.string.title_beginner)
        // Стандардна боја за „Beginner" — мека ментолна; останатите ја носат
        // својата уникатна боја од Achievement.colorHex
        val colorHex = achievement?.colorHex ?: "#A7F3D0"

        binding.tvUserTitle.text = title
        try {
            binding.tvUserTitle.setTextColor(android.graphics.Color.parseColor(colorHex))
        } catch (_: IllegalArgumentException) {
            binding.tvUserTitle.setTextColor(android.graphics.Color.WHITE)
        }
    }

    private fun setupStats() {
        binding.recyclerAchievements.layoutManager = GridLayoutManager(this, 3)

        repository.visitedCountries.observe(this) { countries ->
            lastVisited = countries
            recomputeStats()
        }
        repository.allCities.observe(this) { cities ->
            lastCities = cities
            recomputeStats()
        }
    }

    private fun recomputeStats() {
        val count = lastVisited.size
        val percentage = (count / 195.0 * 100).toInt()
        val continents = lastVisited.map { it.continent }.toSet().size

        binding.tvStatVisited.text = count.toString()
        binding.tvStatPercentage.text = "$percentage%"
        binding.tvStatContinents.text = continents.toString()
        binding.tvStatCities.text = lastCities.size.toString()

        val achievements = Achievements.evaluate(lastVisited, lastCities)
        currentAchievements = achievements
        val unlocked = achievements.count { it.unlocked }
        val lang = resources.configuration.locales[0].language
        binding.tvAchievementsProgress.text = getString(
            R.string.achievements_progress, unlocked, achievements.size
        )
        binding.recyclerAchievements.adapter = AchievementAdapter(achievements, lang)

        updateTitle()
    }

    private fun setupLanguage() {
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
        LocaleHelper.saveLanguage(this, langCode)
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun setupTheme() {
        when (ThemeHelper.getSavedTheme(this)) {
            ThemeHelper.MODE_LIGHT -> binding.rgTheme.check(binding.rbThemeLight.id)
            ThemeHelper.MODE_DARK -> binding.rgTheme.check(binding.rbThemeDark.id)
            else -> binding.rgTheme.check(binding.rbThemeSystem.id)
        }
        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                binding.rbThemeLight.id -> ThemeHelper.MODE_LIGHT
                binding.rbThemeDark.id -> ThemeHelper.MODE_DARK
                else -> ThemeHelper.MODE_SYSTEM
            }
            ThemeHelper.saveTheme(this, mode)
        }
    }

    private fun setupNotifications() {
        binding.switchNotifications.isChecked = DailyTipWorker.isEnabled(this)
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            DailyTipWorker.setEnabled(this, isChecked)
        }
        binding.btnTestNotification.setOnClickListener {
            sendTestNotification()
        }
    }

    private fun sendTestNotification() {
        // Провери дозвола за Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIF_PERMISSION
                )
                return
            }
        }
        DailyTipWorker.sendTestNotification(this)
        Toast.makeText(this, R.string.notif_sent, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIF_PERMISSION) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                DailyTipWorker.sendTestNotification(this)
                Toast.makeText(this, R.string.notif_sent, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.notif_permission_needed, Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val REQUEST_NOTIF_PERMISSION = 201
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            val user = auth.currentUser
            // За анонимни корисници — целосно избриши го Firebase Auth акаунтот.
            // Локалните Room податоци ќе се изгубат бидејќи новиот аноним. UID
            // ќе биде различен од стариот → нова чиста сесија.
            if (user != null && user.isAnonymous) {
                user.delete().addOnCompleteListener {
                    auth.signOut()
                    goBackToLogin()
                }
            } else {
                auth.signOut()
                goBackToLogin()
            }
        }
    }

    private fun goBackToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> {
                    startActivity(
                        Intent(this, MapActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                    finish()
                    true
                }
                R.id.nav_countries -> {
                    startActivity(
                        Intent(this, CountriesActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                    finish()
                    true
                }
                R.id.nav_profile -> true
                else -> false
            }
        }
    }
}
