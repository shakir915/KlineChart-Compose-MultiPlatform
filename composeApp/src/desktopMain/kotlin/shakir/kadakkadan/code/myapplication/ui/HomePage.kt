package shakir.kadakkadan.code.myapplication.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import shakir.kadakkadan.code.myapplication.api.BinanceApi
import shakir.kadakkadan.code.myapplication.model.TickerData

@Composable
fun HomePage(
    onPairSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var tickers by remember { mutableStateOf<List<TickerData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val binanceApi = remember { BinanceApi() }
    
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            error = null
            val tickerData = binanceApi.get24hrTicker()
            // Sort by quote volume (most traded today)
            tickers = tickerData.sortedByDescending { it.quoteVolume.toDoubleOrNull() ?: 0.0 }
            println("📊 Loaded ${tickers.size} USDT pairs, sorted by volume")
        } catch (e: Exception) {
            error = e.message
            println("❌ Error loading ticker data: ${e.message}")
        } finally {
            isLoading = false
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
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF238636)
                )
            }
        }
        
        // Subheader
        Text(
            text = "Sorted by 24h Volume (Most Traded Today)",
            fontSize = 14.sp,
            color = Color(0xFF8B949E),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        when {
            error != null -> {
                ErrorMessage(error = error!!) {
                    // Retry
                    coroutineScope.launch {
                        try {
                            isLoading = true
                            error = null
                            val tickerData = binanceApi.get24hrTicker()
                            tickers = tickerData.sortedByDescending { it.quoteVolume.toDoubleOrNull() ?: 0.0 }
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            isLoading = false
                        }
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
                // Trading pairs list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tickers) { ticker ->
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
                    text = "$${String.format("%.4f", ticker.lastPrice.toDoubleOrNull() ?: 0.0)}",
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
                    text = "${changePrefix}${String.format("%.2f", priceChange)}%",
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
        volume >= 1_000_000_000 -> String.format("%.1fB", volume / 1_000_000_000)
        volume >= 1_000_000 -> String.format("%.1fM", volume / 1_000_000)
        volume >= 1_000 -> String.format("%.1fK", volume / 1_000)
        else -> String.format("%.0f", volume)
    }
}