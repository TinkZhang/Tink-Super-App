package app.tinks.tink.zi.game

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType

/**
 * Matches spoken Mandarin ASR output against a target Chinese character using Pinyin
 * so that homophones ("目" -> "木"), polyphones ("乐" -> le/yue), and child-friendly
 * short words/phrases ("木头" -> "木") are recognized accurately.
 */
object ZiPinyinMatcher {
    private const val MAX_STREAM_WINDOW_CHARS = 6

    private val tonelessFormat = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.LOWERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
        vCharType = HanyuPinyinVCharType.WITH_V
    }

    private val toneMarkFormat = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.LOWERCASE
        toneType = HanyuPinyinToneType.WITH_TONE_MARK
        vCharType = HanyuPinyinVCharType.WITH_U_UNICODE
    }

    fun tonelessPinyins(character: Char): Set<String> {
        val readings = runCatching {
            PinyinHelper.toHanyuPinyinStringArray(character, tonelessFormat)
        }.getOrNull() ?: return emptySet()
        return readings
            .mapNotNull { normalizePinyin(it) }
            .toSet()
    }

    fun toneMarkedPinyins(character: Char): List<String> {
        val readings = runCatching {
            PinyinHelper.toHanyuPinyinStringArray(character, toneMarkFormat)
        }.getOrNull() ?: return emptyList()
        return readings
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun displayPinyin(zi: String): String {
        val firstChar = zi.trim().firstOrNull() ?: return ""
        val marked = toneMarkedPinyins(firstChar)
        return marked.take(2).joinToString(" / ")
    }

    /**
     * Checks whether [spokenText] contains a pronunciation matching [targetZi].
     *
     * Rules designed for 1st-grade Mandarin learners:
     * 1. Direct character match (always accepted).
     * 2. Homophone match by toneless Pinyin across all polyphone readings.
     * 3. Allows short phrases (e.g. "木头" for "木") by inspecting up to the last
     *    [MAX_STREAM_WINDOW_CHARS] Han characters in the streaming recognition buffer.
     * 4. Also accepts direct Latin pinyin tokens (useful for testing/debugging).
     */
    fun matchesSpokenText(
        targetZi: String,
        spokenText: String,
        allowPhrase: Boolean = true,
    ): Boolean {
        val targetChar = targetZi.trim().firstOrNull() ?: return false
        val cleaned = spokenText.trim()
        if (cleaned.isEmpty()) return false

        val hanChars = cleaned.filter(::isHanCharacter)
        val windowChars = when {
            hanChars.isEmpty() -> ""
            allowPhrase -> hanChars.takeLast(MAX_STREAM_WINDOW_CHARS)
            else -> hanChars.takeLast(1)
        }

        if (windowChars.contains(targetChar)) return true

        val targetPinyins = tonelessPinyins(targetChar)
        if (targetPinyins.isEmpty()) return false

        for (ch in windowChars) {
            val spokenPinyins = tonelessPinyins(ch)
            if (spokenPinyins.any { it in targetPinyins }) {
                return true
            }
        }

        // Also match plain ASCII pinyin tokens (e.g., "mu" or "mu4") for debug/test input.
        val asciiTokens = cleaned.lowercase()
            .split(Regex("[^a-z1-5ü]+"))
            .mapNotNull { normalizePinyin(it) }
        return asciiTokens.any { it in targetPinyins }
    }

    private fun normalizePinyin(raw: String): String? {
        val normalized = raw.trim()
            .lowercase()
            .replace("u:", "v")
            .replace("ü", "v")
            .replace(Regex("[1-5]"), "")
        return normalized.takeIf { it.isNotEmpty() && it.all { ch -> ch in 'a'..'z' } }
    }

    private fun isHanCharacter(ch: Char): Boolean {
        val code = ch.code
        return ch == '〇' || (code >= 0x3400 && Character.UnicodeScript.of(code) == Character.UnicodeScript.HAN)
    }
}
