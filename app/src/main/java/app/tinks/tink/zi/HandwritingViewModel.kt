package app.tinks.tink.zi

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tinks.tink.ui.components.AppSnackbarBus
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal data class HandwritingPoint(val x: Float, val y: Float, val timeMillis: Long)

internal enum class InkModelState { Loading, Ready, Failed }
internal enum class InkRecognitionState { Idle, Recognizing, NoMatch, Failed }

internal data class HandwritingState(
    val boardId: String? = null,
    val strokes: List<List<HandwritingPoint>> = emptyList(),
    val candidates: List<String> = emptyList(),
    val queuedCharacters: List<String> = emptyList(),
    val model: InkModelState = InkModelState.Loading,
    val recognition: InkRecognitionState = InkRecognitionState.Idle,
    val isDrawing: Boolean = false,
    val isSaving: Boolean = false,
    val saveMessage: String? = null,
    val saved: Boolean = false,
) {
    val canSave get() = queuedCharacters.isNotEmpty() && !isDrawing && !isSaving && !saved

    fun enqueue(character: String): HandwritingState {
        if (isDrawing || isSaving || saved || character !in candidates) return this
        return copy(
            strokes = emptyList(),
            candidates = emptyList(),
            queuedCharacters = if (character in queuedCharacters) queuedCharacters else queuedCharacters + character,
            recognition = InkRecognitionState.Idle,
        )
    }

    fun remove(character: String): HandwritingState {
        if (isSaving || saved) return this
        return copy(queuedCharacters = queuedCharacters - character)
    }

    fun failedSave(confirmedCount: Int): HandwritingState = copy(
        queuedCharacters = queuedCharacters.drop(confirmedCount),
        isSaving = false,
        saveMessage = if (confirmedCount == 0) "保存失败，请重试。" else "部分汉字已处理，请重试剩余汉字。",
    )
}

internal fun singleCharacterRecognitionContext(): RecognitionContext =
    RecognitionContext.builder()
        .setPreContext("")
        .setWritingArea(WritingArea(1f, 1f))
        .build()

internal fun handwritingCandidates(values: List<String>): List<String> =
    values.asSequence()
        .map(String::trim)
        .filter { it.codePointCount(0, it.length) == 1 }
        .filter {
            val codePoint = it.codePointAt(0)
            it == "〇" ||
                (codePoint >= 0x3400 && Character.isIdeographic(codePoint) &&
                    Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN)
        }
        .distinct()
        .take(4)
        .toList()

@HiltViewModel
class HandwritingViewModel @Inject constructor(private val repository: WrittenZiRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(HandwritingState())
    internal val state = mutableState.asStateFlow()
    private val model = DigitalInkRecognitionModel.builder(DigitalInkRecognitionModelIdentifier.ZH_HANI_CN).build()
    private val manager = RemoteModelManager.getInstance()
    private val recognizer = lazy {
        DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
    }
    private var modelJob: Job? = null
    private var recognitionJob: Job? = null

    private var saveJob: Job? = null

    fun enterBoard(boardId: String) {
        val previous = mutableState.value
        if (previous.boardId == boardId) return
        recognitionJob?.cancel()
        saveJob?.cancel()
        mutableState.value = HandwritingState(boardId = boardId, model = previous.model)
        if (previous.model != InkModelState.Ready) prepareModel()
    }

    fun prepareModel() {
        if (modelJob?.isActive == true) return
        modelJob = viewModelScope.launch {
            mutableState.update { it.copy(model = InkModelState.Loading) }
            try {
                if (!manager.isModelDownloaded(model).await()) {
                    manager.download(model, DownloadConditions.Builder().build()).await()
                }
                mutableState.update { it.copy(model = InkModelState.Ready) }
                recognize()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(model = InkModelState.Failed) }
            }
        }
    }

    fun beginStroke() {
        if (mutableState.value.isSaving) return
        recognitionJob?.cancel()
        mutableState.update {
            it.copy(
                isDrawing = true, candidates = emptyList(), recognition = InkRecognitionState.Idle,
            )
        }
    }

    internal fun finishStroke(points: List<HandwritingPoint>) {
        if (!mutableState.value.isDrawing) return
        mutableState.update {
            it.copy(strokes = if (points.isEmpty()) it.strokes else it.strokes + listOf(points), isDrawing = false)
        }
        recognize()
    }

    fun undo() {
        if (!mutableState.value.isSaving) {
            mutableState.update { it.copy(strokes = it.strokes.dropLast(1)) }
            recognize()
        }
    }

    fun clear() {
        if (!mutableState.value.isSaving) {
            mutableState.update { it.copy(strokes = emptyList()) }
            recognize()
        }
    }

    fun recognize() {
        recognitionJob?.cancel()
        val snapshot = mutableState.value
        mutableState.update { it.copy(candidates = emptyList(), recognition = InkRecognitionState.Idle) }
        if (snapshot.model != InkModelState.Ready || snapshot.strokes.isEmpty() ||
            snapshot.isDrawing || snapshot.isSaving || snapshot.saved
        ) return

        recognitionJob = viewModelScope.launch {
            delay(450)
            mutableState.update { it.copy(recognition = InkRecognitionState.Recognizing) }
            try {
                val ink = Ink.builder()
                for (points in snapshot.strokes) {
                    val stroke = Ink.Stroke.builder()
                    for (point in points) stroke.addPoint(Ink.Point.create(point.x, point.y, point.timeMillis))
                    ink.addStroke(stroke.build())
                }
                val context = singleCharacterRecognitionContext()
                val result = recognizer.value.recognize(ink.build(), context).await()
                val candidates = handwritingCandidates(result.candidates.map { it.text })
                mutableState.update {
                    it.copy(
                        candidates = candidates,
                        recognition = if (candidates.isEmpty()) InkRecognitionState.NoMatch else InkRecognitionState.Idle,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("ZiHandwriting", "Digital ink recognition failed", error)
                mutableState.update { it.copy(recognition = InkRecognitionState.Failed) }
            }
        }
    }

    fun select(character: String) {
        mutableState.update { it.enqueue(character) }
    }

    fun remove(character: String) {
        mutableState.update { it.remove(character) }
    }

    fun save() {
        val current = mutableState.value
        if (!current.canSave) return
        recognitionJob?.cancel()
        mutableState.update { it.copy(isSaving = true, saveMessage = null) }
        saveJob = viewModelScope.launch {
            val result = repository.saveBatch(current.queuedCharacters)
            if (result.confirmedCount != current.queuedCharacters.size) {
                mutableState.update { it.failedSave(result.confirmedCount) }
                recognize()
                return@launch
            }

            mutableState.update { it.copy(isSaving = false, saved = true) }
            val message = when (result.addedCount) {
                0 -> "这些字已在会写列表里"
                else -> "已添加 ${result.addedCount} 个会写的字"
            }
            AppSnackbarBus.showMessage(message)
        }
    }

    override fun onCleared() {
        if (recognizer.isInitialized()) recognizer.value.close()
    }
}
