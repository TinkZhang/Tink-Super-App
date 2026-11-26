package app.tinks.tink.zi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandwritingStateTest {
    @Test fun isolatedCharacterUsesEmptyPreContextAndNormalizedBoard() {
        val context = singleCharacterRecognitionContext()

        assertEquals("", context.preContext)
        val board = requireNotNull(context.writingArea)
        assertEquals(1f, board.width, 0f)
        assertEquals(1f, board.height, 0f)
    }

    @Test fun candidatesKeepDistinctHanCharactersInRecognitionOrder() {
        assertEquals(
            listOf("土", "士", "𠀀", "〇"),
            handwritingCandidates(listOf("A", "土", "土", "  士", "天地", "𠀀", "⼀", "〸", "〇", "王")),
        )
    }

    @Test fun selectingCandidateQueuesItAndClearsOnlyTheCurrentWriting() {
        val state = HandwritingState(
            strokes = listOf(listOf(HandwritingPoint(.5f, .5f, 0L))),
            queuedCharacters = listOf("山"),
            candidates = listOf("土", "士"),
        )

        val selected = state.enqueue("土")

        assertEquals(listOf("山", "土"), selected.queuedCharacters)
        assertTrue(selected.strokes.isEmpty())
        assertTrue(selected.candidates.isEmpty())
        assertEquals(listOf("山", "土"), selected.copy(candidates = listOf("土")).enqueue("土").queuedCharacters)
        assertEquals(listOf("土"), selected.remove("山").queuedCharacters)
    }

    @Test fun confirmationRequiresQueuedCharactersAndKeepsUnconfirmedEntriesForRetry() {
        val state = HandwritingState(candidates = listOf("土", "士"))
        assertFalse(state.canSave)
        assertFalse(state.copy(queuedCharacters = listOf("土"), isDrawing = true).canSave)
        val saving = state.copy(queuedCharacters = listOf("土", "山", "木"), isSaving = true)
        assertFalse(saving.canSave)
        assertEquals(saving, saving.remove("土"))
        val retry = saving.failedSave(1)
        assertEquals(listOf("山", "木"), retry.queuedCharacters)
        assertTrue(retry.canSave)
    }
}
