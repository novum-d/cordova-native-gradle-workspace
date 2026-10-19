package dev.example.nativefeature

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ComposeRuntimeCheck()
            }
        }
    }
}

@Composable
private fun ComposeRuntimeCheck() {
    var clicks by remember { mutableIntStateOf(0) }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Compose runtime check", style = MaterialTheme.typography.headlineSmall)
            Text(text = "Cordova APK / native feature")
            Text(text = "Clicks: $clicks")
            Button(onClick = { clicks++ }) {
                Text(text = "Check recomposition")
            }
        }
    }
}
