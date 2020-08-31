package com.fantasyfang.fancy.data

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

data class Song(
    val id: Long,
    val title: String,
    val artistName: String,
    val albumId: Long,
    val albumName: String,
    val coverPath: String,
    val contentUri: Uri
) {
    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Song>() {
            override fun areItemsTheSame(oldItem: Song, newItem: Song) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Song, newItem: Song) =
                oldItem == newItem
        }
    }
}
