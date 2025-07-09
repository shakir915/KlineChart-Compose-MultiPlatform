# KlineChart Compose Multiplatform

A cross-platform cryptocurrency trading chart application built with Kotlin Multiplatform and Compose Multiplatform, featuring real-time market data from Binance API.

## 🌟 Features

- **Real-time Market Data**: Displays live cryptocurrency prices and trading data from Binance
- **Interactive Candlestick Charts**: Visualize price movements with customizable timeframes (1m, 5m, 15m, 1h, 4h, 1d, 1w)
- **Market Overview**: Browse USDT trading pairs with volume sorting and search functionality
- **Infinite Scroll**: Load historical chart data seamlessly
- **Interactive Crosshair**: Hover over charts to see detailed price information
- **Dark Theme**: Modern dark UI optimized for trading

## 📱 Supported Platforms

- **Android** - Native Android app
- **iOS** - Native iOS app  
- **Desktop** - Cross-platform desktop application (Windows, macOS, Linux)

## 🏗️ Architecture

This is a Kotlin Multiplatform project with the following structure:

* `/composeApp` - Shared code across all platforms
  - `commonMain` - Common business logic, UI components, and API integration
  - `androidMain` - Android-specific implementations
  - `iosMain` - iOS-specific implementations  
  - `desktopMain` - Desktop-specific implementations

* `/iosApp` - iOS app entry point and SwiftUI integration

## 🚀 Getting Started

### Prerequisites
- Android Studio or IntelliJ IDEA with Kotlin Multiplatform plugin
- Xcode (for iOS development)
- JDK 11 or higher

### Running the App

**Android:**
```bash
./gradlew :composeApp:installDebug
```

**iOS:**
Open `iosApp/iosApp.xcodeproj` in Xcode and run

**Desktop:**
```bash
./gradlew :composeApp:run
```

## 🛠️ Technical Stack

- **Kotlin Multiplatform** - Cross-platform development
- **Compose Multiplatform** - UI framework
- **Ktor** - HTTP client for API calls
- **Kotlinx Serialization** - JSON parsing
- **Material 3** - UI components and theming
- **Binance API** - Real-time cryptocurrency data

## 📊 API Integration

The app integrates with Binance public API endpoints:
- `/api/v3/klines` - Historical candlestick data
- `/api/v3/ticker/24hr` - 24-hour ticker statistics

## 🔮 TODO / Future Enhancements

### Core Features
- [ ] Add more cryptocurrency exchanges (Coinbase, Kraken, etc.)
- [ ] Implement technical indicators (RSI, MACD, Bollinger Bands)
- [ ] Add price alerts and notifications
- [ ] Portfolio tracking functionality
- [ ] Favorites/Watchlist management

### UI/UX Improvements
- [ ] Add light theme support
- [ ] Implement pull-to-refresh on home screen
- [ ] Add loading states and error handling UI
- [ ] Improve chart responsiveness on mobile devices
- [ ] Add chart export functionality (PNG/PDF)

### Technical Enhancements
- [ ] Add offline caching with local database
- [ ] Implement WebSocket for real-time updates
- [ ] Add unit and integration tests
- [ ] Optimize chart rendering performance
- [ ] Add CI/CD pipeline
- [ ] Implement proper error handling and retry logic

### Platform-Specific
- [ ] Add Android widgets for price tracking
- [ ] Implement iOS shortcuts and Siri integration
- [ ] Add desktop system tray integration
- [ ] Support for different screen sizes and orientations

### Security & Performance
- [ ] Add API rate limiting handling
- [ ] Implement request caching strategy
- [ ] Add network connectivity detection
- [ ] Optimize memory usage for large datasets

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

## 🔗 Learn More

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Binance API Documentation](https://binance-docs.github.io/apidocs/spot/en/)