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
        val coroutineScope = rememberCoroutineScope()
        val binanceApi = remember { BinanceApi() }
        
        LaunchedEffect(Unit) {
            try {
                candles = binanceApi.getBtcUsdtKlines(interval = "1d")
            } catch (e: Exception) {
                println("Error fetching data: ${e.message}")
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
                            candles = binanceApi.getBtcUsdtKlines(interval = "1d")
                        } catch (e: Exception) {
                            println("Error refreshing data: ${e.message}")
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
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}