package shakir.kadakkadan.klinechart.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import shakir.kadakkadan.klinechart.model.CandleData
import shakir.kadakkadan.klinechart.model.TickerData
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
            logger = Logger.SIMPLE
            level = LogLevel.INFO
        }
    }

    suspend fun getBtcUsdtKlines(symbol: String = "BTCUSDT", interval: String = "1d", limit: Int = 1000, startTime: Long? = null, endTime: Long? = null): List<CandleData> {
        val response = client.get("https://api.binance.com/api/v3/klines") {
            parameter("symbol", symbol)
            parameter("interval", interval)
            parameter("limit", limit)
            startTime?.let { parameter("startTime", it) }
            endTime?.let { parameter("endTime", it) }
        }
        
        val rawData: List<List<JsonElement>> = response.body()
        
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
        val response = client.get("https://api.binance.com/api/v3/ticker/24hr")
        val tickers: List<TickerData> = response.body()
        
        // Filter for USDT pairs only
        return tickers.filter { it.symbol.endsWith("USDT") }
    }
    
    fun close() {
        client.close()
    }
}