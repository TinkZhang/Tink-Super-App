package app.tinks.tink.connect

import app.tinks.tink.di.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectTest {
    @Test
    fun refreshingHubKeepsLocalEditsButUpdatesPublishedText() {
        val before = ConnectHub(text = "Published")
        val after = ConnectHub(text = "From Mac")
        assertEquals("From Mac", ConnectUiState(hub = before, draft = before.text).withHub(after).draft)
        val edited = ConnectUiState(hub = before, draft = "Local edit").withHub(after)
        assertEquals("Local edit", edited.draft)
        assertEquals("From Mac", edited.hub.text)
    }

    @Test
    fun uploadTicketDecodesAndEncodesSupabaseMetadata() {
        val ticket = NetworkModule.json.decodeFromString<ConnectUpload>(
            """{"id":"file-id","upload_url":"https://test.example/tus","token":"signed","publishable_key":"public","signed_upload_url":"https://test.example/sign","bucket":"dev-connect","object_path":"file-id"}"""
        )
        assertEquals("public", ticket.publishableKey)
        assertEquals(
            "bucketName ZGV2LWNvbm5lY3Q=,objectName ZmlsZS1pZA==,contentType YXBwbGljYXRpb24vcGRm",
            connectUploadMetadata(ticket, "application/pdf"),
        )
    }
}
