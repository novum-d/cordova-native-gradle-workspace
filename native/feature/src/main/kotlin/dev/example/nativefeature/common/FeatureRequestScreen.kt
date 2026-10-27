package dev.example.nativefeature.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject

/** 画面名・要求と、各機能が渡す結果表示を中央に配置する共通レイアウト。 */
@Composable
internal fun FeatureRequestScreen(
    title: String,
    id: String,
    args: JsonObject?,
    onBack: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title)
        Text(text = "処理ID: $id")
        Text(text = "args: ${args ?: "未指定"}")
        content()
        Button(onClick = onBack) {
            Text(text = "戻る")
        }
    }
}
