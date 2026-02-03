package app.tinks.tink.merriam.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import app.tinks.tink.merriam.data.Root
import app.tinks.tink.merriam.data.Unit
import app.tinks.tink.ui.theme.TinkTheme
import kotlinx.datetime.LocalDate

@Composable
fun UnitItem(
    unit: Unit,
    onRootComplete: (Int) -> kotlin.Unit = {},
) {
    var isExpanded by remember { mutableStateOf(unit.isExpanded) }
    val rotation by animateFloatAsState(if (isExpanded) 0f else 180f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(if (unit.roots.any { !it.isCompleted }) 4.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Unit ${unit.id}",
                    style = MaterialTheme.typography.titleLarge,
                )
                if (unit.roots.all { it.isCompleted }) {
                    Icon(
                        Icons.Filled.Done,
                        contentDescription = "Unit Done",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        unit.roots.map {
                            AnimatedContent(it.isCompleted) { completed ->
                                if (completed) {
                                    Icon(
                                        Icons.Filled.Circle,
                                        contentDescription = "Root Done",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                } else {
                                    Icon(
                                        Icons.Outlined.Circle,
                                        contentDescription = "Root Todo",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    tint = MaterialTheme.colorScheme.secondary,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation)
                )
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    repeat(unit.roots.size / 2) { rowIndex ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            repeat(2) { columnIndex ->
                                val root = unit.roots.getOrNull(rowIndex * 2 + columnIndex)
                                Text(
                                    text = root?.text
                                        ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (root?.isCompleted == true) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Unspecified
                                    },
                                    modifier = Modifier.weight(1.0f)
                                )
                            }
                        }
                    }
                    Text(
                        text = unit.roots.last().text,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    repeat(unit.roots.size) { index ->
                        RootCard(root = unit.roots[index], onComplete = onRootComplete)
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun UnitItemPreview(
    @PreviewParameter(UnitPreviewParameterProvider::class) unit: Unit
) {
    TinkTheme {
        UnitItem(unit)
    }
}

private class UnitPreviewParameterProvider : PreviewParameterProvider<Unit> {
    private val rootList = listOf(
        Root(
            unit = 1,
            id = 11,
            text = "BENE",
            meaning = "Well",
            words = listOf("benediction", "benefactor", "beneficiary", "benevolence")
        ),
        Root(
            unit = 1,
            id = 12,
            text = "AM",
            meaning = "To love",
            words = listOf("amicable", "enamored", "amorous", "paramour"),
            isCompleted = true,
            completeDate = LocalDate(year = 2026, month = 1, day = 26)
        ),
    )

    private val units = listOf(
        Unit(id = 1, roots = rootList, isExpanded = true),
        Unit(id = 2, roots = rootList, isExpanded = false),
        Unit(id = 3, roots = rootList, isExpanded = true),
    )

    override val values = units.asSequence()

    override fun getDisplayName(index: Int): String {
        return "${units[index].id}"
    }
}
