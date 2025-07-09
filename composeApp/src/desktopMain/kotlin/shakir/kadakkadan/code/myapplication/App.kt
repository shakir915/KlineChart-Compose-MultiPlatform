package shakir.kadakkadan.code.myapplication

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
import shakir.kadakkadan.code.myapplication.api.BinanceApi
import shakir.kadakkadan.code.myapplication.model.CandleData
import shakir.kadakkadan.code.myapplication.model.TickerData
import shakir.kadakkadan.code.myapplication.ui.CandlestickChart
import shakir.kadakkadan.code.myapplication.ui.HomePage
import shakir.kadakkadan.code.myapplication.ui.MarketCategory
import shakir.kadakkadan.code.myapplication.ui.Timeframe

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
                    println("🔍 Loading historical data before timestamp: $oldestCandleTime")
                    println("📈 Current dataset size: ${candles.size} candles")
                    
                    // Fetch 500 more candles ending before the oldest one
                    val historicalData = binanceApi.getBtcUsdtKlines(
                        symbol = symbol,
                        interval = selectedTimeframe.apiValue,
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
    
    LaunchedEffect(symbol, selectedTimeframe) {
        try {
            println("🚀 Starting initial data load for $symbol (${selectedTimeframe.displayName})...")
            candles = binanceApi.getBtcUsdtKlines(symbol = symbol, interval = selectedTimeframe.apiValue)
            println("✅ Initial data loaded successfully: ${candles.size} candles")
        } catch (e: Exception) {
            println("❌ Error fetching initial data: ${e.message}")
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
                            println("🔄 Refreshing data...")
                            candles = binanceApi.getBtcUsdtKlines(symbol = symbol, interval = selectedTimeframe.apiValue)
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