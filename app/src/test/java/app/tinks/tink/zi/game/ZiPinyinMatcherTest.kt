package app.tinks.tink.zi.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZiPinyinMatcherTest {
    @Test
    fun matchesExactCharacterAndHomophones() {
        assertTrue(ZiPinyinMatcher.matchesSpokenText("木", "木"))
        assertTrue(ZiPinyinMatcher.matchesSpokenText("木", "目"))
        assertTrue(ZiPinyinMatcher.matchesSpokenText("木", "幕"))
        assertTrue(ZiPinyinMatcher.matchesSpokenText("水", "睡"))
    }

    @Test
    fun matchesAllPolyphoneReadings() {
        // 乐: le / yue
        assertTrue(ZiPinyinMatcher.matchesSpokenText("乐", "了"))
        assertTrue(ZiPinyinMatcher.matchesSpokenText("乐", "月"))
        // 行: xing / hang
        assertTrue(ZiPinyinMatcher.matchesSpokenText("行", "星"))
        assertTrue(ZiPinyinMatcher.matchesSpokenText("行", "航"))
    }

    @Test
    fun allowsChildFriendlyShortPhrases() {
        assertTrue(ZiPinyinMatcher.matchesSpokenText("木", "木头"))
        assertTrue(ZiPinyinMatcher.matchesSpokenText("山", "大山"))
        assertTrue(ZiPinyinMatcher.matchesSpokenText("火", "着火了"))
        assertTrue(ZiPinyinMatcher.matchesSpokenText("桥", "小乔"))
    }

    @Test
    fun rejectsUnrelatedPronunciationAndBlankInput() {
        assertFalse(ZiPinyinMatcher.matchesSpokenText("木", ""))
        assertFalse(ZiPinyinMatcher.matchesSpokenText("木", "山"))
        assertFalse(ZiPinyinMatcher.matchesSpokenText("水", "大火"))
    }

    @Test
    fun formatsToneMarkedPinyinForReviewDisplay() {
        val muPinyin = ZiPinyinMatcher.displayPinyin("木")
        assertTrue(muPinyin.contains("mù"))
        val lePinyin = ZiPinyinMatcher.displayPinyin("乐")
        assertTrue(lePinyin.contains("lè") || lePinyin.contains("yuè"))
    }
}
