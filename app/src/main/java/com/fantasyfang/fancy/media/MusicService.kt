package com.fantasyfang.fancy.media

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import com.fantasyfang.fancy.BuildConfig
import com.fantasyfang.fancy.R
import com.fantasyfang.fancy.di.InjectorUtils
import com.fantasyfang.fancy.extension.toMediaMetadataCompat
import com.fantasyfang.fancy.extension.toMediaSource
import com.fantasyfang.fancy.repository.SongListRepository
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


class MusicService : MediaBrowserServiceCompat(), CoroutineScope by MainScope() {

    private val TAG = MusicService::class.java.simpleName
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var songListRepository: SongListRepository
    private lateinit var notificationManager: MusicNotificationManager

    private var currentPlaylistItems: List<MediaMetadataCompat> = emptyList()
    private var isForegroundService = false

    private lateinit var currentPlayer: Player

    private val exoPlayer: ExoPlayer by lazy {
        SimpleExoPlayer.Builder(this).build().apply {
            setAudioAttributes(musicAudioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            addListener(playerListener)
        }
    }

    private val playerListener = PlayerEventListener()

    private val musicAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val dataSourceFactory: DefaultDataSourceFactory by lazy {
        DefaultDataSourceFactory(this, Util.getUserAgent(this, MUSIC_USER_AGENT), null)
    }

    override fun onCreate() {
        super.onCreate()
        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(this, 0, sessionIntent, 0)
            }

        mediaSession = MediaSessionCompat(this, TAG)
            .apply {
                setSessionActivity(sessionActivityPendingIntent)
                isActive = true
            }

        sessionToken = mediaSession.sessionToken

        songListRepository = InjectorUtils.provideSongListRepository(this)
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(MusicPlaybackPreparer())
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator(mediaSession))

        switchToPlayer(previousPlayer = null, newPlayer = exoPlayer)

        notificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            PlayerNotificationListener()
        )

        notificationManager.showNotificationForPlayer(currentPlayer)
    }

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

    private inner class MusicPlaybackPreparer : MediaSessionConnector.PlaybackPreparer {
        override fun onPrepareFromSearch(
            query: String, playWhenReady: Boolean,
            extras: Bundle?
        ) = Unit

        override fun onCommand(
            player: Player, controlDispatcher: ControlDispatcher, command: String,
            extras: Bundle?, cb: ResultReceiver?
        ): Boolean = false

        override fun getSupportedPrepareActions(): Long =
            PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                    PlaybackStateCompat.ACTION_PLAY_FROM_URI

        override fun onPrepareFromMediaId(
            mediaId: String, playWhenReady: Boolean,
            extras: Bundle?
        ) {
            launch {
                val itemToPlay = songListRepository.getSongs().find { item ->
                    item.id.toString() == mediaId
                }?.toMediaMetadataCompat()

                val playlist = songListRepository.getSongs().toMediaMetadataCompat()

                if (itemToPlay == null) {
                    Log.w(TAG, "Content not found: MediaID=$mediaId")
                } else {
                    preparePlaylist(
                        playlist,
                        itemToPlay,
                        playWhenReady,
                        0
                    )
                }
            }
        }

        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit

        override fun onPrepare(playWhenReady: Boolean) = Unit
    }

    private inner class MusicQueueNavigator(
        mediaSession: MediaSessionCompat
    ) : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat =
            currentPlaylistItems[windowIndex].description
    }

    private inner class PlayerEventListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {
                    notificationManager.showNotificationForPlayer(currentPlayer)
                    // If playback is paused we remove the foreground state which allows the
                    // notification to be dismissed. An alternative would be to provide a "close"
                    // button in the notification which stops playback and clears the notification.
                    if (playbackState == Player.STATE_READY) {
                        if (!playWhenReady) stopForeground(false)
                    }
                }
                else -> {
                    notificationManager.hideNotification()
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            var message = R.string.generic_error
            when (error.type) {
                // If the data from MediaSource object could not be loaded the Exoplayer raises
                // a type_source error.
                // An error message is printed to UI via Toast message to inform the user.
                ExoPlaybackException.TYPE_SOURCE -> {
                    message = R.string.error_media_not_found
                    Log.e(TAG, "TYPE_SOURCE: " + error.sourceException.message)
                }
                // If the error occurs in a render component, Exoplayer raises a type_remote error.
                ExoPlaybackException.TYPE_RENDERER -> {
                    Log.e(TAG, "TYPE_RENDERER: " + error.rendererException.message)
                }
                // If occurs an unexpected RuntimeException Exoplayer raises a type_unexpected error.
                ExoPlaybackException.TYPE_UNEXPECTED -> {
                    Log.e(TAG, "TYPE_UNEXPECTED: " + error.unexpectedException.message)
                }
                // Occurs when there is a OutOfMemory error.
                ExoPlaybackException.TYPE_OUT_OF_MEMORY -> {
                    Log.e(TAG, "TYPE_OUT_OF_MEMORY: " + error.outOfMemoryError.message)
                }
                // If the error occurs in a remote component, Exoplayer raises a type_remote error.
                ExoPlaybackException.TYPE_REMOTE -> {
                    Log.e(TAG, "TYPE_REMOTE: " + error.message)
                }
            }
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun switchToPlayer(previousPlayer: Player?, newPlayer: Player) {
        if (previousPlayer == newPlayer) {
            return
        }
        currentPlayer = newPlayer
        if (previousPlayer != null) {
            val playbackState = previousPlayer.playbackState
            if (currentPlaylistItems.isEmpty()) {
                // We are joining a playback session. Loading the session from the new player is
                // not supported, so we stop playback.
                currentPlayer.stop(/* reset= */true)
            } else if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
                preparePlaylist(
                    metadataList = currentPlaylistItems,
                    itemToPlay = currentPlaylistItems[previousPlayer.currentWindowIndex],
                    playWhenReady = previousPlayer.playWhenReady,
                    playbackStartPositionMs = previousPlayer.currentPosition
                )
            }
        }
        mediaSessionConnector.setPlayer(newPlayer)
        previousPlayer?.stop(/* reset= */true)
    }

    private fun preparePlaylist(
        metadataList: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?, playWhenReady: Boolean,
        playbackStartPositionMs: Long
    ) {
        val initialWindowIndex = if (itemToPlay == null) 0 else metadataList.indexOf(itemToPlay)
        currentPlaylistItems = metadataList

        currentPlayer.playWhenReady = playWhenReady
        currentPlayer.stop(/* reset= */ true)
        if (currentPlayer == exoPlayer) {
            val mediaSource = metadataList.toMediaSource(dataSourceFactory)
            exoPlayer.prepare(mediaSource)
            exoPlayer.seekTo(initialWindowIndex, playbackStartPositionMs)
        }
    }

    private inner class PlayerNotificationListener :
        PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            if (ongoing && !isForegroundService) {
                ContextCompat.startForegroundService(
                    applicationContext,
                    Intent(applicationContext, this@MusicService.javaClass)
                )

                startForeground(notificationId, notification)
                isForegroundService = true
            }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopForeground(true)
            isForegroundService = false
            stopSelf()
        }
    }
}

const val FANCY_BROWSABLE_ROOT = "/"
const val FANCY_EMPTY_ROOT = "@empty@"
private const val MUSIC_USER_AGENT = "music.agent"