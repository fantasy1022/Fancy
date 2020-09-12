package com.fantasyfang.fancy.media

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.fantasyfang.fancy.R
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.*

const val NOW_PLAYING_CHANNEL_ID = "com.fantasyfang.fancy.media.NOW_PLAYING"
const val NOW_PLAYING_NOTIFICATION_ID = 0x123 // Arbitrary number used to identify our notification

class MusicNotificationManager(
    private val context: Context,
    sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener
) : CoroutineScope by MainScope() {

    private val notificationManager: PlayerNotificationManager

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)

        notificationManager = PlayerNotificationManager.createWithNotificationChannel(
            context,
            NOW_PLAYING_CHANNEL_ID,
            R.string.notification_channel,
            R.string.notification_channel_description,
            NOW_PLAYING_NOTIFICATION_ID,
            DescriptionAdapter(mediaController),
            notificationListener
        ).apply {
            setMediaSessionToken(sessionToken)
            setSmallIcon(R.drawable.ic_default_cover_icon)
            // Don't display the rewind or fast-forward buttons.
            setRewindIncrementMs(0)
            setFastForwardIncrementMs(0)
        }
    }

    fun hideNotification() {
        notificationManager.setPlayer(null)
    }

    fun showNotificationForPlayer(player: Player) {
        notificationManager.setPlayer(player)
    }

    private inner class DescriptionAdapter(private val controller: MediaControllerCompat) :
        PlayerNotificationManager.MediaDescriptionAdapter {

        var currentIconUri: Uri? = null
        var currentBitmap: Bitmap? = null

        override fun createCurrentContentIntent(player: Player): PendingIntent? =
            controller.sessionActivity

        override fun getCurrentContentText(player: Player) =
            controller.metadata.description.subtitle.toString()

        override fun getCurrentContentTitle(player: Player): CharSequence =
            controller.metadata.description.title.toString()

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            val iconUri = controller.metadata.description.iconUri
            return if (currentIconUri != iconUri || currentBitmap == null) {

                // Cache the bitmap for the current song so that successive calls to
                // `getCurrentLargeIcon` don't cause the bitmap to be recreated.
                currentIconUri = iconUri
                launch {
                    currentBitmap = iconUri?.let {
                        resolveUriAsBitmap(it)
                    }
                    currentBitmap?.let { callback.onBitmap(it) }
                }
                null
            } else {
                currentBitmap
            }
        }

        private suspend fun resolveUriAsBitmap(uri: Uri): Bitmap? {
            return withContext(Dispatchers.IO) {
                // Block on downloading artwork.
                Glide.with(context).applyDefaultRequestOptions(glideOptions)
                    .asBitmap()
                    .load(uri)
                    .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
                    .get()
            }
        }
    }
}

private const val NOTIFICATION_LARGE_ICON_SIZE = 144 // px

private val glideOptions = RequestOptions()
    .fallback(R.drawable.ic_default_cover_background)
    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)