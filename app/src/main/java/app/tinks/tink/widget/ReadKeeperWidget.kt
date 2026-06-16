package app.tinks.tink.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.tinks.tink.BuildConfig
import app.tinks.tink.MainActivity
import app.tinks.tink.R
import app.tinks.tink.book.Book
import app.tinks.tink.book.BookDto
import app.tinks.tink.book.BookPageFormat
import app.tinks.tink.book.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class ReadKeeperWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = ReadKeeperWidgetLoader.load()
        provideContent {
            ReadKeeperWidgetContent(state)
        }
    }
}

class ReadKeeperWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReadKeeperWidget()
}

@Composable
private fun ReadKeeperWidgetContent(state: ReadKeeperWidgetState) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFF3F7F0)))
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = "ReadKeeper",
            style = TextStyle(
                color = ColorProvider(Color(0xFF31503B)),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(8.dp))
        when (state) {
            ReadKeeperWidgetState.Empty -> EmptyWidgetContent()
            ReadKeeperWidgetState.Error -> ErrorWidgetContent()
            is ReadKeeperWidgetState.Loaded -> LatestBookContent(state)
        }
    }
}

@Composable
private fun LatestBookContent(state: ReadKeeperWidgetState.Loaded) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Image(
            provider = state.cover ?: ImageProvider(R.drawable.ic_readkeeperlogo),
            contentDescription = state.book.title,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.size(72.dp),
        )
        Spacer(GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = state.book.title,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF111827)),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = state.progressLabel,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF4B5563)),
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(8.dp))
            LinearProgressIndicator(
                progress = state.progressFraction,
                modifier = GlanceModifier.fillMaxWidth(),
                color = ColorProvider(Color(0xFF2E7D32)),
                backgroundColor = ColorProvider(Color(0xFFDDE7D8)),
            )
        }
    }
}

@Composable
private fun EmptyWidgetContent() {
    Text(
        text = "No reading book yet",
        style = TextStyle(
            color = ColorProvider(Color(0xFF111827)),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        ),
        maxLines = 2,
    )
    Spacer(GlanceModifier.height(6.dp))
    Text(
        text = "Open ReadKeeper to start one.",
        style = TextStyle(color = ColorProvider(Color(0xFF4B5563)), fontSize = 12.sp),
        maxLines = 2,
    )
}

@Composable
private fun ErrorWidgetContent() {
    Text(
        text = "Unable to load reading book",
        style = TextStyle(
            color = ColorProvider(Color(0xFF111827)),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        ),
        maxLines = 2,
    )
    Spacer(GlanceModifier.height(6.dp))
    Text(
        text = "Tap to open ReadKeeper.",
        style = TextStyle(color = ColorProvider(Color(0xFF4B5563)), fontSize = 12.sp),
        maxLines = 2,
    )
}

private sealed interface ReadKeeperWidgetState {
    data object Empty : ReadKeeperWidgetState
    data object Error : ReadKeeperWidgetState
    data class Loaded(
        val book: Book,
        val cover: ImageProvider?,
        val progressLabel: String,
        val progressFraction: Float,
    ) : ReadKeeperWidgetState
}

private object ReadKeeperWidgetLoader {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): ReadKeeperWidgetState = withContext(Dispatchers.IO) {
        runCatching {
            val book = fetchLatestReadingBook() ?: return@withContext ReadKeeperWidgetState.Empty
            ReadKeeperWidgetState.Loaded(
                book = book,
                cover = book.coverUrl?.let(::downloadCover)?.let { ImageProvider(it) },
                progressLabel = book.widgetProgressLabel(),
                progressFraction = ((book.widgetProgressPercent() ?: 0.0) / 100.0).toFloat()
                    .coerceIn(0f, 1f),
            )
        }.getOrElse {
            ReadKeeperWidgetState.Error
        }
    }

    private fun fetchLatestReadingBook(): Book? {
        val url = URL("${BuildConfig.TINK_API_BASE_URL.ensureTrailingSlash()}book/reading?page=0&size=1")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<List<BookDto>>(body).firstOrNull()?.toDomain()
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadCover(url: String): Bitmap? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.use(BitmapFactory::decodeStream)
        } finally {
            connection.disconnect()
        }
    }
}

private fun String.ensureTrailingSlash(): String =
    if (endsWith("/")) this else "$this/"

private fun Book.widgetProgressPercent(): Double? =
    if (pageFormat.usesPages) {
        currentPage?.let { page ->
            pages?.takeIf { it > 0 }?.let { totalPages -> page * 100.0 / totalPages }
        } ?: progressPercentage
    } else {
        progressPercentage
    }

private fun Book.widgetProgressLabel(): String {
    val percent = widgetProgressPercent()
    return if (pageFormat.usesPages) {
        val pageLabel = currentPage?.let { page -> pages?.let { "$page / $it pages" } ?: "Page $page" }
        listOfNotNull(pageLabel, percent?.let { "${it.toInt()}%" }).joinToString(" · ")
            .ifBlank { "Not started" }
    } else {
        percent?.formatWidgetPercent(pageFormat) ?: "Not started"
    }
}

private fun Double.formatWidgetPercent(format: BookPageFormat): String =
    if (format.precision == 0) {
        "${toInt()}%"
    } else {
        String.format(Locale.US, "%.${format.precision}f%%", this)
    }
