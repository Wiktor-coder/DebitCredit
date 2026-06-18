package ru.github.debitcredit.utils

import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Valute(
    val ID: String,
    val NumCode: String,
    val CharCode: String,
    val Nominal: Int,
    val Name: String,
    val Value: Double,
    val Previous: Double
)

data class CurrencyResponse(
    val Date: String,
    val PreviousDate: String,
    val PreviousURL: String,
    val Timestamp: String,
    val Valute: Map<String, Valute>
)

object CurrencyService {
    private const val API_URL = "https://www.cbr-xml-daily.ru/daily_json.js"
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun getExchangeRates(): Map<String, Double>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(API_URL)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string()
                val currencyResponse = gson.fromJson(json, CurrencyResponse::class.java)

                val rates = mutableMapOf<String, Double>()
                rates["RUB"] = 1.0
                currencyResponse.Valute.forEach { (code, valute) ->
                    rates[code] = valute.Value / valute.Nominal
                }
                rates
            } else {
                null
            }
        } catch (e: IOException) {
            Log.e("CurrencyService", "Error fetching rates", e)
            null
        }
    }
}