package com.fantasyfang.fancy.extension

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler


/**
 * Convenience extension method to register a [ContentObserver] given a lambda.
 */
fun ContentResolver.registerObserver(
    uri: Uri,
    observer: (selfChange: Boolean) -> Unit
): ContentObserver {
    val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            observer(selfChange)
        }
    }
    registerContentObserver(uri, true, contentObserver)
    return contentObserver
}
