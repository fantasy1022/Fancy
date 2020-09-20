package com.fantasyfang.fancy.widget

object WidgetConstants {

    private const val TAG = "WidgetConstants"
    const val METADATA_CHANGED = "$TAG.METADATA_CHANGED"
    const val STATE_CHANGED = "$TAG.STATE_CHANGED"

    //    arguments metadata
    const val ARGUMENT_SONG_ID = "$TAG.ARGUMENT_SONG_ID"
    const val ARGUMENT_TITLE = "$TAG.ARGUMENT_TITLE"
    const val ARGUMENT_SUBTITLE = "$TAG.ARGUMENT_SUBTITLE"
    const val ARGUMENT_COVER_URI = "$TAG.ARGUMENT_COVER_URI"

    //    arguments state
    const val ARGUMENT_IS_PLAYING = "$TAG.ARGUMENT_IS_PLAYING"

    enum class WidgetAction {
        PLAY, PLAY_PAUSE, SKIP_NEXT, SKIP_PREVIOUS
    }
}