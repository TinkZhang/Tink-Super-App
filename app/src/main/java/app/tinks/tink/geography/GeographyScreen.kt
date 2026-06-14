package app.tinks.tink.geography

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.tinks.tink.ui.theme.TinkTheme
import kotlin.math.cos
import kotlin.math.sin

private enum class GeographyMode {
    Browse,
    Quiz,
}

private enum class GeographyVisualKind {
    Flag,
    Map,
}

private data class EnlargedGeographyVisual(
    val country: GeographyCountry,
    val kind: GeographyVisualKind,
)

@Composable
fun GeographyScreen() {
    val context = LocalContext.current
    val countries = remember(context) { GeographyData.loadCountries(context) }
    GeographyScreen(countries = countries)
}

@Composable
internal fun GeographyScreen(
    countries: List<GeographyCountry>,
    enableFeedback: Boolean = true,
) {
    var mode by rememberSaveable { mutableStateOf(GeographyMode.Browse) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCountryId by rememberSaveable { mutableStateOf<String?>(null) }
    var quizType by rememberSaveable { mutableStateOf(GeographyQuizType.CountryToFlag) }
    var question by remember(quizType, countries) {
        mutableStateOf(buildGeographyQuestion(countries, quizType))
    }
    var selectedAnswerId by rememberSaveable(question) { mutableStateOf<String?>(null) }
    var enlargedVisual by remember { mutableStateOf<EnlargedGeographyVisual?>(null) }

    val filteredCountries = countries.filterByCountryName(searchQuery)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4FBFF))
            .testTag("geography_screen"),
    ) {
        GeographyModeHeader(
            selectedMode = mode,
            onModeSelected = { mode = it },
        )

        when (mode) {
            GeographyMode.Browse -> {
                if (selectedCountryId == null) {
                    GeographyCountryList(
                        query = searchQuery,
                        countries = filteredCountries,
                        onQueryChange = { searchQuery = it },
                        onCountryClick = { selectedCountryId = it.id },
                    )
                } else {
                    GeographyCountryPager(
                        countries = countries,
                        selectedCountryId = selectedCountryId,
                        onSelectedCountryChanged = { selectedCountryId = it.id },
                        onBackToList = { selectedCountryId = null },
                        onVisualClick = { country, kind ->
                            enlargedVisual = EnlargedGeographyVisual(country, kind)
                        },
                    )
                }
            }

            GeographyMode.Quiz -> GeographyQuizScreen(
                countries = countries,
                quizType = quizType,
                question = question,
                selectedAnswerId = selectedAnswerId,
                enableFeedback = enableFeedback,
                onQuizTypeChange = { nextType ->
                    quizType = nextType
                    question = buildGeographyQuestion(countries, nextType)
                    selectedAnswerId = null
                },
                onAnswer = { country ->
                    if (selectedAnswerId == null) {
                        selectedAnswerId = country.id
                    }
                },
                onNextQuestion = {
                    question = buildGeographyQuestion(countries, quizType)
                    selectedAnswerId = null
                },
                onVisualClick = { country, kind ->
                    enlargedVisual = EnlargedGeographyVisual(country, kind)
                },
            )
        }
    }

    enlargedVisual?.let { visual ->
        GeographyImageDialog(
            visual = visual,
            onDismiss = { enlargedVisual = null },
        )
    }
}

@Composable
private fun GeographyModeHeader(
    selectedMode: GeographyMode,
    onModeSelected: (GeographyMode) -> Unit,
) {
    Surface(color = Color(0xFF12355B), contentColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD166)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Public,
                        contentDescription = null,
                        tint = Color(0xFF12355B),
                    )
                }
                Column {
                    Text(
                        text = "地理探险",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "看国家资料，玩四选一挑战",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFE6F4FF),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeButton(
                    label = "浏览",
                    selected = selectedMode == GeographyMode.Browse,
                    icon = Icons.Filled.Map,
                    testTag = "geography_mode_browse",
                    onClick = { onModeSelected(GeographyMode.Browse) },
                )
                ModeButton(
                    label = "测试",
                    selected = selectedMode == GeographyMode.Quiz,
                    icon = Icons.Filled.SportsEsports,
                    testTag = "geography_mode_quiz",
                    onClick = { onModeSelected(GeographyMode.Quiz) },
                )
            }
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit,
) {
    val container = if (selected) Color(0xFFFFD166) else Color(0xFFE6F4FF)
    val content = if (selected) Color(0xFF12355B) else Color(0xFF1D4E89)
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(48.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(8.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
        ),
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GeographyCountryList(
    query: String,
    countries: List<GeographyCountry>,
    onQueryChange: (String) -> Unit,
    onCountryClick: (GeographyCountry) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("geography_country_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item("search") {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("geography_search_field"),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                label = { Text("搜索国家") },
                placeholder = { Text("中国 / Japan / Brasil") },
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
        }

        if (countries.isEmpty()) {
            item("empty") {
                Text(
                    text = "没有找到这个国家",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .testTag("geography_empty_search"),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            items(countries, key = { it.id }) { country ->
                CountryListItem(
                    country = country,
                    onClick = { onCountryClick(country) },
                )
            }
        }
    }
}

@Composable
private fun CountryListItem(
    country: GeographyCountry,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("geography_country_${country.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CountryFlagArt(
                country = country,
                modifier = Modifier
                    .width(88.dp)
                    .aspectRatio(1.55f),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = country.chineseShortName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "${country.englishName} · ${country.region}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GeographyCountryPager(
    countries: List<GeographyCountry>,
    selectedCountryId: String?,
    onSelectedCountryChanged: (GeographyCountry) -> Unit,
    onBackToList: () -> Unit,
    onVisualClick: (GeographyCountry, GeographyVisualKind) -> Unit,
) {
    val initialPage = countries.indexOfFirst { it.id == selectedCountryId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { countries.size })

    LaunchedEffect(pagerState.currentPage) {
        countries.getOrNull(pagerState.currentPage)?.let(onSelectedCountryChanged)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(
                onClick = onBackToList,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("geography_back_to_list"),
            ) {
                Text("返回列表", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = "${pagerState.currentPage + 1} / ${countries.size}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D4E89),
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("geography_country_pager"),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
        ) { page ->
            CountryDetailPage(
                country = countries[page],
                onVisualClick = onVisualClick,
            )
        }
    }
}

@Composable
private fun CountryDetailPage(
    country: GeographyCountry,
    onVisualClick: (GeographyCountry, GeographyVisualKind) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("geography_detail_${country.id}"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item("title") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = country.chineseShortName,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF12355B),
                )
                Text(
                    text = country.chineseFullName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF1D4E89),
                )
                Text(
                    text = "${country.englishName} · ${country.localName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        item("visuals") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DetailVisualCard(
                    title = "国旗",
                    testTag = "geography_detail_flag_${country.id}",
                    modifier = Modifier.weight(1f),
                    onClick = { onVisualClick(country, GeographyVisualKind.Flag) },
                ) {
                    CountryFlagArt(
                        country = country,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.55f),
                    )
                }
                DetailVisualCard(
                    title = "地图位置",
                    testTag = "geography_detail_map_${country.id}",
                    modifier = Modifier.weight(1f),
                    onClick = { onVisualClick(country, GeographyVisualKind.Map) },
                ) {
                    CountryMapArt(
                        country = country,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                    )
                }
            }
        }

        item("facts") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FactRow("首都", country.capital)
                FactRow("人口", country.population)
                FactRow("面积", country.area)
                FactRow("货币", country.currency)
                FactRow("地区", country.region)
            }
        }
    }
}

@Composable
private fun DetailVisualCard(
    title: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = label,
                modifier = Modifier.width(64.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1D4E89),
            )
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun GeographyQuizScreen(
    countries: List<GeographyCountry>,
    quizType: GeographyQuizType,
    question: GeographyQuestion,
    selectedAnswerId: String?,
    enableFeedback: Boolean,
    onQuizTypeChange: (GeographyQuizType) -> Unit,
    onAnswer: (GeographyCountry) -> Unit,
    onNextQuestion: () -> Unit,
    onVisualClick: (GeographyCountry, GeographyVisualKind) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val toneGenerator = remember(enableFeedback) {
        if (enableFeedback) ToneGenerator(AudioManager.STREAM_MUSIC, 80) else null
    }

    DisposableEffect(toneGenerator) {
        onDispose { toneGenerator?.release() }
    }

    fun submit(country: GeographyCountry) {
        if (selectedAnswerId != null) return
        onAnswer(country)
        if (enableFeedback) {
            val correct = country.id == question.answer.id
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            toneGenerator?.startTone(
                if (correct) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK,
                180,
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("geography_quiz_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item("types") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "选择挑战",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF12355B),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GeographyQuizType.entries.take(2).forEach { type ->
                        QuizTypeChip(
                            type = type,
                            selected = type == quizType,
                            onClick = { onQuizTypeChange(type) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GeographyQuizType.entries.drop(2).forEach { type ->
                        QuizTypeChip(
                            type = type,
                            selected = type == quizType,
                            onClick = { onQuizTypeChange(type) },
                        )
                    }
                }
            }
        }

        item("question") {
            QuizPromptCard(
                question = question,
                selectedAnswerId = selectedAnswerId,
                onVisualClick = onVisualClick,
            )
        }

        item("options") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                question.options.chunked(2).forEach { rowCountries ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowCountries.forEach { country ->
                            GeographyAnswerOption(
                                country = country,
                                question = question,
                                selectedAnswerId = selectedAnswerId,
                                modifier = Modifier.weight(1f),
                                onClick = { submit(country) },
                            )
                        }
                        if (rowCountries.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item("next") {
            FilledTonalButton(
                onClick = onNextQuestion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("geography_next_question"),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = if (selectedAnswerId == null) "换一题" else "下一题",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }

    @Suppress("UNUSED_VARIABLE")
    val keepCountriesStable = countries
}

@Composable
private fun QuizTypeChip(
    type: GeographyQuizType,
    selected: Boolean,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = type.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        leadingIcon = {
            Icon(
                if (type == GeographyQuizType.CountryToFlag || type == GeographyQuizType.FlagToCountry) {
                    Icons.Filled.Flag
                } else {
                    Icons.Filled.Map
                },
                contentDescription = null,
            )
        },
        modifier = Modifier.testTag("geography_quiz_type_${type.name}"),
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            containerColor = if (selected) Color(0xFFFFD166) else Color.White,
            labelColor = Color(0xFF12355B),
            leadingIconContentColor = Color(0xFF1D4E89),
        ),
        border = androidx.compose.material3.AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = if (selected) Color(0xFFEF476F) else Color(0xFF9BC9E8),
        ),
    )
}

@Composable
private fun QuizPromptCard(
    question: GeographyQuestion,
    selectedAnswerId: String?,
    onVisualClick: (GeographyCountry, GeographyVisualKind) -> Unit,
) {
    val isAnswered = selectedAnswerId != null
    val isCorrect = selectedAnswerId == question.answer.id

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("geography_quiz_prompt"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = question.promptText(),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color(0xFF12355B),
                textAlign = TextAlign.Center,
            )

            when (question.type) {
                GeographyQuizType.MapToCountry -> CountryMapArt(
                    country = question.answer,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .aspectRatio(1f)
                        .clickable {
                            onVisualClick(question.answer, GeographyVisualKind.Map)
                        }
                        .testTag("geography_prompt_map"),
                )

                GeographyQuizType.FlagToCountry -> CountryFlagArt(
                    country = question.answer,
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .aspectRatio(1.55f)
                        .clickable {
                            onVisualClick(question.answer, GeographyVisualKind.Flag)
                        }
                        .testTag("geography_prompt_flag"),
                )

                else -> Unit
            }

            AnimatedVisibility(visible = isAnswered) {
                val text = if (isCorrect) {
                    "答对了！就是 ${question.answer.chineseShortName}"
                } else {
                    "差一点，答案是 ${question.answer.chineseShortName}"
                }
                val color = if (isCorrect) Color(0xFF06A77D) else Color(0xFFEF476F)
                Text(
                    text = text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("geography_answer_feedback"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = color,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun GeographyQuestion.promptText(): String = when (type) {
    GeographyQuizType.CountryToFlag -> "${answer.chineseShortName} 的国旗是哪一个？"
    GeographyQuizType.CountryToMap -> "${answer.chineseShortName} 在地图上的位置是哪一个？"
    GeographyQuizType.MapToCountry -> "地图上标出的是哪个国家？"
    GeographyQuizType.FlagToCountry -> "这面国旗属于哪个国家？"
}

@Composable
private fun GeographyAnswerOption(
    country: GeographyCountry,
    question: GeographyQuestion,
    selectedAnswerId: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val answered = selectedAnswerId != null
    val isCorrectOption = country.id == question.answer.id
    val isSelected = country.id == selectedAnswerId
    val targetColor = when {
        answered && isCorrectOption -> Color(0xFFD9FBE8)
        answered && isSelected -> Color(0xFFFFDDE6)
        else -> Color.White
    }
    val borderColor = when {
        answered && isCorrectOption -> Color(0xFF06A77D)
        answered && isSelected -> Color(0xFFEF476F)
        else -> Color(0xFF9BC9E8)
    }
    val containerColor by animateColorAsState(targetColor, label = "answer-color")
    val scale by animateFloatAsState(
        targetValue = if (answered && isCorrectOption) 1.04f else if (answered && isSelected) 0.97f else 1f,
        label = "answer-scale",
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .height(160.dp)
            .border(3.dp, borderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !answered, role = Role.Button, onClick = onClick)
            .testTag("geography_answer_${country.id}"),
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (question.type) {
                GeographyQuizType.CountryToFlag -> CountryFlagArt(
                    country = country,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.55f),
                )

                GeographyQuizType.CountryToMap -> CountryMapArt(
                    country = country,
                    modifier = Modifier
                        .fillMaxHeight(0.78f)
                        .aspectRatio(1f),
                )

                GeographyQuizType.MapToCountry,
                GeographyQuizType.FlagToCountry -> {
                    Text(
                        text = country.chineseShortName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF12355B),
                    )
                    Text(
                        text = country.region,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun GeographyImageDialog(
    visual: EnlargedGeographyVisual,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE071421))
                .padding(16.dp)
                .testTag("geography_image_dialog"),
            contentAlignment = Alignment.Center,
        ) {
            if (visual.kind == GeographyVisualKind.Flag) {
                CountryFlagArt(
                    country = visual.country,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.55f),
                )
            } else {
                CountryMapArt(
                    country = visual.country,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(56.dp)
                    .testTag("geography_image_close"),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "关闭大图",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CountryFlagArt(
    country: GeographyCountry,
    modifier: Modifier = Modifier,
) {
    val handDrawnFlagIds = remember {
        setOf("chn", "jpn", "usa", "can", "bra", "aus", "fra", "deu", "ind", "egy", "zaf", "gbr")
    }

    if (country.id in handDrawnFlagIds) {
        Canvas(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(2.dp, Color(0x33000000), RoundedCornerShape(8.dp)),
        ) {
            drawFlag(country.id)
        }
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(2.dp, Color(0x33000000), RoundedCornerShape(8.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = country.flagEmoji.ifBlank { country.cca2 },
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CountryMapArt(
    country: GeographyCountry,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE6F4FF)),
    ) {
        drawGlobe(country)
    }
}

private fun DrawScope.drawFlag(countryId: String) {
    when (countryId) {
        "chn" -> drawChinaFlag()
        "jpn" -> {
            drawRect(Color.White)
            drawCircle(Color(0xFFBC002D), radius = size.minDimension * 0.18f, center = center)
        }

        "usa" -> drawUnitedStatesFlag()
        "can" -> drawCanadaFlag()
        "bra" -> drawBrazilFlag()
        "aus" -> drawAustraliaFlag()
        "fra" -> drawVerticalTricolor(Color(0xFF0055A4), Color.White, Color(0xFFEF4135))
        "deu" -> drawHorizontalTricolor(Color.Black, Color(0xFFDD0000), Color(0xFFFFCE00))
        "ind" -> drawIndiaFlag()
        "egy" -> drawHorizontalTricolor(Color(0xFFCE1126), Color.White, Color.Black)
        "zaf" -> drawSouthAfricaFlag()
        "gbr" -> drawUnitedKingdomFlag()
        else -> drawRect(Color(0xFFE6F4FF))
    }
}

private fun DrawScope.drawChinaFlag() {
    drawRect(Color(0xFFDE2910))
    drawStar(center = Offset(size.width * 0.2f, size.height * 0.28f), radius = size.height * 0.12f, color = Color(0xFFFFDE00))
    listOf(
        Offset(size.width * 0.34f, size.height * 0.16f),
        Offset(size.width * 0.40f, size.height * 0.28f),
        Offset(size.width * 0.40f, size.height * 0.42f),
        Offset(size.width * 0.34f, size.height * 0.54f),
    ).forEach {
        drawStar(center = it, radius = size.height * 0.04f, color = Color(0xFFFFDE00))
    }
}

private fun DrawScope.drawUnitedStatesFlag() {
    val stripeHeight = size.height / 13f
    repeat(13) { index ->
        drawRect(
            color = if (index % 2 == 0) Color(0xFFB22234) else Color.White,
            topLeft = Offset(0f, stripeHeight * index),
            size = Size(size.width, stripeHeight),
        )
    }
    drawRect(Color(0xFF3C3B6E), size = Size(size.width * 0.42f, stripeHeight * 7f))
    repeat(5) { row ->
        repeat(6) { column ->
            drawCircle(
                color = Color.White,
                radius = size.height * 0.012f,
                center = Offset(size.width * (0.06f + column * 0.06f), size.height * (0.08f + row * 0.095f)),
            )
        }
    }
}

private fun DrawScope.drawCanadaFlag() {
    drawRect(Color.White)
    drawRect(Color(0xFFD52B1E), size = Size(size.width * 0.25f, size.height))
    drawRect(
        color = Color(0xFFD52B1E),
        topLeft = Offset(size.width * 0.75f, 0f),
        size = Size(size.width * 0.25f, size.height),
    )
    drawStar(center = center, radius = size.height * 0.18f, color = Color(0xFFD52B1E), points = 8)
}

private fun DrawScope.drawBrazilFlag() {
    drawRect(Color(0xFF009B3A))
    val diamond = Path().apply {
        moveTo(size.width * 0.5f, size.height * 0.12f)
        lineTo(size.width * 0.88f, size.height * 0.5f)
        lineTo(size.width * 0.5f, size.height * 0.88f)
        lineTo(size.width * 0.12f, size.height * 0.5f)
        close()
    }
    drawPath(diamond, Color(0xFFFFDF00))
    drawCircle(Color(0xFF002776), radius = size.height * 0.22f, center = center)
    drawLine(
        color = Color.White,
        start = Offset(size.width * 0.32f, size.height * 0.45f),
        end = Offset(size.width * 0.68f, size.height * 0.55f),
        strokeWidth = size.height * 0.035f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawAustraliaFlag() {
    drawRect(Color(0xFF012169))
    drawUnitedKingdomFlag(Size(size.width * 0.46f, size.height * 0.52f))
    drawStar(Offset(size.width * 0.72f, size.height * 0.32f), size.height * 0.08f, Color.White, points = 7)
    drawStar(Offset(size.width * 0.62f, size.height * 0.70f), size.height * 0.06f, Color.White, points = 7)
    drawStar(Offset(size.width * 0.82f, size.height * 0.68f), size.height * 0.05f, Color.White, points = 7)
}

private fun DrawScope.drawIndiaFlag() {
    drawHorizontalTricolor(Color(0xFFFF9933), Color.White, Color(0xFF138808))
    drawCircle(Color(0xFF000080), radius = size.height * 0.11f, center = center, style = Stroke(width = size.height * 0.018f))
    repeat(12) { index ->
        val angle = Math.toRadians((index * 30).toDouble())
        drawLine(
            color = Color(0xFF000080),
            start = center,
            end = Offset(
                center.x + cos(angle).toFloat() * size.height * 0.105f,
                center.y + sin(angle).toFloat() * size.height * 0.105f,
            ),
            strokeWidth = size.height * 0.006f,
        )
    }
}

private fun DrawScope.drawSouthAfricaFlag() {
    drawRect(Color(0xFFDE3831))
    drawRect(Color(0xFF002395), topLeft = Offset(0f, size.height * 0.5f), size = Size(size.width, size.height * 0.5f))
    val green = Path().apply {
        moveTo(0f, size.height * 0.08f)
        lineTo(size.width * 0.46f, size.height * 0.5f)
        lineTo(0f, size.height * 0.92f)
        lineTo(0f, size.height * 0.68f)
        lineTo(size.width * 0.25f, size.height * 0.5f)
        lineTo(0f, size.height * 0.32f)
        close()
    }
    drawPath(green, Color.White)
    drawPath(green, Color(0xFF007A4D))
    val black = Path().apply {
        moveTo(0f, size.height * 0.20f)
        lineTo(size.width * 0.22f, size.height * 0.5f)
        lineTo(0f, size.height * 0.80f)
        close()
    }
    drawPath(black, Color(0xFF000000))
}

private fun DrawScope.drawUnitedKingdomFlag(flagSize: Size = size) {
    val scaleX = flagSize.width / size.width
    val scaleY = flagSize.height / size.height
    fun sx(value: Float) = value * scaleX
    fun sy(value: Float) = value * scaleY
    drawRect(Color(0xFF012169), size = flagSize)
    drawLine(Color.White, Offset(0f, 0f), Offset(flagSize.width, flagSize.height), strokeWidth = sy(size.height * 0.20f))
    drawLine(Color.White, Offset(flagSize.width, 0f), Offset(0f, flagSize.height), strokeWidth = sy(size.height * 0.20f))
    drawLine(Color(0xFFC8102E), Offset(0f, 0f), Offset(flagSize.width, flagSize.height), strokeWidth = sy(size.height * 0.09f))
    drawLine(Color(0xFFC8102E), Offset(flagSize.width, 0f), Offset(0f, flagSize.height), strokeWidth = sy(size.height * 0.09f))
    drawRect(
        Color.White,
        topLeft = Offset(0f, flagSize.height * 0.38f),
        size = Size(flagSize.width, sy(size.height * 0.24f)),
    )
    drawRect(
        Color.White,
        topLeft = Offset(flagSize.width * 0.40f, 0f),
        size = Size(sx(size.width * 0.20f), flagSize.height),
    )
    drawRect(
        Color(0xFFC8102E),
        topLeft = Offset(0f, flagSize.height * 0.43f),
        size = Size(flagSize.width, sy(size.height * 0.14f)),
    )
    drawRect(
        Color(0xFFC8102E),
        topLeft = Offset(flagSize.width * 0.44f, 0f),
        size = Size(sx(size.width * 0.12f), flagSize.height),
    )
}

private fun DrawScope.drawVerticalTricolor(left: Color, middle: Color, right: Color) {
    val third = size.width / 3f
    drawRect(left, size = Size(third, size.height))
    drawRect(middle, topLeft = Offset(third, 0f), size = Size(third, size.height))
    drawRect(right, topLeft = Offset(third * 2f, 0f), size = Size(third, size.height))
}

private fun DrawScope.drawHorizontalTricolor(top: Color, middle: Color, bottom: Color) {
    val third = size.height / 3f
    drawRect(top, size = Size(size.width, third))
    drawRect(middle, topLeft = Offset(0f, third), size = Size(size.width, third))
    drawRect(bottom, topLeft = Offset(0f, third * 2f), size = Size(size.width, third))
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color, points: Int = 5) {
    val path = Path()
    val innerRadius = radius * 0.45f
    repeat(points * 2) { index ->
        val angle = -Math.PI / 2.0 + index * Math.PI / points
        val currentRadius = if (index % 2 == 0) radius else innerRadius
        val point = Offset(
            x = center.x + cos(angle).toFloat() * currentRadius,
            y = center.y + sin(angle).toFloat() * currentRadius,
        )
        if (index == 0) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawGlobe(country: GeographyCountry) {
    val radius = size.minDimension * 0.43f
    val globeCenter = center
    drawCircle(Color(0xFF75C7F0), radius = radius, center = globeCenter)
    drawCircle(Color(0xFF0B6FA4), radius = radius, center = globeCenter, style = Stroke(width = size.minDimension * 0.018f))

    listOf(-60f, -30f, 0f, 30f, 60f).forEach { latitude ->
        val y = globeCenter.y - (latitude / 90f) * radius
        val width = radius * 2f * cos(Math.toRadians(latitude.toDouble())).toFloat()
        drawOval(
            color = Color(0x55FFFFFF),
            topLeft = Offset(globeCenter.x - width / 2f, y - radius * 0.06f),
            size = Size(width, radius * 0.12f),
            style = Stroke(width = size.minDimension * 0.006f),
        )
    }

    listOf(-120f, -60f, 0f, 60f, 120f).forEach { longitude ->
        val x = globeCenter.x + (longitude / 180f) * radius
        drawOval(
            color = Color(0x55FFFFFF),
            topLeft = Offset(x - radius * 0.18f, globeCenter.y - radius),
            size = Size(radius * 0.36f, radius * 2f),
            style = Stroke(width = size.minDimension * 0.006f),
        )
    }

    drawLandBlobs(globeCenter, radius)

    val marker = Offset(
        x = globeCenter.x + (country.longitude / 180f) * radius,
        y = globeCenter.y - (country.latitude / 90f) * radius,
    )
    drawCircle(Color.White, radius = size.minDimension * 0.065f, center = marker)
    drawCircle(Color(0xFFEF476F), radius = size.minDimension * 0.045f, center = marker)
    drawCircle(Color(0xFFFFD166), radius = size.minDimension * 0.018f, center = marker)
}

private fun DrawScope.drawLandBlobs(globeCenter: Offset, radius: Float) {
    fun blob(points: List<Offset>) {
        val path = Path()
        points.forEachIndexed { index, point ->
            val mapped = Offset(globeCenter.x + point.x * radius, globeCenter.y + point.y * radius)
            if (index == 0) path.moveTo(mapped.x, mapped.y) else path.lineTo(mapped.x, mapped.y)
        }
        path.close()
        drawPath(path, Color(0xFF55B96C))
    }

    blob(listOf(Offset(-0.72f, -0.45f), Offset(-0.28f, -0.62f), Offset(-0.10f, -0.18f), Offset(-0.34f, 0.05f), Offset(-0.62f, -0.10f)))
    blob(listOf(Offset(-0.36f, 0.10f), Offset(-0.12f, 0.25f), Offset(-0.24f, 0.70f), Offset(-0.52f, 0.58f), Offset(-0.58f, 0.24f)))
    blob(listOf(Offset(-0.06f, -0.50f), Offset(0.52f, -0.56f), Offset(0.74f, -0.18f), Offset(0.40f, 0.10f), Offset(0.02f, -0.04f)))
    blob(listOf(Offset(0.24f, 0.12f), Offset(0.50f, 0.24f), Offset(0.40f, 0.66f), Offset(0.12f, 0.58f)))
    blob(listOf(Offset(0.54f, 0.48f), Offset(0.78f, 0.58f), Offset(0.68f, 0.76f), Offset(0.46f, 0.66f)))
}

@Preview(showBackground = true)
@Composable
private fun GeographyScreenPreview() {
    TinkTheme(dynamicColor = false) {
        GeographyScreen(countries = GeographyData.previewCountries, enableFeedback = false)
    }
}
