package ru.github.debitcredit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.github.debitcredit.utils.CurrencyService
import ru.github.debitcredit.utils.SettingsManager
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _exchangeRates = MutableStateFlow<Map<String, Double>?>(null)
    val exchangeRates: StateFlow<Map<String, Double>?> = _exchangeRates.asStateFlow()

    fun getCurrency(): String = settingsManager.getCurrency()
    fun isDarkTheme(): Boolean = settingsManager.isDarkTheme()
    fun getLanguage(): String = settingsManager.getLanguage()

    fun saveCurrency(currency: String) {
        settingsManager.saveCurrency(currency)
    }

    fun saveTheme(isDark: Boolean) {
        settingsManager.saveTheme(isDark)
    }

    fun saveLanguage(language: String) {
        settingsManager.saveLanguage(language)
    }

    fun saveTimeZoneOffset(offset: Int) {
        settingsManager.saveTimeZoneOffset(offset)
    }

    fun loadExchangeRates() {
        viewModelScope.launch {
            try {
                val rates = CurrencyService.getExchangeRates()
                _exchangeRates.value = rates
            } catch (_: Exception) {
                _exchangeRates.value = null
            }
        }
    }
}