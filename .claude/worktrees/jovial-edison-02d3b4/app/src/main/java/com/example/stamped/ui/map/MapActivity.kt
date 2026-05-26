package com.example.stamped.ui.map

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.stamped.R
import com.example.stamped.databinding.ActivityMapBinding

import com.example.stamped.ui.auth.LoginActivity
import com.example.stamped.ui.countries.CountriesActivity
import com.example.stamped.ui.profile.ProfileActivity
//manifest
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val visitedCountries = mutableSetOf<String>()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupToolbar()
        setupWebView()
        setupBottomNav()
        loadVisitedCountries()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_logout) {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            } else false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webViewMap.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(AndroidBridge(), "Android")
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/world_map.html")
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> true
                R.id.nav_countries -> {
                    startActivity(Intent(this, CountriesActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadVisitedCountries() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("countries")
            .document(userId)
            .collection("visited")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    visitedCountries.add(doc.id)
                    binding.webViewMap.post {
                        binding.webViewMap.evaluateJavascript(
                            "markCountry('${doc.id}', true)", null
                        )
                    }
                }
                updateStats()
            }
    }

    private fun updateStats() {
        val count = visitedCountries.size
        val percentage = (count / 195.0) * 100
        binding.tvVisitedCount.text = getString(R.string.visited_countries, count)
        binding.tvPercentage.text = getString(R.string.world_percentage, percentage)
    }

    private fun saveCountryToFirestore(code: String, name: String, visited: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val ref = db.collection("countries")
            .document(userId)
            .collection("visited")
            .document(code)
        if (visited) {
            ref.set(mapOf("name" to name, "code" to code, "visitedAt" to System.currentTimeMillis()))
            visitedCountries.add(code)
        } else {
            ref.delete()
            visitedCountries.remove(code)
        }
        runOnUiThread { updateStats() }
    }

    // JavaScript Bridge — поврзување меѓу HTML и Android
    inner class AndroidBridge {
        @JavascriptInterface
        fun onCountryClicked(code: String, name: String, visited: Boolean) {
            runOnUiThread {
                val message = if (visited)
                    "✅ $name added to your visited countries!"
                else
                    "❌ $name removed from your list"
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                saveCountryToFirestore(code, name, visited)
            }
        }
    }
}