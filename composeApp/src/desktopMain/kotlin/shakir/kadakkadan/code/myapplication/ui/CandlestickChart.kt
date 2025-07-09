package shakir.kadakkadan.code.myapplication.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import shakir.kadakkadan.code.myapplication.model.CandleData

@Composable
fun CandlestickChart(
    candles: List<CandleData>,
    symbol: String = "BTC/USDT",
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

    // Zoom and scroll state
    var xZoom by remember { mutableStateOf(1f) }
    var yZoom by remember { mutableStateOf(1f) }
    var xOffset by remember { mutableStateOf(0f) }
    var yOffset by remember { mutableStateOf(0f) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var isInitialPosition by remember { mutableStateOf(true) }
    var mousePosition by remember { mutableStateOf<Offset?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Base dimensions
    val baseCandleWidth = 20f
    val baseCandleSpacing = 5f
    
    // Zoomed dimensions
    val candleWidth = baseCandleWidth * xZoom
    val candleSpacing = baseCandleSpacing * xZoom
    val totalWidth = candles.size * (candleWidth + candleSpacing)
    val baseChartHeight = 600f // Base height without zoom
    
    val minPrice = candles.minOfOrNull { minOf(it.low, it.open, it.close, it.high) } ?: 0.0
    val maxPrice = candles.maxOfOrNull { maxOf(it.high, it.open, it.close, it.low) } ?: 100.0
    val priceRange = maxPrice - minPrice
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117)) // Dark background like TradingView
    ) {
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
                    text = "Price: $${candles.lastOrNull()?.close?.let { "%.2f".format(it) } ?: "0.00"}",
                    fontSize = 14.sp,
                    color = Color(0xFF8B949E) // Muted gray for secondary text
                )
            }
            
            // Zoom control buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // X-axis zoom controls
                Button(
                    onClick = { 
                        val oldZoom = xZoom
                        val newZoom = (xZoom + 0.2f).coerceIn(0.03f, 5f)
                        // Adjust offset to keep zoom centered on screen center
                        if (canvasSize != Size.Zero) {
                            val screenCenter = canvasSize.width / 2f
                            val zoomRatio = newZoom / oldZoom
                            xOffset = screenCenter - (screenCenter - xOffset) * zoomRatio
                        }
                        xZoom = newZoom
                        isInitialPosition = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("X+", fontSize = 12.sp, color = Color.White)
                }
                
                Button(
                    onClick = { 
                        val oldZoom = xZoom
                        val newZoom = (xZoom - 0.2f).coerceIn(0.03f, 5f)
                        // Adjust offset to keep zoom centered on screen center
                        if (canvasSize != Size.Zero) {
                            val screenCenter = canvasSize.width / 2f
                            val zoomRatio = newZoom / oldZoom
                            xOffset = screenCenter - (screenCenter - xOffset) * zoomRatio
                        }
                        xZoom = newZoom
                        isInitialPosition = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("X-", fontSize = 12.sp, color = Color.White)
                }
                
                // Y-axis zoom controls
                Button(
                    onClick = { yZoom = (yZoom + 0.2f).coerceIn(0.5f, 5f) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1f6feb)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Y+", fontSize = 12.sp, color = Color.White)
                }
                
                Button(
                    onClick = { yZoom = (yZoom - 0.2f).coerceIn(0.5f, 5f) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1f6feb)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Y-", fontSize = 12.sp, color = Color.White)
                }
                
                // Reset button
                Button(
                    onClick = { 
                        xZoom = 1f
                        yZoom = 1f
                        xOffset = 0f
                        yOffset = 0f
                        isInitialPosition = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF656d76)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Reset", fontSize = 12.sp, color = Color.White)
                }
                
                // Display current zoom levels
                Text(
                    text = "X:${String.format("%.1f", xZoom)} Y:${String.format("%.1f", yZoom)}",
                    fontSize = 12.sp,
                    color = Color(0xFF8B949E),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
        
        // Chart area with price and time bars
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main chart area
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Chart with scrolling and zooming
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF161B22)) // Slightly lighter dark background for chart area
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                // Handle infinite scrolling
                                xOffset += dragAmount.x
                                yOffset += dragAmount.y
                                isInitialPosition = false
                                
                                // Check if user is scrolling near the left edge (backward in time)
                                val totalWidth = candles.size * (candleWidth + candleSpacing)
                                val leftBoundary = size.width - totalWidth
                                val threshold = size.width * 0.3f
                                
                                if (xOffset > leftBoundary - threshold) {
                                    onLoadMoreHistoricalData()
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            // Handle scroll zoom for X-axis (touchpad scroll)
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { change ->
                                        val scroll = change.scrollDelta
                                        // Check if this is a scroll event (not just pointer movement)
                                        if (abs(scroll.y) > 0.1f) {
                                            val oldZoom = xZoom
                                            val newZoom = if (scroll.y > 0) {
                                                // Scroll up - zoom out X-axis
                                                xZoom - 0.1f
                                            } else {
                                                // Scroll down - zoom in X-axis
                                                xZoom + 0.1f
                                            }
                                            val clampedZoom = newZoom.coerceIn(0.03f, 5f)
                                            
                                            // Adjust offset to keep zoom centered on screen center
                                            val screenCenter = size.width / 2f
                                            val zoomRatio = clampedZoom / oldZoom
                                            xOffset = screenCenter - (screenCenter - xOffset) * zoomRatio
                                            
                                            xZoom = clampedZoom
                                            isInitialPosition = false
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            // Track mouse position for crosshair
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    mousePosition = event.changes.firstOrNull()?.position
                                }
                            }
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                    ) {
                        // Update canvas size for button handlers
                        canvasSize = size
                        
                        // Calculate initial offset to show latest candles on right side with 5% margin
                        if (candles.isNotEmpty() && isInitialPosition && canvasSize != Size.Zero) {
                            val totalCandleWidth = candles.size * (candleWidth + candleSpacing)
                            val marginWidth = canvasSize.width * 0.05f
                            xOffset = canvasSize.width - totalCandleWidth - marginWidth
                            isInitialPosition = false
                        }
                        
                        drawCandlestickChart(
                            candles = candles,
                            candleWidth = candleWidth,
                            candleSpacing = candleSpacing,
                            minPrice = minPrice,
                            maxPrice = maxPrice,
                            priceRange = priceRange,
                            chartHeight = baseChartHeight,
                            xOffset = xOffset,
                            yOffset = yOffset,
                            xZoom = xZoom,
                            yZoom = yZoom
                        )
                        
                        // Draw crosshair
                        mousePosition?.let { pos ->
                            drawCrosshair(pos)
                        }
                    }
                    
                    // Crosshair overlays
                    mousePosition?.let { pos ->
                        if (canvasSize != Size.Zero) {
                            // Calculate price at mouse Y position
                            val scaledHeight = canvasSize.height * yZoom
                            val adjustedY = pos.y - yOffset
                            val priceRatio = 1.0 - (adjustedY / scaledHeight).toDouble()
                            val currentPrice = minPrice + (priceRatio * priceRange)
                            
                            // Calculate time at mouse X position
                            val adjustedX = pos.x - xOffset
                            val candleIndex = (adjustedX / (candleWidth + candleSpacing)).toInt()
                            
                            if (candleIndex >= 0 && candleIndex < candles.size) {
                                val candle = candles[candleIndex]
                                val date = Date(candle.openTime)
                                val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())
                                val timeText = dateFormat.format(date)
                                
                                // Price label on Y-axis
                                CrosshairPriceLabel(
                                    price = currentPrice,
                                    yPosition = pos.y,
                                    canvasWidth = canvasSize.width
                                )
                                
                                // Time label on X-axis
                                CrosshairTimeLabel(
                                    time = timeText,
                                    xPosition = pos.x,
                                    canvasHeight = canvasSize.height
                                )
                            }
                        }
                    }
                }
                
                // Time bar at bottom
                TimeBar(
                    candles = candles,
                    candleWidth = candleWidth,
                    candleSpacing = candleSpacing,
                    xOffset = xOffset,
                    xZoom = xZoom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color(0xFF21262D))
                )
            }
            
            // Price bar on right with drag zoom
            PriceBar(
                minPrice = minPrice,
                maxPrice = maxPrice,
                chartHeight = baseChartHeight,
                yOffset = yOffset,
                yZoom = yZoom,
                isPriceBarClicked = false, // Not used anymore
                onPriceBarClick = { }, // Not used anymore
                onYZoomChange = { newZoom -> yZoom = newZoom },
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF21262D))
            )
        }
    }
}

private fun DrawScope.drawCandlestickChart(
    candles: List<CandleData>,
    candleWidth: Float,
    candleSpacing: Float,
    minPrice: Double,
    maxPrice: Double,
    priceRange: Double,
    chartHeight: Float,
    xOffset: Float,
    yOffset: Float,
    xZoom: Float,
    yZoom: Float
) {
    candles.forEachIndexed { index, candle ->
        val x = (index * (candleWidth + candleSpacing)) + xOffset
        
        // Only draw candles that are visible on screen
        if (x + candleWidth < 0 || x > size.width) return@forEachIndexed
        
        // Normalize prices to chart height with zoom and offset
        val baseHeight = size.height
        val scaledHeight = baseHeight * yZoom
        
        val openNorm = (candle.open - minPrice) / priceRange
        val closeNorm = (candle.close - minPrice) / priceRange
        val highNorm = (candle.high - minPrice) / priceRange
        val lowNorm = (candle.low - minPrice) / priceRange
        
        val openY = (scaledHeight - (openNorm * scaledHeight)).toFloat() + yOffset
        val closeY = (scaledHeight - (closeNorm * scaledHeight)).toFloat() + yOffset
        val highY = (scaledHeight - (highNorm * scaledHeight)).toFloat() + yOffset
        val lowY = (scaledHeight - (lowNorm * scaledHeight)).toFloat() + yOffset
        
        // Determine candle color (green for bullish, red for bearish) - dark theme colors
        val candleColor = if (candle.close > candle.open) {
            Color(0xFF00D4AA) // Bright green for bullish candles
        } else {
            Color(0xFFFF4747) // Bright red for bearish candles
        }
        
        // Draw high-low line (wick)
        drawLine(
            color = candleColor,
            start = Offset(x + candleWidth / 2, highY),
            end = Offset(x + candleWidth / 2, lowY),
            strokeWidth = 2f
        )
        
        // Draw open-close rectangle (body)
        val bodyTop = minOf(openY, closeY)
        val bodyBottom = maxOf(openY, closeY)
        val bodyHeight = bodyBottom - bodyTop
        
        drawRect(
            color = candleColor,
            topLeft = Offset(x, bodyTop),
            size = Size(candleWidth, bodyHeight)
        )
        
        // Draw borders for hollow candles when close > open
        if (candle.close > candle.open) {
            drawRect(
                color = Color(0xFF00D4AA), // Bright green border for bullish candles
                topLeft = Offset(x, bodyTop),
                size = Size(candleWidth, bodyHeight),
                style = Stroke(width = 2f)
            )
        }
    }
}

@Composable
fun PriceBar(
    minPrice: Double,
    maxPrice: Double,
    chartHeight: Float,
    yOffset: Float,
    yZoom: Float,
    isPriceBarClicked: Boolean,
    onPriceBarClick: () -> Unit,
    onYZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val priceRange = maxPrice - minPrice
    val priceSteps = 12 // More price levels for better granularity
    
    // Calculate visible price range based on zoom and offset
    val effectiveHeight = chartHeight * yZoom
    val visibleHeight = 400f
    val visibleTopRatio = (-yOffset) / maxOf(effectiveHeight - visibleHeight, 1f)
    val visibleBottomRatio = (visibleHeight - yOffset) / effectiveHeight
    
    val visibleMinPrice = minPrice + (priceRange * (1 - visibleBottomRatio))
    val visibleMaxPrice = minPrice + (priceRange * (1 - visibleTopRatio))
    val visiblePriceRange = visibleMaxPrice - visibleMinPrice
    
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    // Continuous Y-axis zoom while dragging
                    val dragY = dragAmount.y
                    println("Price bar drag: dragY = $dragY, current yZoom = $yZoom")
                    
                    // Apply zoom change based on drag amount
                    if (abs(dragY) > 0.1f) {
                        val zoomDelta = if (dragY < 0) {
                            // Drag up - zoom in Y-axis
                            0.02f
                        } else {
                            // Drag down - zoom out Y-axis
                            -0.02f
                        }
                        val newZoom = yZoom + zoomDelta
                        val clampedZoom = newZoom.coerceIn(0.5f, 5f)
                        println("Setting new Y zoom: $clampedZoom")
                        onYZoomChange(clampedZoom)
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(priceSteps) { index ->
                val price = visibleMaxPrice - (visiblePriceRange * index / (priceSteps - 1))
                Text(
                    text = "$%.0f".format(price),
                    color = Color(0xFF8B949E),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun TimeBar(
    candles: List<CandleData>,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    xZoom: Float,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    val timeSteps = 6 // Number of time labels to show
    
    // Calculate visible candle range based on offset and zoom
    val candleStepWidth = candleWidth + candleSpacing
    val visibleWidth = 800f // Approximate visible width
    
    // Calculate visible candle indices
    val startCandleIndex = ((-xOffset) / candleStepWidth).toInt().coerceIn(0, candles.size - 1)
    val endCandleIndex = ((visibleWidth - xOffset) / candleStepWidth).toInt().coerceIn(0, candles.size - 1)
    val visibleCandleCount = maxOf(endCandleIndex - startCandleIndex, 1)
    
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(timeSteps) { index ->
                val candleIndex = if (visibleCandleCount > 1) {
                    startCandleIndex + (visibleCandleCount * index / (timeSteps - 1))
                } else {
                    startCandleIndex
                }.coerceIn(0, candles.size - 1)
                
                val candle = candles[candleIndex]
                val date = Date(candle.openTime)
                
                Text(
                    text = dateFormat.format(date),
                    color = Color(0xFF8B949E),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawCrosshair(mousePosition: Offset) {
    // Draw crosshair lines
    val crosshairColor = Color(0xFF8B949E).copy(alpha = 0.7f)
    
    // Horizontal line
    drawLine(
        color = crosshairColor,
        start = Offset(0f, mousePosition.y),
        end = Offset(size.width, mousePosition.y),
        strokeWidth = 1.dp.toPx(),
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
    )
    
    // Vertical line
    drawLine(
        color = crosshairColor,
        start = Offset(mousePosition.x, 0f),
        end = Offset(mousePosition.x, size.height),
        strokeWidth = 1.dp.toPx(),
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
    )
}

@Composable
fun CrosshairPriceLabel(
    price: Double,
    yPosition: Float,
    canvasWidth: Float
) {
    with(LocalDensity.current) {
        Box(
            modifier = Modifier
                .offset(x = (canvasWidth - 60.dp.toPx()).toDp(), y = (yPosition - 10.dp.toPx()).toDp())
                .background(Color(0xFF21262D), shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = String.format("%.2f", price),
                color = Color.White,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun CrosshairTimeLabel(
    time: String,
    xPosition: Float,
    canvasHeight: Float
) {
    with(LocalDensity.current) {
        Box(
            modifier = Modifier
                .offset(x = (xPosition - 40.dp.toPx()).toDp(), y = (canvasHeight - 20.dp.toPx()).toDp())
                .background(Color(0xFF21262D), shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = time,
                color = Color.White,
                fontSize = 11.sp
            )
        }
    }
}