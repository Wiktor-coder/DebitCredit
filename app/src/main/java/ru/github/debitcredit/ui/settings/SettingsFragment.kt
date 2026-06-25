package ru.github.debitcredit.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import ru.github.debitcredit.R
import ru.github.debitcredit.utils.CurrencyService
import ru.github.debitcredit.utils.TimeZoneHelper
import java.util.*

class SettingsFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var currencySpinner: Spinner
    private lateinit var themeSwitch: SwitchCompat
    private lateinit var languageSpinner: Spinner
    private lateinit var applyButton: MaterialButton
    private lateinit var backButton: ImageButton
    private lateinit var currencyRateTextView: TextView
    private lateinit var timezoneSpinner: Spinner

    private var exchangeRates: Map<String, Double>? = null

    companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_CURRENCY = "currency"
        const val KEY_THEME = "theme"
        const val KEY_LANGUAGE = "language"
        const val DEFAULT_LANGUAGE = "ru" // Русский по умолчанию
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        currencySpinner = view.findViewById(R.id.currencySpinner)
        themeSwitch = view.findViewById(R.id.themeSwitch)
        languageSpinner = view.findViewById(R.id.languageSpinner)
        applyButton = view.findViewById(R.id.applyButton)
        backButton = view.findViewById(R.id.backButton)
        currencyRateTextView = view.findViewById(R.id.currencyRateTextView)
        timezoneSpinner = view.findViewById(R.id.timezoneSpinner)

        setupBackButton()
        setupCurrencySpinner()
        setupLanguageSpinner()
        setupTimezoneSpinner()
        loadSavedSettings()
        loadExchangeRates()

        applyButton.setOnClickListener {
            saveSettings()
            applySettings()
        }
    }

    private fun setupTimezoneSpinner() {
        val timezones = listOf(
            "UTC-12", "UTC-11", "UTC-10", "UTC-9", "UTC-8", "UTC-7", "UTC-6", "UTC-5",
            "UTC-4", "UTC-3", "UTC-2", "UTC-1", "UTC+0", "UTC+1", "UTC+2", "UTC+3",
            "UTC+4", "UTC+5", "UTC+6", "UTC+7", "UTC+8", "UTC+9", "UTC+10", "UTC+11", "UTC+12"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timezones)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timezoneSpinner.adapter = adapter

        val currentOffset = TimeZoneHelper.getTimeZoneOffset(requireContext())
        val defaultIndex = timezones.indexOf("UTC+$currentOffset")
        timezoneSpinner.setSelection(if (defaultIndex >= 0) defaultIndex else 12)
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupCurrencySpinner() {
        val currencies = listOf("RUB", "USD", "EUR", "CNY", "GBP", "JPY")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencySpinner.adapter = adapter

        currencySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCurrency = currencies[position]
                updateCurrencyRateDisplay(selectedCurrency)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun loadExchangeRates() {
        lifecycleScope.launch {
            exchangeRates = CurrencyService.getExchangeRates()
            val selectedCurrency = currencySpinner.selectedItem.toString()
            updateCurrencyRateDisplay(selectedCurrency)
        }
    }

    private fun updateCurrencyRateDisplay(currencyCode: String) {
        val rate = exchangeRates?.get(currencyCode)
        currencyRateTextView.text = when {
            rate != null && currencyCode != "RUB" -> "1 $currencyCode = ${String.format("%.4f", rate)} ₽"
            currencyCode == "RUB" -> getString(R.string.base_currency)
            else -> getString(R.string.rate_not_available)
        }
    }

    // только русский и английский
    private fun setupLanguageSpinner() {
        val languages = listOf(
            getString(R.string.russian),
            getString(R.string.english)
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter
    }

    private fun loadSavedSettings() {
        val savedCurrency = sharedPreferences.getString(KEY_CURRENCY, "RUB") ?: "RUB"
        val currencies = listOf("RUB", "USD", "EUR", "CNY", "GBP", "JPY")
        currencySpinner.setSelection(currencies.indexOf(savedCurrency))

        val isDarkTheme = sharedPreferences.getBoolean(KEY_THEME, false)
        themeSwitch.isChecked = isDarkTheme

        // Загружаем сохраненный язык, по умолчанию русский
        val savedLanguage = sharedPreferences.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        val languages = listOf("ru", "en")
        languageSpinner.setSelection(languages.indexOf(savedLanguage).coerceAtLeast(0))
    }

    private fun saveSettings() {
        val selectedCurrency = currencySpinner.selectedItem.toString()
        val isDarkTheme = themeSwitch.isChecked
        // Сохраняем язык: 0 - русский, 1 - английский
        val selectedLanguage = when (languageSpinner.selectedItemPosition) {
            0 -> "ru"
            1 -> "en"
            else -> "ru"
        }

        sharedPreferences.edit {
            putString(KEY_CURRENCY, selectedCurrency)
            putBoolean(KEY_THEME, isDarkTheme)
            putString(KEY_LANGUAGE, selectedLanguage)
        }

        val selectedTimezone = timezoneSpinner.selectedItem.toString()
        val offset = selectedTimezone.replace("UTC", "").toIntOrNull() ?: 0
        TimeZoneHelper.saveTimeZoneOffset(requireContext(), offset)
    }

    private fun applySettings() {
        saveSettings()

        val isDarkTheme = themeSwitch.isChecked
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Получаем выбранный язык: 0 - русский, 1 - английский
        val selectedLanguage = when (languageSpinner.selectedItemPosition) {
            0 -> "ru"
            1 -> "en"
            else -> "ru"
        }

        val locale = Locale(selectedLanguage)
        Log.d("SettingsFragment", "Selected language: $selectedLanguage, Locale: ${locale.language}")

        // Устанавливаем локаль
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        Toast.makeText(requireContext(), getString(R.string.settings_applied), Toast.LENGTH_LONG).show()
        requireActivity().recreate()
    }
}