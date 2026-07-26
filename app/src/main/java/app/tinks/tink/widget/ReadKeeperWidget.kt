package app.tinks.tink.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
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
import app.tinks.tink.MainActivity
import app.tinks.tink.book.Book
import app.tinks.tink.book.BookCoverCache
import app.tinks.tink.book.BookDao
import app.tinks.tink.book.BookPageFormat
import app.tinks.tink.book.ReadKeeperSyncScheduler
import app.tinks.tink.book.toDomain
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class ReadKeeperWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = ReadKeeperWidgetLoader.load(context.applicationContext)
        provideContent {
            GlanceTheme {
                ReadKeeperWidgetContent(state)
            }
        }
    }
}

class ReadKeeperWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReadKeeperWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        ReadKeeperSyncScheduler.enqueue(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ReadKeeperSyncScheduler.enqueue(context)
    }
}

@Composable
private fun ReadKeeperWidgetContent(state: ReadKeeperWidgetState) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        when (state) {
            ReadKeeperWidgetState.Empty -> EmptyWidgetContent()
            ReadKeeperWidgetState.Error -> ErrorWidgetContent()
            is ReadKeeperWidgetState.Loaded -> LatestBookContent(state)
        }
    }
}

@Composable
private fun LatestBookContent(state: ReadKeeperWidgetState.Loaded) {
    val availableSize = LocalSize.current
    val coverHeight = (availableSize.height - 32.dp).coerceAtLeast(1.dp)
    val coverWidth = coverHeight * state.coverAspectRatio

    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        state.cover?.let { cover ->
            Image(
                provider = cover,
                contentDescription = state.book.title,
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.size(coverWidth, coverHeight),
            )
            Spacer(GlanceModifier.width(16.dp))
        }
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = state.book.title,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
            )
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = state.progressLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 16.sp,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(12.dp))
            LinearProgressIndicator(
                progress = state.progressFraction,
                modifier = GlanceModifier.fillMaxWidth(),
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.primaryContainer,
            )
        }
    }
}

@Composable
private fun EmptyWidgetContent() {
    Text(
        text = "No reading book yet",
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        ),
        maxLines = 2,
    )
    Spacer(GlanceModifier.height(6.dp))
    Text(
        text = "Open ReadKeeper to start one.",
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 16.sp),
        maxLines = 2,
    )
}

@Composable
private fun ErrorWidgetContent() {
    Text(
        text = "Unable to load reading book",
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        ),
        maxLines = 2,
    )
    Spacer(GlanceModifier.height(6.dp))
    Text(
        text = "Tap to open ReadKeeper.",
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 16.sp),
        maxLines = 2,
    )
}

private sealed interface ReadKeeperWidgetState {
    data object Empty : ReadKeeperWidgetState
    data object Error : ReadKeeperWidgetState
    data class Loaded(
        val book: Book,
        val cover: ImageProvider?,
        val coverAspectRatio: Float,
        val progressLabel: String,
        val progressFraction: Float,
    ) : ReadKeeperWidgetState
}

private object ReadKeeperWidgetLoader {
    suspend fun load(context: Context): ReadKeeperWidgetState = withContext(Dispatchers.IO) {
        runCatching {
            val entryPoint = EntryPointAccessors.fromApplication(
                context,
                ReadKeeperWidgetEntryPoint::class.java,
            )
            val book = entryPoint.bookDao().getLatestReadingBook()?.toDomain()
                ?: return@withContext ReadKeeperWidgetState.Empty
            val coverBitmap = entryPoint.bookCoverCache().bitmap(book.coverUrl)
            ReadKeeperWidgetState.Loaded(
                book = book,
                cover = coverBitmap?.let { ImageProvider(it) },
                coverAspectRatio = coverBitmap?.let { bitmap ->
                    bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)
                } ?: DEFAULT_COVER_ASPECT_RATIO,
                progressLabel = book.widgetProgressLabel(),
                progressFraction = ((book.widgetProgressPercent() ?: 0.0) / 100.0).toFloat()
                    .coerceIn(0f, 1f),
            )
        }.getOrElse {
            ReadKeeperWidgetState.Error
        }
    }

    private const val DEFAULT_COVER_ASPECT_RATIO = 2f / 3f
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReadKeeperWidgetEntryPoint {
    fun bookDao(): BookDao
    fun bookCoverCache(): BookCoverCache
}

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
