package shakir.kadakkadan.klinechart

import androidx.compose.runtime.Composable

@Composable
actual fun handleBackButton(onBackPressed: () -> Unit): () -> Unit {
    // iOS doesn't have a system back button like Android
    // The back button is handled by the UI itself
    // Return empty cleanup function
    return { }
}