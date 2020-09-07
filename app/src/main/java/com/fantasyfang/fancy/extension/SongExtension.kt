package com.fantasyfang.fancy.extension

import android.support.v4.media.MediaMetadataCompat
import com.fantasyfang.fancy.data.Song


fun Song.toMediaMetadataCompat(): MediaMetadataCompat =
    MediaMetadataCompat.Builder().also {
        it.id = id.toString()
        it.title = title
        it.artist = artistName
        it.albumArtUri = coverPath
        it.mediaUri = contentUri.toString()

        it.displayTitle = title
        it.displaySubtitle = artistName
        it.displayDescription = albumName
        it.displayIconUri = coverPath
    }.build()

fun List<Song>.toMediaMetadataCompat(): List<MediaMetadataCompat> =
    this.map { it.toMediaMetadataCompat() }