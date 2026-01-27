package app.tinks.tink.merriam.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.tinks.tink.merriam.data.Root
import app.tinks.tink.ui.theme.TinkTheme
import kotlinx.datetime.LocalDate

@Composable
fun RootCard(
    root: Root,
    onToggleRoot: (Boolean) -> Unit = {},
) {
    var isCompleted by remember { mutableStateOf(root.isCompleted) }
    ElevatedCard() {
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
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            root.meaning?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            // Words List
            LazyVerticalGrid(
                modifier = Modifier.fillMaxWidth(),
                columns = GridCells.Adaptive(minSize = 128.dp)
            ) {
                items(items = root.words, key = { it }) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            AnimatedContent(
                targetState = isCompleted,
                modifier = Modifier.align(Alignment.End)
            ) {
                if (it) {
                    Text(
                        text = root.completeDate.toString(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Button(
                        onClick = { onToggleRoot(true) },
                    ) {
                        Text(text = "Done")
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
    TinkTheme() {
        RootCard(root = root)
    }
}

private class RootRowPreviewParameterProvider : PreviewParameterProvider<Root> {
    private val rootList = listOf(
        Root(
            text = "BENE",
            meaning = "Well",
            words = listOf("benediction", "benefactor", "beneficiary", "benevolence")
        ),
        Root(
            text = "AM",
            meaning = "To love",
            words = listOf("amicable", "enamored", "amorous", "paramour"),
            isCompleted = true,
            completeDate = LocalDate(year = 2026, month = 1, day = 26)
        ),
    )

    override val values = rootList.asSequence()

    override fun getDisplayName(index: Int): String? {
        // Return null or an empty string to use the default index-based name
        val root = rootList.getOrNull(index) ?: return null
        return "${root.text} - ${root.isCompleted}"
    }
}