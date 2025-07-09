package shakir.kadakkadan.code.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import shakir.kadakkadan.code.myapplication.api.BinanceApi
import shakir.kadakkadan.code.myapplication.model.CandleData
import shakir.kadakkadan.code.myapplication.ui.CandlestickChart

@Composable
@Preview
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        var candles by remember { mutableStateOf<List<CandleData>>(emptyList()) }
        var isLoadingHistorical by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val binanceApi = remember { BinanceApi() }
        
        // Function to load more historical data
        val loadMoreHistoricalData = {
            if (!isLoadingHistorical && candles.isNotEmpty()) {
                coroutineScope.launch {
                    isLoadingHistorical = true
                    try {
                        // Get the timestamp of the oldest candle
                        val oldestCandleTime = candles.first().openTime
                        println("🔍 Loading historical data before timestamp: $oldestCandleTime")
                        println("📈 Current dataset size: ${candles.size} candles")
                        
                        // Fetch 500 more candles ending before the oldest one
                        val historicalData = binanceApi.getBtcUsdtKlines(
                            interval = "1d",
                            limit = 500,
                            endTime = oldestCandleTime - 1
                        )
                        
                        println("📊 Historical data loaded: ${historicalData.size} candles")
                        
                        // Prepend historical data to existing candles
                        candles = historicalData + candles
                        
                        println("✅ Dataset updated: ${candles.size} total candles")
                    } catch (e: Exception) {
                        println("❌ Error loading historical data: ${e.message}")
                    } finally {
                        isLoadingHistorical = false
                    }
                }
            }
        }
        
        LaunchedEffect(Unit) {
            try {
                println("🚀 Starting initial data load...")
                candles = binanceApi.getBtcUsdtKlines(interval = "1d")
                println("✅ Initial data loaded successfully: ${candles.size} candles")
            } catch (e: Exception) {
                println("❌ Error fetching initial data: ${e.message}")
            }
        }
        
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .background(Color(0xFF0D1117)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = { 
                    coroutineScope.launch {
                        try {
                            println("🔄 Refreshing data...")
                            candles = binanceApi.getBtcUsdtKlines(interval = "1d")
                            println("✅ Data refreshed successfully: ${candles.size} candles")
                        } catch (e: Exception) {
                            println("❌ Error refreshing data: ${e.message}")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF238636)
                )
            ) {
                Text("Refresh Data", color = Color.White)
            }
            
            CandlestickChart(
                candles = candles,
                onLoadMoreHistoricalData = loadMoreHistoricalData,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}