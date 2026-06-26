package ru.github.debitcredit.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Valute(
    @SerializedName("ID")
    val id: String,
    @SerializedName("NumCode")
    val numCode: String,
    @SerializedName("CharCode")
    val charCode: String,
    @SerializedName("Nominal")
    val nominal: Int,
    @SerializedName("Name")
    val name: String,
    @SerializedName("Value")
    val value: Double,
    @SerializedName("Previous")
    val previous: Double
)

data class CurrencyResponse(
    @SerializedName("Date")
    val date: String,
    @SerializedName("PreviousDate")
    val previousDate: String,
    @SerializedName("PreviousURL")
    val previousUrl: String,
    @SerializedName("Timestamp")
    val timestamp: String,
    @SerializedName("Valute")
    val valute: Map<String, Valute>
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
                rates["RUB"] = 0.0
                currencyResponse.valute.forEach { (code, valute) ->
                    rates[code] = valute.value / valute.nominal
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