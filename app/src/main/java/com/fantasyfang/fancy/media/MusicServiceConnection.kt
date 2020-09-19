package com.fantasyfang.fancy.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.fantasyfang.fancy.extension.displayIconUri
import com.fantasyfang.fancy.extension.displaySubtitle
import com.fantasyfang.fancy.extension.id
import com.fantasyfang.fancy.extension.title
import com.fantasyfang.fancy.widget.MusicAppWidget
import com.fantasyfang.fancy.widget.WidgetConstants

private const val TAG = "MusicServiceConnection"

class MusicServiceConnection(val context: Context, serviceComponent: ComponentName) {
    val isConnected = MutableLiveData<Boolean>()
        .apply { postValue(false) }
    val playbackState = MutableLiveData<PlaybackStateCompat>()
        .apply { postValue(EMPTY_PLAYBACK_STATE) }
    val nowPlaying = MutableLiveData<MediaMetadataCompat>()
        .apply { postValue(NOTHING_PLAYING) }

    private lateinit var mediaController: MediaControllerCompat
    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)
    private val mediaBrowser = MediaBrowserCompat(
        context,
        serviceComponent,
        mediaBrowserConnectionCallback, null
    ).apply { connect() }


    private inner class MediaBrowserConnectionCallback(private val context: Context) :
        MediaBrowserCompat.ConnectionCallback() {
        /**
         * Invoked after [MediaBrowserCompat.connect] when the request has successfully
         * completed.
         */
        override fun onConnected() {
            Log.d(TAG, "MediaBrowserConnectionCallback onConnected")
            // Get a MediaController for the MediaSession.
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }

            isConnected.postValue(true)
        }

        /**
         * Invoked when the client is disconnected from the media browser.
         */
        override fun onConnectionSuspended() {
            Log.d(TAG, "MediaBrowserConnectionCallback onConnectionSuspended")
            isConnected.postValue(false)
        }

        /**
         * Invoked when the connection to the media browser failed.
         */
        override fun onConnectionFailed() {
            Log.d(TAG, "MediaBrowserConnectionCallback onConnectionFailed")
            isConnected.postValue(false)
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            Log.d(TAG, "MediaControllerCallback onPlaybackStateChanged:$state")
            playbackState.postValue(state ?: EMPTY_PLAYBACK_STATE)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Log.d(TAG, "MediaControllerCallback onMetadataChanged:$metadata")
            nowPlaying.postValue(
                if (metadata?.id == null) {
                    NOTHING_PLAYING
                } else {
                    sendWidgetIntent(metadata)
                    metadata
                }
            )
        }
    }

    private fun sendWidgetIntent(metadata: MediaMetadataCompat) {
        val intent = Intent(context, MusicAppWidget::class.java).apply {
            action = WidgetConstants.METADATA_CHANGED
            putExtra(WidgetConstants.ARGUMENT_SONG_ID, metadata.id)
            putExtra(WidgetConstants.ARGUMENT_TITLE, metadata.title)
            putExtra(WidgetConstants.ARGUMENT_SUBTITLE, metadata.displaySubtitle)
            putExtra(WidgetConstants.ARGUMENT_COVER_URI, metadata.displayIconUri.toString())
        }
        context.sendBroadcast(intent)
    }

    companion object {
        // For Singleton instantiation.
        @Volatile
        private var instance: MusicServiceConnection? = null

        fun getInstance(context: Context, serviceComponent: ComponentName) =
            instance ?: synchronized(this) {
                instance ?: MusicServiceConnection(context, serviceComponent)
                    .also { instance = it }
            }
    }
}


@Suppress("PropertyName")
val EMPTY_PLAYBACK_STATE: PlaybackStateCompat = PlaybackStateCompat.Builder()
    .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
    .build()

@Suppress("PropertyName")
val NOTHING_PLAYING: MediaMetadataCompat = MediaMetadataCompat.Builder()
    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
    .build()