package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Yahoo Finance API service for IDX / BEI stock data.
 * Supports tickers in format: CODE.JK (e.g., BBCA.JK, GOTO.JK, BMRI.JK).
 */
interface YahooFinanceApi {

    /**
     * Fetch chart data and market meta for a single ticker.
     * Example: https://query1.finance.yahoo.com/v8/finance/chart/BBCA.JK?interval=1d&range=1d
     */
    @GET("v8/finance/chart/{ticker}")
    suspend fun getChart(
        @Path("ticker") ticker: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "1d"
    ): YahooChartResponse

    /**
     * Fetch quotes for single or multiple comma-separated tickers.
     * Example: https://query1.finance.yahoo.com/v7/finance/quote?symbols=BBCA.JK,GOTO.JK
     */
    @GET("v7/finance/quote")
    suspend fun getQuote(
        @Query("symbols") symbols: String
    ): YahooQuoteResponse

    companion object {
        private const val BASE_URL = "https://query1.finance.yahoo.com/"

        /**
         * Formats raw user input (e.g. "BBCA" or "bbca") into IDX Yahoo Finance format ("BBCA.JK").
         */
        fun formatTickerForIdx(ticker: String): String {
            val clean = ticker.trim().uppercase()
            return if (clean.contains(".")) clean else "$clean.JK"
        }

        fun create(): YahooFinanceApi {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
                        )
                        .header("Accept", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(YahooFinanceApi::class.java)
        }
    }
}

// ----------------------------------------------------------------------------
// Response Models for v8/finance/chart/{ticker}
// ----------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class YahooChartResponse(
    @Json(name = "chart") val chart: YahooChartData? = null
)

@JsonClass(generateAdapter = true)
data class YahooChartData(
    @Json(name = "result") val result: List<YahooChartResult>? = null,
    @Json(name = "error") val error: YahooError? = null
)

@JsonClass(generateAdapter = true)
data class YahooChartResult(
    @Json(name = "meta") val meta: YahooChartMeta? = null
)

@JsonClass(generateAdapter = true)
data class YahooChartMeta(
    @Json(name = "currency") val currency: String? = null,
    @Json(name = "symbol") val symbol: String? = null,
    @Json(name = "regularMarketPrice") val regularMarketPrice: Double? = null,
    @Json(name = "chartPreviousClose") val chartPreviousClose: Double? = null,
    @Json(name = "previousClose") val previousClose: Double? = null
) {
    /**
     * Returns the latest available price: regular market price, falling back to previous close.
     */
    val effectivePrice: Double?
        get() = regularMarketPrice ?: previousClose ?: chartPreviousClose
}

// ----------------------------------------------------------------------------
// Response Models for v7/finance/quote
// ----------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class YahooQuoteResponse(
    @Json(name = "quoteResponse") val quoteResponse: YahooQuoteData? = null
)

@JsonClass(generateAdapter = true)
data class YahooQuoteData(
    @Json(name = "result") val result: List<YahooQuoteResult>? = null,
    @Json(name = "error") val error: YahooError? = null
)

@JsonClass(generateAdapter = true)
data class YahooQuoteResult(
    @Json(name = "symbol") val symbol: String? = null,
    @Json(name = "shortName") val shortName: String? = null,
    @Json(name = "longName") val longName: String? = null,
    @Json(name = "regularMarketPrice") val regularMarketPrice: Double? = null,
    @Json(name = "regularMarketPreviousClose") val regularMarketPreviousClose: Double? = null,
    @Json(name = "regularMarketChange") val regularMarketChange: Double? = null,
    @Json(name = "regularMarketChangePercent") val regularMarketChangePercent: Double? = null
) {
    val effectivePrice: Double?
        get() = regularMarketPrice ?: regularMarketPreviousClose
}

@JsonClass(generateAdapter = true)
data class YahooError(
    @Json(name = "code") val code: String? = null,
    @Json(name = "description") val description: String? = null
)
