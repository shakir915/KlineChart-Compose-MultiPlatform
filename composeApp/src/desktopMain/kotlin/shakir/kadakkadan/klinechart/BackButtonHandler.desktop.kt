package shakir.kadakkadan.klinechart

import androidx.compose.runtime.Composable

@Composable
actual fun handleBackButton(onBackPressed: () -> Unit): () -> Unit {
    // Desktop doesn't have a system back button
    // Return empty cleanup function
    return { }
}