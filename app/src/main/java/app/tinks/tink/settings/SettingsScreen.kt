package app.tinks.tink.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.zi.StorySettingsDialog

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        storyLength = uiState.storyLength,
        apiEnvironment = uiState.apiEnvironment,
        showStoryLengthDialog = uiState.showStoryLengthDialog,
        onOpenStoryLength = viewModel::openStoryLengthDialog,
        onDismissStoryLength = viewModel::dismissStoryLengthDialog,
        onUpdateStoryLength = viewModel::updateStoryLength,
        onUpdateApiEnvironment = viewModel::updateApiEnvironment,
    )
}

@Composable
private fun SettingsScreen(
    storyLength: Int,
    apiEnvironment: ApiEnvironment,
    showStoryLengthDialog: Boolean,
    onOpenStoryLength: () -> Unit,
    onDismissStoryLength: () -> Unit,
    onUpdateStoryLength: (Int) -> Unit,
    onUpdateApiEnvironment: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "偏好设置",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        ListItem(
            headlineContent = { Text("开发 API") },
            supportingContent = {
                Text(
                    if (apiEnvironment == ApiEnvironment.Dev) {
                        "当前使用 Tink-Dev 数据库"
                    } else {
                        "当前使用生产数据库"
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            trailingContent = {
                Switch(
                    checked = apiEnvironment == ApiEnvironment.Dev,
                    onCheckedChange = onUpdateApiEnvironment,
                )
            },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        ListItem(
            headlineContent = { Text("故事长度") },
            supportingContent = { Text("$storyLength") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenStoryLength() }
                .padding(horizontal = 8.dp),
            trailingContent = { Text("修改") },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        if (showStoryLengthDialog) {
            StorySettingsDialog(
                currentLength = storyLength,
                onDismiss = onDismissStoryLength,
                onConfirm = onUpdateStoryLength,
            )
        }
    }
}
