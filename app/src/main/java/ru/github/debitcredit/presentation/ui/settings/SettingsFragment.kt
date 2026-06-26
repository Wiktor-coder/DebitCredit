package ru.github.debitcredit.presentation.ui.settings

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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.github.debitcredit.R
import ru.github.debitcredit.presentation.viewmodel.SettingsViewModel
import ru.github.debitcredit.utils.SettingsManager
import ru.github.debitcredit.utils.TimeZoneHelper
import java.util.*

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    private lateinit var currencySpinner: Spinner
    private lateinit var themeSwitch: SwitchCompat
    private lateinit var languageSpinner: Spinner
    private lateinit var applyButton: MaterialButton
    private lateinit var backButton: ImageButton
    private lateinit var currencyRateTextView: TextView
    private lateinit var timezoneSpinner: Spinner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        settingsViewModel.loadExchangeRates()
        observeExchangeRates()

        applyButton.setOnClickListener {
            saveSettings()
            applySettings()
        }
    }

    private fun observeExchangeRates() {
        viewLifecycleOwner.lifecycleScope.launch {
            settingsViewModel.exchangeRates.collect { rates ->
                val selectedCurrency = currencySpinner.selectedItem.toString()
                updateCurrencyRateDisplay(selectedCurrency, rates)
            }
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
                val rates = settingsViewModel.exchangeRates.value
                updateCurrencyRateDisplay(selectedCurrency, rates)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun updateCurrencyRateDisplay(currencyCode: String, rates: Map<String, Double>?) {
        val rate = rates?.get(currencyCode)
        currencyRateTextView.text = when {
            rate != null && currencyCode != "RUB" -> "1 $currencyCode = ${String.format("%.4f", rate)} ₽"
            currencyCode == "RUB" -> getString(R.string.base_currency)
            else -> getString(R.string.rate_not_available)
        }
    }

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
        val savedCurrency = settingsViewModel.getCurrency()
        val currencies = listOf("RUB", "USD", "EUR", "CNY", "GBP", "JPY")
        currencySpinner.setSelection(currencies.indexOf(savedCurrency))

        val isDarkTheme = settingsViewModel.isDarkTheme()
        themeSwitch.isChecked = isDarkTheme

        val savedLanguage = settingsViewModel.getLanguage()
        val languages = listOf("ru", "en")
        languageSpinner.setSelection(languages.indexOf(savedLanguage).coerceAtLeast(0))
    }

    private fun saveSettings() {
        val selectedCurrency = currencySpinner.selectedItem.toString()
        val isDarkTheme = themeSwitch.isChecked
        val selectedLanguage = when (languageSpinner.selectedItemPosition) {
            0 -> "ru"
            1 -> "en"
            else -> "ru"
        }

        settingsViewModel.saveCurrency(selectedCurrency)
        settingsViewModel.saveTheme(isDarkTheme)
        settingsViewModel.saveLanguage(selectedLanguage)

        val selectedTimezone = timezoneSpinner.selectedItem.toString()
        val offset = selectedTimezone.replace("UTC", "").toIntOrNull() ?: 0
        settingsViewModel.saveTimeZoneOffset(offset)
    }

    private fun applySettings() {
        saveSettings()

        val isDarkTheme = settingsViewModel.isDarkTheme()
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        val selectedLanguage = settingsViewModel.getLanguage()
        val locale = Locale(selectedLanguage)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        Toast.makeText(requireContext(), getString(R.string.settings_applied), Toast.LENGTH_LONG).show()
        requireActivity().recreate()
    }
}