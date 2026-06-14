package ru.github.debitcredit

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.MaterialToolbar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {

        // Загружаем настройки перед super.onCreate
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)

        // Применяем тему ДО создания активности
        val isDarkTheme = sharedPref.getBoolean("theme", false)
        if (isDarkTheme) {
            setTheme(R.style.Theme_DebitCredit_Night)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            setTheme(R.style.Theme_DebitCredit)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Применяем язык
        updateLocale(sharedPref)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostId = R.id.nav_host_fragment
        Log.d("MainActivity", "nav_host_fragment ID: $navHostId")

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setHomeButtonEnabled(false)

        supportFragmentManager.executePendingTransactions()

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Обновляем язык при изменении конфигурации
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        updateLocale(sharedPref)
    }

    private fun updateLocale(sharedPref: SharedPreferences) {
        val savedLanguage = sharedPref.getString("settings_language", "system") ?: "system"
        val languageCode = when (savedLanguage) {
            "ru" -> "ru"
            "en" -> "en"
            else -> null
        }

        if (languageCode != null && languageCode != Locale.getDefault().language) {
            setAppLocale(languageCode)
        }
    }

    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}