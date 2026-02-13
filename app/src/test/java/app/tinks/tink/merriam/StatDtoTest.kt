package app.tinks.tink.merriam

import app.tinks.tink.merriam.data.StatDto
import org.junit.Assert.assertEquals
import org.junit.Test

class StatDtoTest {

    @Test
    fun toDomain_mapsAllFields() {
        val dto = StatDto(
            latest = 42,
            weekStats = listOf(1, null, 3)
        )

        val domain = dto.toDomain()

        assertEquals(42, domain.latest)
        assertEquals(listOf(1, null, 3), domain.weekStats)
    }
}
