package app.linkclear

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

fun readClipboard(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = cm.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
}

fun writeClipboard(
    context: Context,
    text: String,
) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Link Clear", text))
}

fun toast(
    context: Context,
    msg: String,
) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}
