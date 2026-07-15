package app.linkclear

import android.service.quicksettings.TileService
import app.linkclear.core.cleanText
import app.linkclear.core.removedCount

class CleanTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val text = readClipboard(this)
        if (text.isNullOrBlank()) {
            toast(this, "Clipboard empty")
            return
        }
        val results = Engine.get().cleanText(text)
        if (results.isEmpty()) {
            toast(this, "No link found")
            return
        }
        val cleaned = results.joinToString("\n") { it.cleaned }
        val count = results.sumOf { it.removedCount }
        writeClipboard(this, cleaned)
        toast(
            this,
            if (count == 0) {
                "Already clean"
            } else {
                "Cleaned, $count tracker${if (count == 1) "" else "s"} removed"
            },
        )
    }
}
