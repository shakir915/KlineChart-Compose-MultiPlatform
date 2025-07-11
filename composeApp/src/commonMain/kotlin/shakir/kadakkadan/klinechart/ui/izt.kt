package shakir.kadakkadan.klinechart.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import shakir.kadakkadan.klinechart.model.CandleData
import kotlin.math.abs



@Composable
fun IsolatedZoomTest() {
    // Self-contained zoom state
    var testZoom by remember { mutableStateOf(1f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Text(
            text = "Test Zoom: $testZoom",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(16.dp)
        )

        Button(
            onClick = { testZoom += 0.1f },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Manual +")
        }

        Button(
            onClick = { testZoom -= 0.1f },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Manual -")
        }

        Box(
            modifier = Modifier
                .width(60.dp)
                .height(200.dp)
                .background(Color.Gray)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        val dragY = dragAmount.y
                        println("Isolated test - dragY: $dragY, currentZoom: $testZoom")

                        if (abs(dragY) > 1f) {
                            val newZoom = if (dragY < 0) {
                                testZoom + 0.05f
                            } else {
                                testZoom - 0.05f
                            }
                            println("  -> Setting zoom from $testZoom to $newZoom")
                            testZoom = newZoom
                        }
                    }
                }
        ) {
            Text(
                text = "Drag here\nZoom: ${(testZoom * 100).toInt() / 100.0}",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

// Updated PriceBar that directly modifies a local state for testing
@Composable
fun TestPriceBar(
    onYZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local zoom state for testing
    var localZoom by remember { mutableStateOf(1f) }

    // Update parent whenever local changes
    LaunchedEffect(localZoom) {
        onYZoomChange(localZoom)
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    val dragY = dragAmount.y
                    println("TestPriceBar - dragY: $dragY, localZoom: $localZoom")

                    if (abs(dragY) > 1f) {
                        val newZoom = if (dragY < 0) {
                            localZoom + 0.05f
                        } else {
                            localZoom - 0.05f
                        }
                        println("  -> TestPriceBar setting localZoom from $localZoom to $newZoom")
                        localZoom = newZoom
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Local: ${(localZoom * 100).toInt() / 100.0}",
                color = Color.White,
                fontSize = 10.sp
            )
            repeat(8) { index ->
                Text(
                    text = "$${(100 + index * 10)}",
                    color = Color(0xFF8B949E),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}


@Composable
fun DebugCandlestickChart(
    candles: List<CandleData>,
    symbol: String = "BTC/USDT",
    selectedTimeframe: Timeframe = Timeframe.ONE_DAY,
    onTimeframeChanged: (Timeframe) -> Unit = {},
    onLoadMoreHistoricalData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("Loading chart data...", fontSize = 16.sp)
        }
        return
    }

    // Zoom and scroll state with detailed logging
    var xZoom by remember {
        mutableStateOf(1f).also {
            println("DEBUG: xZoom state initialized to 1f")
        }
    }
    var yZoom by remember {
        mutableStateOf(1f).also {
            println("DEBUG: yZoom state initialized to 1f")
        }
    }
    var xOffset by remember { mutableStateOf(0f) }
    var yOffset by remember { mutableStateOf(0f) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var isInitialPosition by remember { mutableStateOf(true) }
    var mousePosition by remember { mutableStateOf<Offset?>(null) }
    var previousCandleCount by remember { mutableStateOf(0) }
    var isLoadingHistorical by remember { mutableStateOf(false) }
    var hasRequestedHistoricalData by remember { mutableStateOf(false) }
    var stableViewportMinPrice by remember { mutableStateOf(0.0) }
    var stableViewportMaxPrice by remember { mutableStateOf(0.0) }
    var useStableViewport by remember { mutableStateOf(false) }

    // Mobile crosshair state
    var isCrosshairActive by remember { mutableStateOf(false) }
    var crosshairPosition by remember { mutableStateOf<Offset?>(null) }

    // Debug: Track yZoom changes
    LaunchedEffect(yZoom) {
        println("DEBUG: yZoom changed to $yZoom")
    }

    // Base dimensions
    val baseCandleWidth = 20f
    val baseCandleSpacing = 5f

    // Zoomed dimensions
    val candleWidth = baseCandleWidth * xZoom
    val candleSpacing = baseCandleSpacing * xZoom
    val totalWidth = candles.size * (candleWidth + candleSpacing)
    val baseChartHeight = 600f

    // Use stable viewport when loading historical data, otherwise use full range
    val displayMinPrice = if (useStableViewport) stableViewportMinPrice else candles.minOfOrNull { minOf(it.low, it.open, it.close, it.high) } ?: 0.0
    val displayMaxPrice = if (useStableViewport) stableViewportMaxPrice else candles.maxOfOrNull { maxOf(it.high, it.open, it.close, it.low) } ?: 100.0
    val priceRange = displayMaxPrice - displayMinPrice

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // DEBUG INFO at the top
        Text(
            text = "DEBUG: yZoom = $yZoom",
            color = Color.Red,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp)
        )

        // Price info header and zoom controls
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = symbol,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Price: $${candles.lastOrNull()?.close ?: "0.00"}",
                    fontSize = 14.sp,
                    color = Color(0xFF8B949E)
                )
            }

            // SIMPLIFIED zoom control buttons (remove potential conflicts)
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Y-axis zoom controls with logging
                Button(
                    onClick = {
                        val oldZoom = yZoom
                        val newZoom = (yZoom + 0.2f).coerceIn(0.1f, 5f)
                        println("DEBUG: Y+ button clicked: $oldZoom -> $newZoom")
                        yZoom = newZoom
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1f6feb))
                ) {
                    Text("Y+", fontSize = 12.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        val oldZoom = yZoom
                        val newZoom = (yZoom - 0.2f).coerceIn(0.1f, 5f)
                        println("DEBUG: Y- button clicked: $oldZoom -> $newZoom")
                        yZoom = newZoom
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1f6feb))
                ) {
                    Text("Y-", fontSize = 12.sp, color = Color.White)
                }

                // Reset button
                Button(
                    onClick = {
                        println("DEBUG: Reset button clicked")
                        xZoom = 1f
                        yZoom = 1f
                        xOffset = 0f
                        yOffset = 0f
                        isInitialPosition = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF656d76))
                ) {
                    Text("Reset", fontSize = 12.sp, color = Color.White)
                }

                // Display current zoom levels
                Text(
                    text = "Y:${(yZoom * 10).toInt() / 10.0}",
                    fontSize = 12.sp,
                    color = Color(0xFF8B949E),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Chart area with price bar - REMOVE main chart to isolate the issue
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Simplified placeholder for main chart
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF161B22))
            ) {
                Text(
                    text = "Chart placeholder\nRemoved to isolate PriceBar issue",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Price bar - THE ONLY THING THAT SHOULD AFFECT yZoom
            SimplePriceBar(
                yZoom = yZoom,
                onYZoomChange = { newZoom ->
                    println("DEBUG: PriceBar callback received: $newZoom (current yZoom: $yZoom)")
                    val clampedZoom = newZoom.coerceIn(0.1f, 5f)
                    println("DEBUG: Setting yZoom from $yZoom to $clampedZoom")
                    yZoom = clampedZoom
                    println("DEBUG: yZoom is now $yZoom")
                },
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF21262D))
            )
        }
    }
}

@Composable
fun SimplePriceBar(
    yZoom: Float,
    onYZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragCount by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        println("DEBUG: PriceBar drag started")
                        dragCount = 0
                    },
                    onDragEnd = {
                        println("DEBUG: PriceBar drag ended after $dragCount drags")
                    }
                ) { change, dragAmount ->
                    dragCount++
                    val dragY = dragAmount.y
                    println("DEBUG: PriceBar drag #$dragCount: dragY=$dragY, yZoom=$yZoom")

                    if (abs(dragY) > 2f) {
                        val zoomDelta = if (dragY < 0) 0.05f else -0.05f
                        val newZoom = yZoom + zoomDelta
                        println("DEBUG: PriceBar calling onYZoomChange($newZoom)")
                        onYZoomChange(newZoom)
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Y: ${(yZoom * 100).toInt() / 100.0}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Drags: $dragCount",
                color = Color.Yellow,
                fontSize = 10.sp
            )

            repeat(8) { index ->
                Text(
                    text = "$${(1000 + index * 100)}",
                    color = Color(0xFF8B949E),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}


