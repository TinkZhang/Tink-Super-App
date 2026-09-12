package app.tinks.tink.random

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomSettingsScreen(settings: RandomSettings, onChange: (RandomSettings) -> Unit = {}, onEditWheel: () -> Unit = {}, onPlay: () -> Unit = {}, speechUnavailable: Boolean = false) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).testTag("random_settings"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                RandomType.entries.forEachIndexed { index, type ->
                    SegmentedButton(selected = settings.type == type, onClick = { onChange(settings.copy(type = type)) }, shape = SegmentedButtonDefaults.itemShape(index, 3), modifier = Modifier.testTag("random_type_${type.name.lowercase()}")) { Text(type.label) }
                }
            }
            when (settings.type) {
                RandomType.Dice -> NumberControl("骰子数量", settings.diceCount, 1..6, "random_dice", { onChange(settings.copy(diceCount = it)) })
                RandomType.Wheel -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(settings.wheelName, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onEditWheel, modifier = Modifier.testTag("random_edit_wheel")) { Text("编辑") }
                }
                RandomType.Coin -> Text("硬币", style = MaterialTheme.typography.titleMedium)
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge) {
                Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
                    RandomBackdrop(settings.type, Modifier.matchParentSize())
                    RandomObjects(settings, RandomResult(dice = List(settings.diceCount) { (it + 2) % 6 + 1 }), modifier = Modifier.fillMaxSize().padding(20.dp))
                }
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("音效"); Spacer(Modifier.width(8.dp)); Switch(settings.sound, { onChange(settings.copy(sound = it)) }, Modifier.testTag("random_sound")) }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("中文播报"); Spacer(Modifier.width(8.dp)); Switch(settings.speech, { onChange(settings.copy(speech = it)) }, Modifier.testTag("random_speech")) }
            }
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("random_start")) { Text(settings.type.action) }
            if (settings.speech && speechUnavailable) Text("本机中文语音不可用", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NumberControl(label: String, value: Int, bounds: IntRange, tag: String, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onChange(value - 1) }, enabled = value > bounds.first, modifier = Modifier.testTag("${tag}_minus")) { Icon(Icons.Default.Remove, "减少$label") }
        Text(value.toString(), Modifier.padding(horizontal = 8.dp).testTag("${tag}_count"))
        IconButton(onClick = { onChange(value + 1) }, enabled = value < bounds.last, modifier = Modifier.testTag("${tag}_plus")) { Icon(Icons.Default.Add, "增加$label") }
    }
}

internal data class WheelDraft(val name: String, val rows: List<Pair<String, String>>, val equalSections: Boolean = false) {
    fun valid() = name.isNotBlank() && name.length <= 20 && rows.size in 2..12 && rows.all { (name, weight) -> name.isNotBlank() && name.length <= 16 && weight.toIntOrNull() in 1..100 }
    fun sections() = rows.map { WheelSection(it.first.trim(), it.second.toInt()) }
    companion object {
        fun from(settings: RandomSettings) = WheelDraft(settings.wheelName, settings.sections.map { it.content to it.weight.toString() }, settings.equalWheelSections)
        val Saver = listSaver<WheelDraft, String>(save = { listOf(it.name, it.equalSections.toString()) + it.rows.flatMap { row -> listOf(row.first, row.second) } }, restore = { WheelDraft(it.first(), it.drop(2).chunked(2).map { row -> row[0] to row[1] }, it[1].toBoolean()) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomWheelEditor(settings: RandomSettings, onSave: (RandomSettings) -> Unit = {}, onCancel: () -> Unit = {}) {
    var draft by rememberSaveable(stateSaver = WheelDraft.Saver) { mutableStateOf(WheelDraft.from(settings)) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf<Int?>(null) }
    val requestBack = { if (draft != WheelDraft.from(settings)) discard = true else onCancel() }
    BackHandler { requestBack() }
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing, topBar = {
        TopAppBar(title = { Text("编辑转盘") }, navigationIcon = { IconButton(onClick = requestBack, modifier = Modifier.testTag("random_editor_back")) { Icon(Icons.AutoMirrored.Default.ArrowBack, "返回") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState()).padding(16.dp).testTag("random_wheel_editor"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(draft.name, { if (it.length <= 20) draft = draft.copy(name = it) }, label = { Text("转盘名称") }, singleLine = true, isError = draft.name.isBlank(), modifier = Modifier.fillMaxWidth().testTag("random_wheel_name"))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("等分显示", style = MaterialTheme.typography.titleMedium)
                    Text("概率仍按权重", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(draft.equalSections, { draft = draft.copy(equalSections = it) }, Modifier.testTag("random_equal_sections"))
            }
            NumberControl("选项数量", draft.rows.size, 2..12, "random_sections") { count ->
                if (count > draft.rows.size) draft = draft.copy(rows = draft.rows + ("选项 $count" to "1")) else deleting = draft.rows.lastIndex
            }
            val total = draft.rows.sumOf { it.second.toIntOrNull()?.coerceIn(0, 100) ?: 0 }
            draft.rows.forEachIndexed { index, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(row.first, { text -> if (text.length <= 16) draft = draft.copy(rows = draft.rows.mapIndexed { i, r -> if (i == index) text to r.second else r }) }, label = { Text("内容 ${index + 1}") }, singleLine = true, isError = row.first.isBlank(), modifier = Modifier.weight(1f).testTag("random_section_${index}_content"))
                    OutlinedTextField(row.second, { text -> if (text.length <= 3) draft = draft.copy(rows = draft.rows.mapIndexed { i, r -> if (i == index) r.first to text else r }) }, label = { Text("权重") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, isError = row.second.toIntOrNull() !in 1..100, modifier = Modifier.width(76.dp).testTag("random_section_${index}_weight"))
                    IconButton(onClick = { deleting = index }, enabled = draft.rows.size > 2, modifier = Modifier.size(48.dp).testTag("random_section_${index}_delete")) { Icon(Icons.Default.Close, "删除选项 ${index + 1}") }
                }
                Text(if (draft.valid() && total > 0) String.format(Locale.CHINA, "概率 %.1f%%", row.second.toInt() * 100f / total) else "权重须为 1–100 的整数，内容不能为空", style = MaterialTheme.typography.labelMedium, color = if (draft.valid()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
            }
            Button(onClick = { onSave(settings.copy(wheelName = draft.name.trim(), sections = draft.sections(), equalWheelSections = draft.equalSections)) }, enabled = draft.valid(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("random_wheel_save")) { Text("保存转盘") }
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("放弃修改？") }, confirmButton = { TextButton(onClick = onCancel) { Text("放弃") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("继续编辑") } })
    deleting?.let { index -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除“${draft.rows[index].first}”？") }, confirmButton = { TextButton(onClick = { draft = draft.copy(rows = draft.rows.filterIndexed { i, _ -> i != index }); deleting = null }) { Text("删除") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }) }
}
