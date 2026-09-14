package app.tinks.tink.random

import kotlin.random.Random

enum class RandomType(val label: String, val action: String, val durationMillis: Long) {
    Dice("骰子", "掷骰子", 1500), Wheel("转盘", "转一下", 3200), Coin("硬币", "抛硬币", 1300),
}

data class WheelSection(val content: String, val weight: Int = 1)

data class RandomSettings(
    val type: RandomType = RandomType.Dice,
    val diceCount: Int = 2,
    val wheelName: String = "午饭吃什么",
    val sections: List<WheelSection> = listOf(
        WheelSection("面条", 2), WheelSection("火锅"), WheelSection("沙拉"), WheelSection("饺子", 2),
    ),
    val sound: Boolean = true,
    val speech: Boolean = true,
    val equalWheelSections: Boolean = false,
) {
    fun isValid(): Boolean = diceCount in 1..6 && wheelName.isNotBlank() && wheelName.length <= 20 &&
        sections.size in 2..12 && sections.all { it.content.isNotBlank() && it.content.length <= 16 && it.weight in 1..100 }
}

/** Display geometry is independent of the weights used by drawRandom. */
fun RandomSettings.displaySections(): List<WheelSection> =
    if (equalWheelSections) sections.map { it.copy(weight = 1) } else sections

data class RandomResult(
    val dice: List<Int> = listOf(3, 5),
    val wheelIndex: Int = 0,
    val heads: Boolean = true,
) {
    fun chinese(settings: RandomSettings): String = when (settings.type) {
        RandomType.Dice -> dice.joinToString("、") { "${chineseNumber(it)}点" } +
            if (dice.size > 1) "，共${chineseNumber(dice.sum())}点" else ""
        RandomType.Wheel -> "选中了，${settings.sections[wheelIndex].content}"
        RandomType.Coin -> if (heads) "正面" else "反面"
    }
}

fun chineseNumber(number: Int): String = when {
    number < 10 -> "零一二三四五六七八九"[number].toString()
    number < 20 -> "十" + if (number % 10 == 0) "" else chineseNumber(number % 10)
    else -> chineseNumber(number / 10) + "十" + if (number % 10 == 0) "" else chineseNumber(number % 10)
}

/** A ticket occupies one integer unit of weight; boundaries are half-open. */
fun weightedIndex(sections: List<WheelSection>, ticket: Int): Int {
    require(sections.isNotEmpty() && sections.all { it.weight > 0 })
    require(ticket in 0 until sections.sumOf { it.weight })
    var remaining = ticket
    return sections.indexOfFirst { section ->
        if (remaining < section.weight) true else { remaining -= section.weight; false }
    }
}

fun wheelCenterDegrees(sections: List<WheelSection>, index: Int): Float =
    (sections.take(index).sumOf { it.weight } + sections[index].weight / 2f) /
        sections.sumOf { it.weight } * 360f

fun nextWheelRotation(current: Float, sections: List<WheelSection>, index: Int): Float {
    val target = (360f - wheelCenterDegrees(sections, index)) % 360f
    return current + 5 * 360f + (target - current % 360f + 360f) % 360f
}

fun drawRandom(settings: RandomSettings, random: Random = Random.Default): RandomResult = when (settings.type) {
    RandomType.Dice -> RandomResult(dice = List(settings.diceCount) { random.nextInt(1, 7) })
    RandomType.Wheel -> RandomResult(wheelIndex = weightedIndex(settings.sections, random.nextInt(settings.sections.sumOf { it.weight })))
    RandomType.Coin -> RandomResult(heads = random.nextBoolean())
}
