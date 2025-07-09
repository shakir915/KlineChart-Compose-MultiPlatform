package shakir.kadakkadan.code.myapplication.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import shakir.kadakkadan.code.myapplication.model.CandleData
import shakir.kadakkadan.code.myapplication.model.TickerData
import kotlinx.serialization.json.JsonElement

class BinanceApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
            filter { request ->
                request.url.host.contains("binance", ignoreCase = true)
            }
        }
    }

    suspend fun getBtcUsdtKlines(symbol: String = "BTCUSDT", interval: String = "1d", limit: Int = 1000, startTime: Long? = null, endTime: Long? = null): List<CandleData> {
        println("🔄 Fetching candles: symbol=$symbol, interval=$interval, limit=$limit, startTime=$startTime, endTime=$endTime")
        
        val response = client.get("https://api.binance.com/api/v3/klines") {
            parameter("symbol", symbol)
            parameter("interval", interval)
            parameter("limit", limit)
            startTime?.let { parameter("startTime", it) }
            endTime?.let { parameter("endTime", it) }
        }
        
        val rawData: List<List<JsonElement>> = response.body()
        
        println("✅ Received ${rawData.size} candles from Binance API")
        if (rawData.isNotEmpty()) {
            val firstCandle = rawData.first()
            val lastCandle = rawData.last()
            println("📊 Data range: ${firstCandle[0]} to ${lastCandle[0]} (timestamps)")
        }
        
        return rawData.map { item ->
            CandleData(
                openTime = item[0].toString().toLong(),
                open = item[1].toString().replace("\"", "").toDouble(),
                high = item[2].toString().replace("\"", "").toDouble(),
                low = item[3].toString().replace("\"", "").toDouble(),
                close = item[4].toString().replace("\"", "").toDouble(),
                volume = item[5].toString().replace("\"", "").toDouble(),
                closeTime = item[6].toString().toLong(),
                quoteAssetVolume = item[7].toString().replace("\"", "").toDouble(),
                numberOfTrades = item[8].toString().toLong(),
                takerBuyBaseAssetVolume = item[9].toString().replace("\"", "").toDouble(),
                takerBuyQuoteAssetVolume = item[10].toString().replace("\"", "").toDouble(),
                ignore = item[11].toString().replace("\"", "")
            )
        }
    }
    
    suspend fun get24hrTicker(): List<TickerData> {
        println("🔄 Fetching 24hr ticker data...")
        
        val response = client.get("https://api.binance.com/api/v3/ticker/24hr")
        val tickers: List<TickerData> = response.body()
        
        // Filter for USDT pairs only
        val usdtPairs = tickers.filter { it.symbol.endsWith("USDT") }
        
        println("✅ Received ${tickers.size} tickers, filtered to ${usdtPairs.size} USDT pairs")
        
        return usdtPairs
    }
    
    fun close() {
        client.close()
    }
}