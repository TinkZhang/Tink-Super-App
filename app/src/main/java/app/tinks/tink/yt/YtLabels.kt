package app.tinks.tink.yt

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Locale

fun labelKey(name: String): String = name.lowercase(Locale.ROOT)

fun parseEpisodeLabels(input: String): List<String> {
    val labels = input.split(',', '\n').map { it.trim().replace(Regex("\\s+"), " ") }
        .filter { it.isNotEmpty() }.distinctBy(::labelKey)
    require(labels.size <= 10) { "Use up to 10 labels per episode." }
    require(labels.all { it.length <= 40 && it.none(Char::isISOControl) }) { "Keep each label within 40 characters." }
    return labels
}

fun episodeLabels(item: YtDownload): List<String> = if (item.standalone) item.labels.split('\n').filter { it.isNotBlank() } else emptyList()
fun libraryLabels(items: List<YtDownload>): List<String> = items.flatMap(::episodeLabels).distinctBy(::labelKey).sortedBy(::labelKey)

val YT_MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE yt_download ADD COLUMN labels TEXT NOT NULL DEFAULT ''")
    }
}
