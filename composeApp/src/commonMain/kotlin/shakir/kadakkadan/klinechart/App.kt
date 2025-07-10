package shakir.kadakkadan.klinechart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import shakir.kadakkadan.klinechart.api.BinanceApi
import shakir.kadakkadan.klinechart.model.CandleData
import shakir.kadakkadan.klinechart.model.TickerData
import shakir.kadakkadan.klinechart.ui.CandlestickChart
import shakir.kadakkadan.klinechart.ui.HomePage
import shakir.kadakkadan.klinechart.ui.MarketCategory
import shakir.kadakkadan.klinechart.ui.Timeframe

// Platform-specific back button handling
@Composable
expect fun handleBackButton(onBackPressed: () -> Unit): () -> Unit

@Composable
@Preview
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        var currentPage by remember { mutableStateOf<String?>(null) }
        var selectedSymbol by remember { mutableStateOf("BTCUSDT") }
        var cachedTickers by remember { mutableStateOf<List<TickerData>>(emptyList()) }
        var selectedCategory by remember { mutableStateOf(MarketCategory.VOLUME) }
        var selectedTimeframe by remember { mutableStateOf(Timeframe.ONE_DAY) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            when (currentPage) {
                null -> {
                    // Home page - show trading pairs
                    HomePage(
                        cachedTickers = cachedTickers,
                        selectedCategory = selectedCategory,
                        onTickersUpdated = { tickers ->
                            cachedTickers = tickers
                        },
                        onCategoryChanged = { category ->
                            selectedCategory = category
                        },
                        onPairSelected = { symbol ->
                            selectedSymbol = symbol
                            currentPage = "chart"
                        }
                    )
                }

                "chart" -> {
                    // Chart page - show candlestick chart
                    ChartPage(
                        symbol = selectedSymbol,
                        selectedTimeframe = selectedTimeframe,
                        onTimeframeChanged = { timeframe ->
                            selectedTimeframe = timeframe
                        },
                        onBackClicked = { currentPage = null }
                    )
                }
            }
        }
    }
}

@Composable
fun ChartPage(
    symbol: String,
    selectedTimeframe: Timeframe,
    onTimeframeChanged: (Timeframe) -> Unit,
    onBackClicked: () -> Unit
) {
    var candles by remember { mutableStateOf<List<CandleData>>(emptyList()) }
    var isLoadingHistorical by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val binanceApi = remember { BinanceApi() }
    
    // Handle platform-specific back button
    val cleanup = handleBackButton(onBackClicked)
    DisposableEffect(onBackClicked) {
        onDispose { cleanup() }
    }

    // Function to load more historical data
    val loadMoreHistoricalData = {
        if (!isLoadingHistorical && candles.isNotEmpty()) {
            coroutineScope.launch {
                isLoadingHistorical = true
                try {
                    // Get the timestamp of the oldest candle
                    val oldestCandleTime = candles.first().openTime

                    // Fetch 500 more candles ending before the oldest one
                    val historicalData = binanceApi.getBtcUsdtKlines(
                        symbol = symbol,
                        interval = selectedTimeframe.apiValue,
                        limit = 500,
                        endTime = oldestCandleTime - 1
                    )

                    // Only prepend if we actually got new data
                    if (historicalData.isNotEmpty()) {
                        candles = historicalData + candles
                    }
                } catch (e: Exception) {
                    // Error will be logged by Ktor logging interceptor
                } finally {
                    isLoadingHistorical = false
                }
            }
        }
    }

    LaunchedEffect(symbol, selectedTimeframe) {
        try {
            candles =
                binanceApi.getBtcUsdtKlines(symbol = symbol, interval = selectedTimeframe.apiValue)
        } catch (e: Exception) {
            // Error will be logged by Ktor logging interceptor
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button with better alignment
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color(0xFF21262D),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "←",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Text(
                text = "Back",
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = symbol,
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            candles = binanceApi.getBtcUsdtKlines(
                                symbol = symbol,
                                interval = selectedTimeframe.apiValue
                            )
                        } catch (e: Exception) {
                            // Error will be logged by Ktor logging interceptor
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF238636)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Refresh", color = Color.White, fontSize = 12.sp)
            }
        }

        CandlestickChart(
            candles = candles,
            symbol = symbol,
            selectedTimeframe = selectedTimeframe,
            onTimeframeChanged = onTimeframeChanged,
            onLoadMoreHistoricalData = loadMoreHistoricalData,
            modifier = Modifier.fillMaxSize()
        )
    }
}