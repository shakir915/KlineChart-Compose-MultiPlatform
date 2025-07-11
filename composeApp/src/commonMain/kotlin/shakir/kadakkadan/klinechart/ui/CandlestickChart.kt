package shakir.kadakkadan.klinechart.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import shakir.kadakkadan.klinechart.model.CandleData
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class Timeframe(val displayName: String, val apiValue: String) {
    ONE_MINUTE("1m", "1m"),
    FIVE_MINUTES("5m", "5m"),
    FIFTEEN_MINUTES("15m", "15m"),
    ONE_HOUR("1h", "1h"),
    FOUR_HOURS("4h", "4h"),
    ONE_DAY("1d", "1d")
}

private fun formatDateTime(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.monthNumber.toString().padStart(2, '0')}/${localDateTime.dayOfMonth.toString().padStart(2, '0')}/${localDateTime.year} ${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
}

private fun formatDate(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.monthNumber.toString().padStart(2, '0')}/${localDateTime.dayOfMonth.toString().padStart(2, '0')}"
}

@Composable
fun CandlestickChart(
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

    // Zoom and scroll state
    var xZoom by remember { mutableStateOf(1f) }
    var yZoom by remember { mutableStateOf(1f) }
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
    
    val coroutineScope = rememberCoroutineScope()
    
    // Base dimensions
    val baseCandleWidth = 20f
    val baseCandleSpacing = 5f
    
    // Zoomed dimensions
    val candleWidth = baseCandleWidth * xZoom
    val candleSpacing = baseCandleSpacing * xZoom
    val totalWidth = candles.size * (candleWidth + candleSpacing)
    val baseChartHeight = 600f // Base height without zoom
    
    // Use stable viewport when loading historical data, otherwise use full range
    val displayMinPrice = if (useStableViewport) stableViewportMinPrice else candles.minOfOrNull { minOf(it.low, it.open, it.close, it.high) } ?: 0.0
    val displayMaxPrice = if (useStableViewport) stableViewportMaxPrice else candles.maxOfOrNull { maxOf(it.high, it.open, it.close, it.low) } ?: 100.0
    val priceRange = displayMaxPrice - displayMinPrice
    
    // Adjust position when new historical data is loaded
    LaunchedEffect(candles.size) {
        if (previousCandleCount > 0 && candles.size > previousCandleCount) {
            // Historical data was added, adjust xOffset to maintain current candle position
            val addedCandles = candles.size - previousCandleCount
            val addedWidth = addedCandles * (candleWidth + candleSpacing)
            xOffset += addedWidth
            
            // Reset loading flags
            hasRequestedHistoricalData = false
            isLoadingHistorical = false
            useStableViewport = false
        } else if (previousCandleCount == 0 && candles.isNotEmpty()) {
            // Initial load - set up stable viewport
            stableViewportMinPrice = candles.minOfOrNull { minOf(it.low, it.open, it.close, it.high) } ?: 0.0
            stableViewportMaxPrice = candles.maxOfOrNull { maxOf(it.high, it.open, it.close, it.low) } ?: 100.0
        }
        previousCandleCount = candles.size
    }
    
    // Activate stable viewport when requesting historical data
    LaunchedEffect(hasRequestedHistoricalData) {
        if (hasRequestedHistoricalData && !useStableViewport) {
            // Lock current viewport before loading historical data
            stableViewportMinPrice = displayMinPrice
            stableViewportMaxPrice = displayMaxPrice
            useStableViewport = true
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117)) // Dark background like TradingView
    ) {
        // Top bar with symbol and reset button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = symbol,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // Reset zoom button (icon only)
            Button(
                onClick = { 
                    xZoom = 1f
                    yZoom = 1f
                    xOffset = 0f
                    yOffset = 0f
                    isInitialPosition = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF656d76)),
                modifier = Modifier.size(32.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "⌂", // Zoom/Reset icon
                    fontSize = 16.sp,
                    color = Color.White
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
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                // Handle pinch zoom (X-axis only)
                                if (zoom != 1f) {
                                    val oldXZoom = xZoom
                                    val newXZoom = (xZoom * zoom).coerceIn(0.03f, 5f)
                                    
                                    // Adjust X offset to zoom towards the pinch center
                                    if (canvasSize != Size.Zero) {
                                        val xZoomRatio = newXZoom / oldXZoom
                                        xOffset = centroid.x - (centroid.x - xOffset) * xZoomRatio
                                    }
                                    
                                    xZoom = newXZoom
                                    isInitialPosition = false
                                }
                                
                                // Handle pan/drag only if crosshair is not active
                                if (pan != Offset.Zero && !isCrosshairActive) {
                                    xOffset += pan.x
                                    yOffset += pan.y
                                    isInitialPosition = false
                                    
                                    // Check if user is scrolling to the beginning of the chart (backward in time)
                                    if (pan.x > 0 && !isLoadingHistorical && !hasRequestedHistoricalData) {
                                        val scrolledFromStart = xOffset
                                        val threshold = size.width * 0.2f // Load when 20% from start
                                        
                                        if (scrolledFromStart > -threshold) {
                                            hasRequestedHistoricalData = true
                                            isLoadingHistorical = true
                                            onLoadMoreHistoricalData()
                                        }
                                    }
                                }
                                
                                // Handle crosshair dragging
                                if (pan != Offset.Zero && isCrosshairActive) {
                                    crosshairPosition = crosshairPosition?.let { 
                                        Offset(
                                            (it.x + pan.x).coerceIn(0f, size.width.toFloat()),
                                            (it.y + pan.y).coerceIn(0f, size.height.toFloat())
                                        )
                                    }
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { offset ->
                                    // Activate crosshair mode on long press
                                    isCrosshairActive = true
                                    crosshairPosition = offset
                                },
                                onTap = { 
                                    // Deactivate crosshair mode on tap
                                    if (isCrosshairActive) {
                                        isCrosshairActive = false
                                        crosshairPosition = null
                                        mousePosition = null // Also clear mouse position
                                    }
                                }
                            )
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
                            // Track mouse position for crosshair (desktop only)
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (!isCrosshairActive) {
                                        mousePosition = event.changes.firstOrNull()?.position
                                    } else {
                                        // Clear mouse position when crosshair is active
                                        mousePosition = null
                                    }
                                }
                            }
                        }
                ) {
                    // Determine if crosshair should be visible
                    // For mobile: only show when activated
                    // For desktop: show when mouse is present (but mousePosition is cleared when activated)
                    val shouldShowCrosshair = isCrosshairActive || mousePosition != null
                    
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                    ) {
                        // Update canvas size for button handlers
                        canvasSize = size
                        
                        // Calculate initial offset to show latest candles on right side with 5% margin
                        // AND position the last candle vertically centered
                        if (candles.isNotEmpty() && isInitialPosition && canvasSize != Size.Zero) {
                            val totalCandleWidth = candles.size * (candleWidth + candleSpacing)
                            val marginWidth = canvasSize.width * 0.05f
                            xOffset = canvasSize.width - totalCandleWidth - marginWidth
                            
                            // Center the chart vertically based on the last candle's price
                            val lastCandle = candles.last()
                            val lastCandlePrice = (lastCandle.high + lastCandle.low) / 2.0
                            val priceRatio = (lastCandlePrice - displayMinPrice) / priceRange
                            val scaledHeight = canvasSize.height * yZoom
                            val targetY = scaledHeight - (priceRatio * scaledHeight)
                            val centerY = canvasSize.height / 2f
                            yOffset = (centerY - targetY).toFloat()
                            
                            isInitialPosition = false
                        }
                        
                        drawCandlestickChart(
                            candles = candles,
                            candleWidth = candleWidth,
                            candleSpacing = candleSpacing,
                            minPrice = displayMinPrice,
                            maxPrice = displayMaxPrice,
                            priceRange = priceRange,
                            chartHeight = baseChartHeight,
                            xOffset = xOffset,
                            yOffset = yOffset,
                            xZoom = xZoom,
                            yZoom = yZoom
                        )
                        
                        // Draw LTP (Last Traded Price) line
                        if (candles.isNotEmpty()) {
                            val lastPrice = candles.last().close
                            drawLTPLine(
                                price = lastPrice,
                                minPrice = displayMinPrice,
                                maxPrice = displayMaxPrice,
                                priceRange = priceRange,
                                yOffset = yOffset,
                                yZoom = yZoom
                            )
                        }
                        
                        // Draw crosshair - only show when should be visible
                        if (shouldShowCrosshair) {
                            val currentCrosshairPosition = if (isCrosshairActive) crosshairPosition else mousePosition
                            currentCrosshairPosition?.let { pos ->
                                drawCrosshair(pos)
                            }
                        }
                    }
                    
                    // Crosshair overlays - only show when crosshair should be visible
                    if (shouldShowCrosshair) {
                        val currentCrosshairPosition = if (isCrosshairActive) crosshairPosition else mousePosition
                        currentCrosshairPosition?.let { pos ->
                            if (canvasSize != Size.Zero) {
                                // Calculate time at X position
                                val adjustedX = pos.x - xOffset
                                val candleIndex = (adjustedX / (candleWidth + candleSpacing)).toInt()
                                
                                // Only show time label when hovering over a valid candle
                                if (candleIndex >= 0 && candleIndex < candles.size) {
                                    val candle = candles[candleIndex]
                                    val timeText = formatDateTime(candle.openTime)
                                    
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
            Box {
                AlternativePriceBar(
                    minPrice = displayMinPrice,
                    maxPrice = displayMaxPrice,
                    chartHeight = baseChartHeight,
                    yOffset = yOffset,
                    yZoom = yZoom,
                    isPriceBarClicked = false, // Not used anymore
                    onPriceBarClick = { }, // Not used anymore
                    onYZoomChange = { newZoom ->  yZoom = newZoom.coerceIn(0.1f, 5f) },
                    modifier = Modifier
                        .width(60.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF21262D))
                )
                
                // LTP Price Label on Price Bar
                if (candles.isNotEmpty() && canvasSize != Size.Zero) {
                    val lastPrice = candles.last().close
                    val scaledHeight = canvasSize.height * yZoom
                    val priceRatio = (lastPrice - displayMinPrice) / priceRange
                    val ltpY = (scaledHeight - (priceRatio * scaledHeight)).toFloat() + yOffset
                    
                    LTPPriceLabel(
                        price = lastPrice,
                        yPosition = ltpY
                    )
                }
                
                // Crosshair Price Label on Price Bar (similar to LTP)
                val shouldShowCrosshair = isCrosshairActive || mousePosition != null
                if (shouldShowCrosshair && canvasSize != Size.Zero) {
                    val currentCrosshairPosition = if (isCrosshairActive) crosshairPosition else mousePosition
                    currentCrosshairPosition?.let { pos ->
                        val scaledHeight = canvasSize.height * yZoom
                        val adjustedY = pos.y - yOffset
                        val priceRatio = 1.0 - (adjustedY / scaledHeight).toDouble()
                        val currentPrice = displayMinPrice + (priceRatio * priceRange)
                        
                        CrosshairPriceBarLabel(
                            price = currentPrice,
                            yPosition = pos.y
                        )
                    }
                }
            }
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
fun AlternativePriceBar(
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
    // Create a reference to the current zoom that updates
    val currentZoom by rememberUpdatedState(yZoom)

    val priceRange = maxPrice - minPrice
    val priceSteps = 12

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
                    val dragY = dragAmount.y
                    println("Alternative PriceBar drag: dragY=$dragY, currentZoom=${currentZoom}")

                    // Apply zoom immediately on any significant drag
                    if (abs(dragY) > 2f) {
                        val zoomDelta = if (dragY < 0) {
                            0.05f // Zoom in (drag up)
                        } else {
                            -0.05f // Zoom out (drag down)
                        }

                        val newZoom = currentZoom + zoomDelta
                        println("Alternative PriceBar calling onYZoomChange with: $newZoom")
                        onYZoomChange(newZoom)
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
                    text = "$${price.toInt()}",
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
    // Format dates using multiplatform function
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
                .fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(timeSteps) { index ->
                val candleIndex = if (visibleCandleCount > 1) {
                    startCandleIndex + (visibleCandleCount * index / (timeSteps - 1))
                } else {
                    startCandleIndex
                }.coerceIn(0, candles.size - 1)
                
                val candle = candles[candleIndex]
                
                Text(
                    text = formatDate(candle.openTime),
                    color = Color(0xFF8B949E),
                    fontSize = 11.sp
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
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
    )
    
    // Vertical line
    drawLine(
        color = crosshairColor,
        start = Offset(mousePosition.x, 0f),
        end = Offset(mousePosition.x, size.height),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
    )
}

private fun DrawScope.drawLTPLine(
    price: Double,
    minPrice: Double,
    maxPrice: Double,
    priceRange: Double,
    yOffset: Float,
    yZoom: Float
) {
    // Calculate Y position for LTP line
    val scaledHeight = size.height * yZoom
    val priceRatio = (price - minPrice) / priceRange
    val ltpY = (scaledHeight - (priceRatio * scaledHeight)).toFloat() + yOffset
    
    // Draw LTP line in yellow
    val ltpColor = Color(0xFFFFD700) // Gold/Yellow color
    
    drawLine(
        color = ltpColor,
        start = Offset(0f, ltpY),
        end = Offset(size.width, ltpY),
        strokeWidth = 1.dp.toPx(), // Same thickness as crosshair
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)) // Same dash pattern as crosshair
    )
}

@Composable
fun CrosshairPriceLabel(
    price: Double,
    yPosition: Float,
    canvasWidth: Float,
    isInsidePriceBar: Boolean = false
) {
    with(LocalDensity.current) {
        Box(
            modifier = Modifier
                .offset(x = (canvasWidth - 60.dp.toPx()).toDp(), y = (yPosition - 10.dp.toPx()).toDp())
                .background(Color(0xFF21262D), shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${(price * 100).toInt() / 100.0}",
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

@Composable
fun TimeframeSelector(
    selectedTimeframe: Timeframe,
    onTimeframeSelected: (Timeframe) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            Text(
                text = "Timeframe:",
                fontSize = 12.sp,
                color = Color(0xFF8B949E),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        items(Timeframe.entries) { timeframe ->
            TimeframeButton(
                timeframe = timeframe,
                isSelected = timeframe == selectedTimeframe,
                onClick = { onTimeframeSelected(timeframe) }
            )
        }
    }
}

@Composable
fun TimeframeButton(
    timeframe: Timeframe,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF238636) else Color(0xFF21262D),
            contentColor = if (isSelected) Color.White else Color(0xFF8B949E)
        ),
        modifier = Modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = timeframe.displayName,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun LTPPriceLabel(
    price: Double,
    yPosition: Float
) {
    with(LocalDensity.current) {
        Box(
            modifier = Modifier
                .offset(x = 5.dp, y = (yPosition - 8.dp.toPx()).toDp())
                .background(
                    Color(0xFFFFD700).copy(alpha = 0.9f), // Gold/Yellow background
                    shape = RoundedCornerShape(3.dp)
                )
                .padding(horizontal = 4.dp, vertical = 1.dp) // Reduced padding
        ) {
            Text(
                text = "${(price * 100).toInt() / 100.0}",
                color = Color.Black, // Black text on yellow background
                fontSize = 9.sp // Reduced font size
            )
        }
    }
}

@Composable
fun CrosshairPriceBarLabel(
    price: Double,
    yPosition: Float
) {
    with(LocalDensity.current) {
        Box(
            modifier = Modifier
                .offset(x = 5.dp, y = (yPosition - 8.dp.toPx()).toDp())
                .background(
                    Color(0xFF21262D), // Same color as original crosshair
                    shape = RoundedCornerShape(3.dp)
                )
                .padding(horizontal = 4.dp, vertical = 1.dp) // Reduced padding
        ) {
            Text(
                text = "${(price * 100).toInt() / 100.0}",
                color = Color.White,
                fontSize = 9.sp // Reduced font size
            )
        }
    }
}
