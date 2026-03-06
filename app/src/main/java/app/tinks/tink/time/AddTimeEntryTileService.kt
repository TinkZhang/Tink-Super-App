package app.tinks.tink.time

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.tinks.tink.MainActivity
import app.tinks.tink.R

class AddTimeEntryTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = getString(R.string.qs_tile_add_time_label)
            subtitle = getString(R.string.qs_tile_add_time_subtitle)
            contentDescription = getString(R.string.qs_tile_add_time_content_description)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_ADD_TIME_ENTRY_FROM_TILE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_CODE_ADD_TIME_ENTRY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        startActivityAndCollapse(pendingIntent)
    }

    private companion object {
        private const val REQUEST_CODE_ADD_TIME_ENTRY = 2606
    }
}

