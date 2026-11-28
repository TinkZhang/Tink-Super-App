package app.tinks.tink.zi.game

import androidx.compose.ui.graphics.Color

enum class CelestialKind {
    Planet,
    DwarfOrAsteroid,
}

data class ZiGameStage(
    val number: Int,
    val name: String,
    val englishName: String,
    val kind: CelestialKind,
    val primaryColor: Color,
    val accentColor: Color,
    val hasRing: Boolean = false,
) {
    val displayTitle: String
        get() = "第${number}关 · $name"

    val kindLabel: String
        get() = when (kind) {
            CelestialKind.Planet -> "太阳系行星"
            CelestialKind.DwarfOrAsteroid -> "太阳系小行星"
        }
}

object ZiGameLevels {
    private data class BodySpec(
        val name: String,
        val englishName: String,
        val kind: CelestialKind,
        val primaryColor: Color,
        val accentColor: Color,
        val hasRing: Boolean = false,
    )

    private val solarBodies = listOf(
        // 八大行星（由近及远）
        BodySpec("水星", "Mercury", CelestialKind.Planet, Color(0xFF94A3B8), Color(0xFFCBD5E1)),
        BodySpec("金星", "Venus", CelestialKind.Planet, Color(0xFFF59E0B), Color(0xFFFDE68A)),
        BodySpec("地球", "Earth", CelestialKind.Planet, Color(0xFF2563EB), Color(0xFF4ADE80)),
        BodySpec("火星", "Mars", CelestialKind.Planet, Color(0xFFEA580C), Color(0xFFFDBA74)),
        BodySpec("木星", "Jupiter", CelestialKind.Planet, Color(0xFFD97706), Color(0xFFFEF3C7)),
        BodySpec("土星", "Saturn", CelestialKind.Planet, Color(0xFFEAB308), Color(0xFFFEF08A), hasRing = true),
        BodySpec("天王星", "Uranus", CelestialKind.Planet, Color(0xFF22D3EE), Color(0xFFCFFAFE), hasRing = true),
        BodySpec("海王星", "Neptune", CelestialKind.Planet, Color(0xFF3B82F6), Color(0xFF93C5FD)),
        // 太阳系著名小行星与矮行星
        BodySpec("冥王星", "Pluto", CelestialKind.DwarfOrAsteroid, Color(0xFFA8A29E), Color(0xFFE7E5E4)),
        BodySpec("谷神星", "Ceres", CelestialKind.DwarfOrAsteroid, Color(0xFF78716C), Color(0xFFD6D3D1)),
        BodySpec("灶神星", "Vesta", CelestialKind.DwarfOrAsteroid, Color(0xFFCA8A04), Color(0xFFFEF08A)),
        BodySpec("智神星", "Pallas", CelestialKind.DwarfOrAsteroid, Color(0xFF64748B), Color(0xFFCBD5E1)),
        BodySpec("婚神星", "Juno", CelestialKind.DwarfOrAsteroid, Color(0xFFE11D48), Color(0xFFFDA4AF)),
        BodySpec("龙宫星", "Ryugu", CelestialKind.DwarfOrAsteroid, Color(0xFF52525B), Color(0xFFA1A1AA)),
        BodySpec("贝努星", "Bennu", CelestialKind.DwarfOrAsteroid, Color(0xFF475569), Color(0xFF94A3B8)),
        BodySpec("爱神星", "Eros", CelestialKind.DwarfOrAsteroid, Color(0xFFEC4899), Color(0xFFFBCFE8)),
        BodySpec("灵神星", "Psyche", CelestialKind.DwarfOrAsteroid, Color(0xFF0EA5E9), Color(0xFFBAE6FD)),
        BodySpec("阋神星", "Eris", CelestialKind.DwarfOrAsteroid, Color(0xFF8B5CF6), Color(0xFFDDD6FE)),
        BodySpec("妊神星", "Haumea", CelestialKind.DwarfOrAsteroid, Color(0xFF14B8A6), Color(0xFF99F6E4)),
        BodySpec("鸟神星", "Makemake", CelestialKind.DwarfOrAsteroid, Color(0xFFF97316), Color(0xFFFED7AA)),
    )

    val catalogSize: Int get() = solarBodies.size

    fun stageForNumber(stageNumber: Int): ZiGameStage {
        val safeNumber = stageNumber.coerceAtLeast(1)
        val zeroBased = safeNumber - 1
        val cycle = zeroBased / solarBodies.size
        val spec = solarBodies[zeroBased % solarBodies.size]
        val displayName = if (cycle == 0) spec.name else "${spec.name} · 第${cycle + 1}圈"
        return ZiGameStage(
            number = safeNumber,
            name = displayName,
            englishName = spec.englishName,
            kind = spec.kind,
            primaryColor = spec.primaryColor,
            accentColor = spec.accentColor,
            hasRing = spec.hasRing,
        )
    }

    fun visibleStages(highestUnlockedStage: Int): List<ZiGameStage> {
        val maxStage = maxOf(highestUnlockedStage.coerceAtLeast(1), catalogSize)
        return (1..maxStage).map(::stageForNumber)
    }
}
