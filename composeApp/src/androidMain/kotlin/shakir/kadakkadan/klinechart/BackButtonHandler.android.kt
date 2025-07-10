package shakir.kadakkadan.klinechart

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun handleBackButton(onBackPressed: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }
    }
    
    return remember {
        val activity = context as? ComponentActivity
        if (activity != null) {
            activity.onBackPressedDispatcher.addCallback(callback)
        }
        
        // Return cleanup function
        { callback.remove() }
    }
}