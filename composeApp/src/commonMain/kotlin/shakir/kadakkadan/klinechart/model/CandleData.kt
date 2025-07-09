package shakir.kadakkadan.klinechart.model

import kotlinx.serialization.Serializable

@Serializable
data class CandleData(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long,
    val quoteAssetVolume: Double,
    val numberOfTrades: Long,
    val takerBuyBaseAssetVolume: Double,
    val takerBuyQuoteAssetVolume: Double,
    val ignore: String
)