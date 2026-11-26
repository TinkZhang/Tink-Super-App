package app.tinks.tink.zi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ZiHandwritingRoute(boardId: String, viewModel: HandwritingViewModel, onSaved: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(boardId) { viewModel.enterBoard(boardId) }
    LaunchedEffect(boardId, state.boardId, state.saved) {
        if (state.boardId == boardId && state.saved) onSaved()
    }
    ZiHandwritingScreen(
        state = if (state.boardId == boardId) state else HandwritingState(),
        onStrokeStart = viewModel::beginStroke,
        onStrokeEnd = viewModel::finishStroke,
        onUndo = viewModel::undo,
        onClear = viewModel::clear,
        onSelect = viewModel::select,
        onSave = viewModel::save,
        onRetryModel = viewModel::prepareModel,
        onRetryRecognition = viewModel::recognize,
    )
}

@Composable
internal fun ZiHandwritingScreen(
    state: HandwritingState,
    onStrokeStart: () -> Unit = {},
    onStrokeEnd: (List<HandwritingPoint>) -> Unit = {},
    onUndo: () -> Unit = {},
    onClear: () -> Unit = {},
    onSelect: (String) -> Unit = {},
    onSave: () -> Unit = {},
    onRetryModel: () -> Unit = {},
    onRetryRecognition: () -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("zi_handwriting_screen")) {
        val boardSize = minOf((maxWidth - 32.dp).coerceAtLeast(0.dp), (maxHeight - 270.dp).coerceIn(160.dp, 320.dp))
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.weight(1f).widthIn(max = 480.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("请在田字格里写一个汉字。", style = MaterialTheme.typography.bodyLarge)
                HandwritingBoard(
                    side = boardSize,
                    strokes = state.strokes,
                    enabled = !state.isSaving && !state.saved,
                    onStrokeStart = onStrokeStart,
                    onStrokeEnd = onStrokeEnd,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    val canEdit = state.strokes.isNotEmpty() && !state.isDrawing && !state.isSaving
                    TextButton(onClick = onUndo, enabled = canEdit) { Text("撤销一笔") }
                    OutlinedButton(onClick = onClear, enabled = canEdit) { Text("清空") }
                }
                Text("候选字", style = MaterialTheme.typography.titleMedium)
                when {
                    state.model == InkModelState.Loading -> Text("正在准备中文手写模型，首次需要联网。")
                    state.model == InkModelState.Failed -> {
                        Text("中文手写模型下载失败。请检查网络后重试。", color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetryModel) { Text("重新下载") }
                    }
                    state.candidates.isNotEmpty() -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.candidates.forEach { character ->
                                FilterChip(
                                    selected = state.selected == character,
                                    onClick = { onSelect(character) },
                                    enabled = !state.isSaving,
                                    label = { Text(character, style = MaterialTheme.typography.headlineSmall) },
                                    modifier = Modifier.weight(1f, fill = false).heightIn(min = 48.dp),
                                )
                            }
                        }
                        if (state.saveMessage == null) Text("选择你写的那个字。")
                    }
                    state.recognition == InkRecognitionState.Recognizing -> Text("正在识别…")
                    state.recognition == InkRecognitionState.NoMatch -> Text("没识别出这个字。可以补写笔画，或清空重写。")
                    state.recognition == InkRecognitionState.Failed -> {
                        Text("手写识别失败。")
                        TextButton(onClick = onRetryRecognition) { Text("重新识别") }
                    }
                    else -> Text("写完后，候选字会显示在这里。")
                }
                state.saveMessage?.let { message ->
                    Text(
                        message,
                        color = if (state.alreadyRecorded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    )
                }
            }
            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(vertical = 8.dp)
                    .heightIn(min = 48.dp).testTag("zi_confirm_written"),
            ) {
                Text(
                    when {
                        state.isSaving -> "正在保存…"
                        state.selected != null -> "确认添加「${state.selected}」"
                        else -> "确认添加"
                    }
                )
            }
        }
    }
}

@Composable
private fun HandwritingBoard(
    side: Dp,
    strokes: List<List<HandwritingPoint>>,
    enabled: Boolean,
    onStrokeStart: () -> Unit,
    onStrokeEnd: (List<HandwritingPoint>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeStroke = remember { mutableStateListOf<HandwritingPoint>() }
    val start by rememberUpdatedState(onStrokeStart)
    val finish by rememberUpdatedState(onStrokeEnd)
    val shape = RoundedCornerShape(20.dp)
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val inkColor = MaterialTheme.colorScheme.onSurface
    Canvas(
        modifier.size(side).clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .semantics { contentDescription = "用于手写一个汉字的田字格" }
            .testTag("zi_handwriting_board")
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    start()
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    fun point(position: Offset, time: Long) = HandwritingPoint(
                        (position.x / width).coerceIn(0f, 1f),
                        (position.y / height).coerceIn(0f, 1f),
                        time,
                    )
                    activeStroke.add(point(down.position, down.uptimeMillis))
                    var completed = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.isConsumed) break
                            change.historical.forEach { activeStroke.add(point(it.position, it.uptimeMillis)) }
                            if (change.position != change.previousPosition) {
                                activeStroke.add(point(change.position, change.uptimeMillis))
                            }
                            change.consume()
                            if (!change.pressed) {
                                completed = true
                                break
                            }
                        }
                    } finally {
                        val points = if (completed) activeStroke.toList() else emptyList()
                        activeStroke.clear()
                        finish(points)
                    }
                }
            },
    ) {
        val inset = 16.dp.toPx()
        val guide = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))
        val edge = 1.dp.toPx()
        drawRect(
            guideColor,
            topLeft = Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(size.width - 2 * inset, size.height - 2 * inset),
            style = Stroke(edge, pathEffect = guide),
        )
        drawLine(guideColor, Offset(center.x, inset), Offset(center.x, size.height - inset), edge, pathEffect = guide)
        drawLine(guideColor, Offset(inset, center.y), Offset(size.width - inset, center.y), edge, pathEffect = guide)
        fun DrawScope.drawStroke(points: List<HandwritingPoint>) {
            val first = points.firstOrNull() ?: return
            val width = 5.dp.toPx()
            if (points.size == 1) {
                drawCircle(inkColor, width / 2, Offset(first.x * size.width, first.y * size.height))
            } else {
                val path = Path().apply {
                    moveTo(first.x * size.width, first.y * size.height)
                    points.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
                }
                drawPath(path, inkColor, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        strokes.forEach { drawStroke(it) }
        drawStroke(activeStroke)
    }
}
