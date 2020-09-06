package com.fantasyfang.fancy.media

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import com.fantasyfang.fancy.BuildConfig

class MusicService : MediaBrowserServiceCompat() {

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        //Do nothing
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        val isKnownCaller = allowBrowsing(clientPackageName)
        return if (isKnownCaller) {
            BrowserRoot(FANCY_BROWSABLE_ROOT, null)
        } else {
            if (BuildConfig.DEBUG) {
                BrowserRoot(FANCY_EMPTY_ROOT, null)
            } else {
                null
            }
        }
    }

    private fun allowBrowsing(clientPackageName: String): Boolean {
        return clientPackageName == packageName
    }
}

const val FANCY_BROWSABLE_ROOT = "/"
const val FANCY_EMPTY_ROOT = "@empty@"