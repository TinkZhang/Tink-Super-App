package app.tinks.tink.weight

import org.junit.Assert.assertEquals
import org.junit.Test

class WeightDtoTest {

    @Test
    fun toDomain_parsesIsoTimestampToEpochMillis() {
        val dto = WeightDto(
            id = 7,
            createdAt = "2026-05-19T08:15:30Z",
            weight = 141.5,
        )

        val domain = dto.toDomain()

        assertEquals(7, domain.id)
        assertEquals(141.5, domain.weight, 0.0)
        assertEquals(1779178530000L, domain.createdTime)
    }

    @Test
    fun toDomain_usesEpochForInvalidTimestamp() {
        val dto = WeightDto(
            id = 8,
            createdAt = "not-a-date",
            weight = 140.0,
        )

        val domain = dto.toDomain()

        assertEquals(0L, domain.createdTime)
    }
}
