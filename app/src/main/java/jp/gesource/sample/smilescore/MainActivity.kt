package jp.gesource.sample.smilescore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import jp.gesource.sample.smilescore.ui.screen.SmileScoreScreen
import jp.gesource.sample.smilescore.ui.theme.SmileScoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmileScoreTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SmileScoreScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
