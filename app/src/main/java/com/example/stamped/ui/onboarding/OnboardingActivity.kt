package com.example.stamped.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.stamped.R
import com.example.stamped.databinding.ActivityOnboardingBinding
import com.example.stamped.ui.auth.LoginActivity
import com.example.stamped.ui.base.BaseActivity
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : BaseActivity() {

    companion object {
        private const val PREFS = "stamped_settings"
        private const val KEY_SHOWN = "onboarding_shown"

        fun isShown(context: Context): Boolean {
            return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOWN, false)
        }

        fun markShown(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SHOWN, true).apply()
        }
    }

    private lateinit var binding: ActivityOnboardingBinding

    private val pages by lazy {
        listOf(
            OnboardingPage("🌍", getString(R.string.onb_title_1), getString(R.string.onb_desc_1)),
            OnboardingPage("⭐", getString(R.string.onb_title_2), getString(R.string.onb_desc_2)),
            OnboardingPage("🏆", getString(R.string.onb_title_3), getString(R.string.onb_desc_3))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = OnboardingAdapter(pages)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabIndicator, binding.viewPager) { _, _ -> }.attach()

        binding.btnSkip.setOnClickListener { finishOnboarding() }
        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < pages.size - 1) {
                binding.viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.btnNext.text = if (position == pages.size - 1) {
                    getString(R.string.onboarding_start)
                } else {
                    getString(R.string.onboarding_next)
                }
            }
        })
    }

    private fun finishOnboarding() {
        markShown(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

data class OnboardingPage(val emoji: String, val title: String, val description: String)
