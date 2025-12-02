package app.tinks.tink.haircutcut

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tinks.tink.haircut.HaircutEvent
import app.tinks.tink.haircut.HaircutViewModel
import app.tinks.tink.haircut.data.Haircut
import kotlinx.datetime.LocalDate

@Composable
fun HaircutScreen(viewModel: HaircutViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HaircutScreen(
        days = uiState.days,
        history = uiState.history,
        isLoading = uiState.isLoading,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun HaircutScreen(
    days: Int,
    history: List<Haircut>,
    isLoading: Boolean = false,
    onEvent: (HaircutEvent) -> Unit = {},
) {

    LaunchedEffect(Unit) {
        onEvent(HaircutEvent.RefreshHaircutList)
    }

    val (statusColor, containerColor, onContainerColor, statusText) = when {
        days > 35 -> Quadruple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "该去剪头了！"
        )

        days > 21 -> Quadruple(
            // 使用自定义的黄色或 Tertiary
            Color(0xFFE6AE26), // 偏暖黄
            Color(0xFFFFF6E0),
            Color(0xFF5C4600),
            "发型稍微有点乱了"
        )

        else -> Quadruple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "发型保持得不错"
        )
    }
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(HaircutEvent.AddHaircutFabClick) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Filled.Add, "添加记录")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // 1. Hero Card: 距离上次天数
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = containerColor,
                        contentColor = onContainerColor
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCut,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .alpha(0.8f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "$days",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 80.sp
                            )
                        )
                        Text(
                            text = "天没理发了",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Spacer(Modifier.height(12.dp))

                        // 状态胶囊
                        Surface(
                            color = statusColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                statusColor.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (days > 21) {
                                    Icon(
                                        Icons.Outlined.Warning,
                                        null,
                                        tint = statusColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                } else {
                                    Icon(
                                        Icons.Outlined.Face,
                                        null,
                                        tint = statusColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = statusColor
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "理发历史",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // 3. 历史记录 List
            items(history) { record ->
                ListItem(
                    headlineContent = {
                        Text(record.shopName, style = MaterialTheme.typography.titleMedium)
                    },
                    supportingContent = {
                        Text(
                            record.date.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Text(
                            "¥${record.price.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { }
                )
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Preview
@Composable
private fun HaircutScreenPreview() {
    HaircutScreen(
        days = 10, history = listOf(
            Haircut(1, 10, LocalDate(2023, 1, 1), "理发店"),
            Haircut(2, 20, LocalDate(2024, 1, 1), "理发店"),
            Haircut(3, 25, LocalDate(2025, 1, 1), "理发店")
        )
    )
}