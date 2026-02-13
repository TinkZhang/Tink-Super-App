package app.tinks.tink.merriam

import app.tinks.tink.merriam.db.RootEntity
import app.tinks.tink.merriam.db.toRoot
import org.junit.Assert.assertEquals
import org.junit.Test

class RootEntityMapperTest {

    @Test
    fun toRoot_mapsEntityToDomain() {
        val entity = RootEntity(
            id = 11,
            unit = 2,
            root = "BENE",
            meaning = "Well",
            words = listOf("benefit", "benefactor")
        )

        val root = entity.toRoot()

        assertEquals(11, root.id)
        assertEquals(2, root.unit)
        assertEquals("BENE", root.text)
        assertEquals("Well", root.meaning)
        assertEquals(listOf("benefit", "benefactor"), root.words)
    }
}
