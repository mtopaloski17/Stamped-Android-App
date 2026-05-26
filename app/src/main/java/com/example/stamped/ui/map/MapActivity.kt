package com.example.stamped.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import android.widget.Toast
import com.example.stamped.ui.base.BaseActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.stamped.R
import com.example.stamped.databinding.ActivityMapBinding
import com.example.stamped.repository.CountryRepository
import com.example.stamped.ui.auth.LoginActivity
import com.example.stamped.ui.countries.CountriesActivity
import com.example.stamped.ui.countries.CountryDetailActivity
import com.example.stamped.ui.profile.ProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MapActivity : BaseActivity() {

    private lateinit var binding: ActivityMapBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var repository: CountryRepository
    private var previousVisited = emptySet<String>()
    private var previousBucket = emptySet<String>()
    private var legendInitialized = false
    private var pageReady = false
    private val pendingJs = ArrayDeque<String>()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        repository = CountryRepository(this)

        setupToolbar()
        setupWebView()
        setupBottomNav()
        populateAndSync()
        observeVisitedCountries()
        observeBucketList()
        requestNotificationPermission()
        getFCMToken()
        saveUserProfile()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_logout) {
                val user = auth.currentUser
                if (user != null && user.isAnonymous) {
                    // Чист logout за анонимни — бриши го Firebase Auth акаунтот
                    user.delete().addOnCompleteListener {
                        auth.signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                } else {
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                true
            } else false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        binding.webViewMap.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(AndroidBridge(), "Android")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(request.url)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    pageReady = true
                    // Изврши ги сите чекани JS повици
                    while (pendingJs.isNotEmpty()) {
                        val js = pendingJs.removeFirst()
                        view?.evaluateJavascript(js, null)
                    }
                }
            }
            loadUrl("https://appassets.androidplatform.net/assets/world_map.html")
        }
    }

    // Сигурно изврши JS — ако страницата не е готова, ставаме во опашка
    private fun runJs(js: String) {
        if (pageReady) {
            binding.webViewMap.post {
                binding.webViewMap.evaluateJavascript(js, null)
            }
        } else {
            pendingJs.add(js)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNav.selectedItemId = R.id.nav_map
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> true
                R.id.nav_countries -> {
                    startActivity(Intent(this, CountriesActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    true
                }
                else -> false
            }
        }
    }

    // Пополни ја базата со земји, потоа синхронизирај од Firestore
    private fun populateAndSync() {
        lifecycleScope.launch {
            repository.populateIfEmpty()
            syncFirestoreToRoom()
        }
    }

    // Парсирај Firestore вредност како Long milliseconds — Timestamp или legacy Long
    private fun parseTimestampMillis(value: Any?): Long? {
        return when (value) {
            is com.google.firebase.Timestamp -> value.toDate().time
            is Long -> value
            is Number -> value.toLong()
            else -> null
        }
    }

    // Вчитај податоци од Firestore и зачувај во Room (само за автентицирани)
    private suspend fun syncFirestoreToRoom() {
        val user = auth.currentUser ?: return
        if (user.isAnonymous) return
        val userId = user.uid

        db.collection("countries").document(userId).collection("visited").get()
            .addOnSuccessListener { documents ->
                lifecycleScope.launch {
                    for (doc in documents) {
                        val visitedAt = parseTimestampMillis(doc.get("visitedAt"))
                        repository.markVisitedFromRemote(doc.id, visitedAt ?: System.currentTimeMillis())
                    }
                }
            }

        db.collection("countries").document(userId).collection("bucketList").get()
            .addOnSuccessListener { documents ->
                lifecycleScope.launch {
                    for (doc in documents) {
                        val wantedAt = parseTimestampMillis(doc.get("wantToVisitAt"))
                        repository.markBucketFromRemote(doc.id, wantedAt ?: System.currentTimeMillis())
                    }
                }
            }

        db.collection("countries").document(userId).collection("notes").get()
            .addOnSuccessListener { documents ->
                lifecycleScope.launch {
                    for (doc in documents) {
                        val notes = doc.getString("notes") ?: ""
                        if (notes.isNotEmpty()) {
                            repository.setNotesFromRemote(doc.id, notes)
                        }
                    }
                }
            }

        db.collection("countries").document(userId).collection("cities").get()
            .addOnSuccessListener { documents ->
                lifecycleScope.launch {
                    for (doc in documents) {
                        val name = doc.getString("name") ?: continue
                        val countryCode = doc.getString("countryCode") ?: continue
                        val isCapital = doc.getBoolean("isCapital") ?: false
                        val visitedAt = parseTimestampMillis(doc.get("visitedAt"))
                            ?: System.currentTimeMillis()
                        repository.addCityFromRemote(countryCode, name, isCapital, visitedAt)
                    }
                }
            }
    }

    // Набљудувај промени во Room и ажурирај ја картата
    private fun observeVisitedCountries() {
        repository.visitedCountries.observe(this) { countries ->
            ensureLegendLabels()
            val newVisited = countries.map { it.code }.toSet()

            (newVisited - previousVisited).forEach { code ->
                runJs("markCountry('$code', true)")
            }
            (previousVisited - newVisited).forEach { code ->
                runJs("markCountry('$code', false)")
            }

            previousVisited = newVisited
            updateStats(countries.size)
        }
    }

    private fun observeBucketList() {
        repository.bucketList.observe(this) { countries ->
            val newBucket = countries.map { it.code }.toSet()

            (newBucket - previousBucket).forEach { code ->
                runJs("markBucket('$code', true)")
            }
            (previousBucket - newBucket).forEach { code ->
                runJs("markBucket('$code', false)")
            }

            previousBucket = newBucket
        }
    }

    private fun ensureLegendLabels() {
        if (legendInitialized) return
        legendInitialized = true
        val visitedLbl = getString(R.string.filter_visited).replace("'", "\\'")
        val bucketLbl = getString(R.string.filter_bucket).replace("'", "\\'")
        runJs("setLegendLabels('$visitedLbl', '$bucketLbl')")
    }

    private fun updateStats(count: Int) {
        val percentage = (count / 195.0) * 100
        binding.tvVisitedCount.text = count.toString()
        binding.tvPercentage.text = String.format(java.util.Locale.getDefault(), "%.1f%%", percentage)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
                )
            }
        }
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                val user = auth.currentUser ?: return@addOnSuccessListener
                if (user.isAnonymous) return@addOnSuccessListener
                db.collection("users")
                    .document(user.uid)
                    .set(mapOf("fcmToken" to token), SetOptions.merge())
            }
            .addOnFailureListener { it.printStackTrace() }
    }

    private fun saveUserProfile() {
        val user = auth.currentUser ?: return
        if (user.isAnonymous) return

        val loginMethod = user.providerData
            .map { it.providerId }
            .firstOrNull { it != "firebase" }
            ?.let {
                when {
                    it.contains("google") -> "google"
                    it == "password" -> "manual"
                    else -> "email"
                }
            }
            ?: "manual"

        val profile = mapOf(
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "loginMethod" to loginMethod,
            "isAnonymous" to false,
            "lastLoginAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("users")
            .document(user.uid)
            .set(profile, SetOptions.merge())
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun log(message: String) {
            android.util.Log.d("WebMap", message)
        }

        @JavascriptInterface
        fun openCountryDetail(code: String) {
            runOnUiThread {
                startActivity(
                    Intent(this@MapActivity, CountryDetailActivity::class.java).apply {
                        putExtra(CountryDetailActivity.EXTRA_CODE, code)
                    }
                )
            }
        }
    }
}
