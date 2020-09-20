package com.fantasyfang.fancy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.fantasyfang.fancy.MainActivity
import com.fantasyfang.fancy.R
import com.fantasyfang.fancy.extension.asServicePendingIntent
import com.fantasyfang.fancy.media.MusicService


private const val TAG = "MusicAppWidget"

class MusicAppWidget : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        Log.d(TAG, "onEnabled")
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.d(TAG, "onDisabled")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager =
            context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager
        val appWidgetIds =
            appWidgetManager.getAppWidgetIds(ComponentName(context, MusicAppWidget::class.java))

        when (intent.action) {
            WidgetConstants.STATE_CHANGED -> {
                val isPlaying = intent.getBooleanExtra(WidgetConstants.ARGUMENT_IS_PLAYING, false)
                for (appWidgetId in appWidgetIds) {
                    stateChanged(context, isPlaying, appWidgetManager, appWidgetId)
                }
            }

            WidgetConstants.METADATA_CHANGED -> {
                val id = intent.getLongExtra(WidgetConstants.ARGUMENT_SONG_ID, 0)
                val title = intent.getStringExtra(WidgetConstants.ARGUMENT_TITLE)!!
                val subtitle = intent.getStringExtra(WidgetConstants.ARGUMENT_SUBTITLE)!!
                val coverPath = intent.getStringExtra(WidgetConstants.ARGUMENT_COVER_URI)!!
                val metadata = WidgetMetadata(id, title, subtitle, coverPath)
                for (appWidgetId in appWidgetIds) {
                    metaChanged(context, metadata, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    private fun stateChanged(
        context: Context,
        isPlaying: Boolean,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.music_app_widget)
        if (isPlaying) {
            views.setImageViewResource(R.id.play, R.drawable.vd_pause)
        } else {
            views.setImageViewResource(R.id.play, R.drawable.vd_play)
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun metaChanged(
        context: Context,
        widgetMetadata: WidgetMetadata,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.music_app_widget)
        views.setTextViewText(R.id.title, widgetMetadata.title)
        views.setTextViewText(R.id.subtitle, widgetMetadata.subtitle)

        setPendingIntent(views, context)

        Glide.with(context)
            .asBitmap()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .load(widgetMetadata.coverUri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    Log.d("Fan", "onLoadFailed")
                    views.setImageViewResource(R.id.cover, R.drawable.ic_default_cover_icon)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    //Nothing
                }

                override fun onResourceReady(
                    resource: Bitmap, transition: Transition<in Bitmap>?
                ) {
                    views.setImageViewBitmap(R.id.cover, resource)
                    Palette.from(resource).generate { palette ->
                        if (palette == null) return@generate

                        views.setInt(
                            R.id.background, "setBackgroundColor", palette.getDominantColor(
                                ContextCompat.getColor(context, android.R.color.black)
                            )
                        )

                        views.setTextColor(
                            R.id.title,
                            palette.getLightVibrantColor(
                                ContextCompat.getColor(
                                    context,
                                    android.R.color.white
                                )
                            )
                        )

                        views.setTextColor(
                            R.id.subtitle,
                            palette.getLightMutedColor(
                                ContextCompat.getColor(
                                    context,
                                    android.R.color.white
                                )
                            )
                        )

                        setMediaButtonColors(
                            views, palette.getLightVibrantColor(
                                ContextCompat.getColor(
                                    context,
                                    android.R.color.white
                                )
                            )
                        )

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            })
    }

    private fun setPendingIntent(remoteViews: RemoteViews, context: Context) {
        remoteViews.setOnClickPendingIntent(
            R.id.previous,
            buildPendingIntent(context, WidgetConstants.WidgetAction.SKIP_PREVIOUS.name)
        )

        remoteViews.setOnClickPendingIntent(
            R.id.play,
            buildPendingIntent(context, WidgetConstants.WidgetAction.PLAY_PAUSE.name)
        )

        remoteViews.setOnClickPendingIntent(
            R.id.next,
            buildPendingIntent(context, WidgetConstants.WidgetAction.SKIP_NEXT.name)
        )

        remoteViews.setOnClickPendingIntent(R.id.cover, buildContentIntent(context))
    }

    private fun setMediaButtonColors(remoteViews: RemoteViews, color: Int) {
        remoteViews.setInt(R.id.previous, "setColorFilter", color)
        remoteViews.setInt(R.id.play, "setColorFilter", color)
        remoteViews.setInt(R.id.next, "setColorFilter", color)
    }

    private fun buildPendingIntent(context: Context, action: String): PendingIntent? {
        val intent = Intent(context, MusicService::class.java)
        intent.action = action
        return intent.asServicePendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun buildContentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context, 0,
            intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

data class WidgetMetadata(
    val id: Long,
    val title: String,
    val subtitle: String,
    val coverUri: String
)