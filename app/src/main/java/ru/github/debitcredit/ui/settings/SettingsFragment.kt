package ru.github.debitcredit.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
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
import java.util.Locale

class SettingsFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var currencySpinner: Spinner
    private lateinit var themeSwitch: SwitchCompat
    private lateinit var languageSpinner: Spinner
    private lateinit var applyButton: MaterialButton
    private lateinit var backButton: ImageButton
    private lateinit var currencyRateTextView: TextView

    private var exchangeRates: Map<String, Double>? = null

    companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_CURRENCY = "currency"
        const val KEY_THEME = "theme"
        const val KEY_LANGUAGE = "language"
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

        setupBackButton()
        setupCurrencySpinner()
        setupLanguageSpinner()
        loadSavedSettings()
        loadExchangeRates()

        applyButton.setOnClickListener {
            saveSettings()
            applySettings()
        }
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

    private fun setupLanguageSpinner() {
        val languages = listOf(
            getString(R.string.system_language),
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

        val savedLanguage = sharedPreferences.getString(KEY_LANGUAGE, "system") ?: "system"
        val languages = listOf("system", "ru", "en")
        languageSpinner.setSelection(languages.indexOf(savedLanguage))
    }

    private fun saveSettings() {
        val selectedCurrency = currencySpinner.selectedItem.toString()
        val isDarkTheme = themeSwitch.isChecked
        val selectedLanguage = when (languageSpinner.selectedItemPosition) {
            0 -> "system"
            1 -> "ru"
            2 -> "en"
            else -> "system"
        }

        sharedPreferences.edit {
            putString(KEY_CURRENCY, selectedCurrency)
            putBoolean(KEY_THEME, isDarkTheme)
            putString(KEY_LANGUAGE, selectedLanguage)
        }
    }

    private fun applySettings() {
        saveSettings()

        val isDarkTheme = themeSwitch.isChecked
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        val languageCode = when (languageSpinner.selectedItemPosition) {
            1 -> "ru"
            2 -> "en"
            else -> null
        }

        if (languageCode != null) {
            setAppLocale(languageCode)
        }

        Toast.makeText(requireContext(), getString(R.string.settings_applied), Toast.LENGTH_LONG).show()
        requireActivity().recreate()
    }

    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}