package app.tinks.tink.merriam.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.tinks.tink.merriam.data.Root
import kotlinx.datetime.LocalDate

@Composable
fun RootCard(
    root: Root,
    onToggleRoot: (Boolean) -> Unit = {},
) {
    Card {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, bottom = 8.dp)) {
            Row() {
                if (root.isCompleted) {
                    Icon(Icons.Rounded.Done, contentDescription = "Done")
                }
                Text(
                    text = root.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            root.meaning?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }

            // Words List
            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 64.dp)) {
                items(items = root.words, key = { it }) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (root.isCompleted) {
                root.completeDate?.toString()?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                Button(onClick = { onToggleRoot(!root.isCompleted) }) {
                    Text(text = "Done")
                }
            }
        }
    }
}

@Preview
@Composable
private fun RootCardPreview(
    @PreviewParameter(RootRowPreviewParameterProvider::class) root: Root
) {
    RootCard(root = root)
}

private class RootRowPreviewParameterProvider : PreviewParameterProvider<Root> {
    private val rootList = listOf(
        Root(text = "BENE", meaning = "Well"),
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