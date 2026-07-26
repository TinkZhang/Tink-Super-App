package app.tinks.tink.book

import android.content.Context
import androidx.glance.appwidget.updateAll
import app.tinks.tink.widget.ReadKeeperWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadKeeperWidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun update() {
        ReadKeeperWidget().updateAll(context)
    }
}
