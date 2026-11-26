package app.tinks.tink.zi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandwritingStateTest {
    @Test fun candidatesKeepDistinctHanCharactersInRecognitionOrder() {
        assertEquals(
            listOf("土", "士", "𠀀", "〇"),
            handwritingCandidates(listOf("A", "土", "土", "  士", "天地", "𠀀", "⼀", "〸", "〇", "王")),
        )
    }

    @Test fun confirmationRequiresAnExplicitSelectionAndAllowsRetry() {
        val state = HandwritingState(candidates = listOf("土", "士"))
        assertFalse(state.canSave)
        assertTrue(state.copy(selected = "土", saveMessage = "保存失败").canSave)
        assertFalse(state.copy(selected = "土", isDrawing = true).canSave)
        assertFalse(state.copy(selected = "土", alreadyRecorded = true).canSave)
    }
}
