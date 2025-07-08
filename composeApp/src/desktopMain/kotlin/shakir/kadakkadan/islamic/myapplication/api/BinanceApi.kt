package shakir.kadakkadan.islamic.myapplication.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import shakir.kadakkadan.islamic.myapplication.model.CandleData
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

class BinanceApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun getBtcUsdtKlines(interval: String = "1d", limit: Int = 100): List<CandleData> {
        val response = client.get("https://api.binance.com/api/v3/klines") {
            parameter("symbol", "BTCUSDT")
            parameter("interval", interval)
            parameter("limit", limit)
        }
        
        val rawData: List<List<kotlinx.serialization.json.JsonElement>> = response.body()
        
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
    
    fun close() {
        client.close()
    }
}