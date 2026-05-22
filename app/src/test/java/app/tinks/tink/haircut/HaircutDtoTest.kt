package app.tinks.tink.haircut

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HaircutDtoTest {

    @Test
    fun toDomain_mapsApiFields() {
        val dto = HaircutDto(
            id = 7,
            createdAt = LocalDate(2026, 5, 22),
            shopName = "Tink Barber",
            price = 35,
        )

        val haircut = dto.toDomain()

        assertEquals(7, haircut.id)
        assertEquals(35, haircut.price)
        assertEquals(LocalDate(2026, 5, 22), haircut.date)
        assertEquals("Tink Barber", haircut.shopName)
    }
}
