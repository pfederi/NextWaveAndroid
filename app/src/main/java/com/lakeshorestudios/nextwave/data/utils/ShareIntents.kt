package com.lakeshorestudios.nextwave.data.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract

/** Builds the share / calendar intents for a wave. */
object ShareIntents {

    private const val WHATSAPP_PACKAGE = "com.whatsapp"

    fun whatsAppInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo(WHATSAPP_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }

    fun whatsApp(text: String): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        setPackage(WHATSAPP_PACKAGE)
        putExtra(Intent.EXTRA_TEXT, text)
    }

    fun sms(text: String): Intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:")
        putExtra("sms_body", text)
    }

    fun mail(subject: String, body: String): Intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

    fun calendar(content: CalendarEventContent): Intent =
        Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, content.title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, content.startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, content.endMillis)
            content.location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
            putExtra(CalendarContract.Events.DESCRIPTION, content.notes)
        }
}
