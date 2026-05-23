package app.tinks.tink.merriam.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.tinks.tink.merriam.data.Root
import app.tinks.tink.ui.theme.TinkTheme

@Composable
fun RootCard(
    root: Root,
    latest: Int,
    onComplete: (Int) -> Unit = {},
) {
    var isCompleted by remember(latest) { mutableStateOf(root.id <= latest) }
    ElevatedCard(
        modifier = Modifier
            .testTag("merriam_root_${root.id}")
            .combinedClickable(
                onLongClick = { onComplete(root.id) },
                onClick = {}
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = root.text,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                AnimatedVisibility(visible = isCompleted) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Done",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            root.meaning?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat((root.words.size + 1) / 2) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        repeat(2) { columnIndex ->
                            Text(
                                text = root.words.getOrNull(rowIndex * 2 + columnIndex)
                                    ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun RootCardPreview(
    @PreviewParameter(RootRowPreviewParameterProvider::class) root: Root
) {
    TinkTheme {
        RootCard(root = root, 1)
    }
}

private class RootRowPreviewParameterProvider : PreviewParameterProvider<Root> {
    private val rootList = listOf(
        Root(
            id = 11,
            unit = 1,
            text = "BENE",
            meaning = "Well",
            words = listOf("benediction", "benefactor", "beneficiary", "benevolence"),
        ),
        Root(
            id = 12,
            unit = 1,
            text = "AM",
            meaning = "To love",
            words = listOf("amicable", "enamored", "amorous", "paramour"),
        ),
    )

    override val values = rootList.asSequence()

    override fun getDisplayName(index: Int): String? {
        // Return null or an empty string to use the default index-based name
        val root = rootList.getOrNull(index) ?: return null
        return "${root.text}}"
    }
}
