package shakir.kadakkadan.klinechart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import shakir.kadakkadan.klinechart.api.BinanceApi
import shakir.kadakkadan.klinechart.model.CandleData
import shakir.kadakkadan.klinechart.model.TickerData
import shakir.kadakkadan.klinechart.ui.CandlestickChart
import shakir.kadakkadan.klinechart.ui.HomePage
import shakir.kadakkadan.klinechart.ui.MarketCategory
import shakir.kadakkadan.klinechart.ui.Timeframe

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
                    
                    // Prepend historical data to existing candles
                    candles = historicalData + candles
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
            candles = binanceApi.getBtcUsdtKlines(symbol = symbol, interval = selectedTimeframe.apiValue)
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBackClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF21262D)
                )
            ) {
                Text("← Back", color = Color.White)
            }
            
            Text(
                text = symbol,
                fontSize = 20.sp,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { 
                    coroutineScope.launch {
                        try {
                            candles = binanceApi.getBtcUsdtKlines(symbol = symbol, interval = selectedTimeframe.apiValue)
                        } catch (e: Exception) {
                            // Error will be logged by Ktor logging interceptor
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF238636)
                )
            ) {
                Text("Refresh", color = Color.White)
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