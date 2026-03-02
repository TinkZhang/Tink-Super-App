package app.tinks.tink.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
        showStoryLengthDialog = uiState.showStoryLengthDialog,
        onOpenStoryLength = viewModel::openStoryLengthDialog,
        onDismissStoryLength = viewModel::dismissStoryLengthDialog,
        onUpdateStoryLength = viewModel::updateStoryLength,
    )
}

@Composable
private fun SettingsScreen(
    storyLength: Int,
    showStoryLengthDialog: Boolean,
    onOpenStoryLength: () -> Unit,
    onDismissStoryLength: () -> Unit,
    onUpdateStoryLength: (Int) -> Unit,
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
            headlineContent = { Text("故事长度") },
            supportingContent = { Text("$storyLength") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenStoryLength() }
                .padding(horizontal = 8.dp),
            trailingContent = { Text("修改") },
        )
        Divider(modifier = Modifier.padding(horizontal = 16.dp))

        if (showStoryLengthDialog) {
            StorySettingsDialog(
                currentLength = storyLength,
                onDismiss = onDismissStoryLength,
                onConfirm = onUpdateStoryLength,
            )
        }
    }
}
