package shakir.kadakkadan.klinechart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import shakir.kadakkadan.klinechart.api.BinanceApi
import shakir.kadakkadan.klinechart.model.TickerData
import kotlinx.datetime.Clock

enum class MarketCategory {
    ALL, GAINERS, LOSERS, VOLUME
}

private fun isRecentlyTraded(ticker: TickerData): Boolean {
    val now = Clock.System.now().toEpochMilliseconds()
    val closeTime = ticker.closeTime
    val dayInMillis = 24 * 60 * 60 * 1000L
    val twoDaysAgo = now - (2 * dayInMillis)
    
    return closeTime >= twoDaysAgo
}

@Composable
fun HomePage(
    onPairSelected: (String) -> Unit,
    cachedTickers: List<TickerData> = emptyList(),
    selectedCategory: MarketCategory = MarketCategory.VOLUME,
    onTickersUpdated: (List<TickerData>) -> Unit = {},
    onCategoryChanged: (MarketCategory) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var tickers by remember { mutableStateOf(cachedTickers) }
    var isLoading by remember { mutableStateOf(cachedTickers.isEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val coroutineScope = rememberCoroutineScope()
    val binanceApi = remember { BinanceApi() }
    
    // Load fresh data
    val loadData = suspend {
        try {
            isLoading = true
            error = null
            val tickerData = binanceApi.get24hrTicker()
            tickers = tickerData
            onTickersUpdated(tickerData)
        } catch (e: Exception) {
            error = e.message
            // Error will be logged by Ktor logging interceptor
        } finally {
            isLoading = false
        }
    }
    
    LaunchedEffect(Unit) {
        if (cachedTickers.isEmpty()) {
            loadData()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "USDT Trading Pairs",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF238636)
                    )
                }
                
                Button(
                    onClick = {
                        coroutineScope.launch {
                            loadData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF238636)
                    ),
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Refresh",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        // Search bar
        SearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onSearchSubmit = { query ->
                // Navigate directly to chart if search query looks like a valid symbol
                val symbol = query.uppercase().let { 
                    if (it.endsWith("USDT")) it else "${it}USDT" 
                }
                onPairSelected(symbol)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        // Category tabs
        CategoryTabs(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategoryChanged,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        when {
            error != null -> {
                ErrorMessage(error = error!!) {
                    // Retry
                    coroutineScope.launch {
                        loadData()
                    }
                }
            }
            
            tickers.isEmpty() && !isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No trading pairs available",
                        color = Color(0xFF8B949E),
                        fontSize = 16.sp
                    )
                }
            }
            
            else -> {
                // First filter out delisted/old pairs
                val recentTickers = tickers.filter { isRecentlyTraded(it) }
                
                // Get filtered and sorted tickers based on selected category and search query
                val categoryFilteredTickers = when (selectedCategory) {
                    MarketCategory.ALL -> recentTickers.sortedBy { it.symbol }
                    MarketCategory.GAINERS -> recentTickers
                        .filter { (it.priceChangePercent.toDoubleOrNull() ?: 0.0) > 0 }
                        .sortedByDescending { it.priceChangePercent.toDoubleOrNull() ?: 0.0 }
                    MarketCategory.LOSERS -> recentTickers
                        .filter { (it.priceChangePercent.toDoubleOrNull() ?: 0.0) < 0 }
                        .sortedBy { it.priceChangePercent.toDoubleOrNull() ?: 0.0 }
                    MarketCategory.VOLUME -> recentTickers
                        .sortedByDescending { it.quoteVolume.toDoubleOrNull() ?: 0.0 }
                }
                
                // Apply search filter
                val filteredTickers = if (searchQuery.isBlank()) {
                    categoryFilteredTickers
                } else {
                    categoryFilteredTickers.filter { 
                        it.symbol.contains(searchQuery.uppercase(), ignoreCase = true)
                    }
                }
                
                // Trading pairs list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTickers) { ticker ->
                        TradingPairCard(
                            ticker = ticker,
                            onClick = { onPairSelected(ticker.symbol) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
            Text(
                text = "Search USDT pairs (e.g. BTC, ETH, ADA)",
                color = Color(0xFF8B949E)
            )
        },
        leadingIcon = {
            Text(
                text = "🔍",
                color = Color(0xFF8B949E),
                fontSize = 16.sp
            )
        },
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(
                    onClick = { onSearchQueryChange("") }
                ) {
                    Text(
                        text = "✕",
                        color = Color(0xFF8B949E),
                        fontSize = 14.sp
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF238636),
            unfocusedBorderColor = Color(0xFF21262D),
            cursorColor = Color(0xFF238636)
        ),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        modifier = modifier
            .onKeyEvent { keyEvent ->
                if (keyEvent.key == Key.Enter) {
                    if (searchQuery.isNotBlank()) {
                        onSearchSubmit(searchQuery)
                    }
                    true
                } else {
                    false
                }
            }
    )
}

@Composable
fun CategoryTabs(
    selectedCategory: MarketCategory,
    onCategorySelected: (MarketCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryTab(
            title = "All",
            isSelected = selectedCategory == MarketCategory.ALL,
            onClick = { onCategorySelected(MarketCategory.ALL) },
            modifier = Modifier.weight(1f)
        )
        
        CategoryTab(
            title = "🚀 Gainers",
            isSelected = selectedCategory == MarketCategory.GAINERS,
            onClick = { onCategorySelected(MarketCategory.GAINERS) },
            modifier = Modifier.weight(1f)
        )
        
        CategoryTab(
            title = "📉 Losers",
            isSelected = selectedCategory == MarketCategory.LOSERS,
            onClick = { onCategorySelected(MarketCategory.LOSERS) },
            modifier = Modifier.weight(1f)
        )
        
        CategoryTab(
            title = "📊 Volume",
            isSelected = selectedCategory == MarketCategory.VOLUME,
            onClick = { onCategorySelected(MarketCategory.VOLUME) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun CategoryTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF238636) else Color(0xFF21262D)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color(0xFF8B949E),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TradingPairCard(
    ticker: TickerData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symbol and price info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = ticker.symbol,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "$${(ticker.lastPrice.toDoubleOrNull() ?: 0.0).toString().take(8)}",
                    fontSize = 14.sp,
                    color = Color(0xFF8B949E),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            // Price change
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val priceChange = ticker.priceChangePercent.toDoubleOrNull() ?: 0.0
                val changeColor = if (priceChange >= 0) Color(0xFF00D4AA) else Color(0xFFFF4747)
                val changePrefix = if (priceChange >= 0) "+" else ""
                
                Text(
                    text = "${changePrefix}${(priceChange * 100).toInt() / 100.0}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = changeColor
                )
                
                Text(
                    text = "Vol: ${formatVolume(ticker.quoteVolume.toDoubleOrNull() ?: 0.0)}",
                    fontSize = 12.sp,
                    color = Color(0xFF8B949E),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ErrorMessage(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF21262D)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error loading data",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF4747)
            )
            
            Text(
                text = error,
                fontSize = 14.sp,
                color = Color(0xFF8B949E),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF238636)
                )
            ) {
                Text("Retry", color = Color.White)
            }
        }
    }
}

private fun formatVolume(volume: Double): String {
    return when {
        volume >= 1_000_000_000 -> "${(volume / 1_000_000_000 * 10).toInt() / 10.0}B"
        volume >= 1_000_000 -> "${(volume / 1_000_000 * 10).toInt() / 10.0}M"
        volume >= 1_000 -> "${(volume / 1_000 * 10).toInt() / 10.0}K"
        else -> "${volume.toInt()}"
    }
}