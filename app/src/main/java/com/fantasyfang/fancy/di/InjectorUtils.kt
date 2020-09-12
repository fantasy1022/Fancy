package com.fantasyfang.fancy.di

import android.content.ComponentName
import android.content.Context
import com.fantasyfang.fancy.media.MusicService
import com.fantasyfang.fancy.media.MusicServiceConnection
import com.fantasyfang.fancy.repository.SongListRepositoryImpl
import com.fantasyfang.fancy.ui.song.SongListViewModel

/**
 * Static methods used to inject classes needed for various Activities and Fragments.
 */
object InjectorUtils {

    private fun provideMusicServiceConnection(context: Context): MusicServiceConnection {
        return MusicServiceConnection.getInstance(
            context,
            ComponentName(context, MusicService::class.java)
        )
    }

    fun provideSongListRepository(context: Context) =
        SongListRepositoryImpl(context.contentResolver)

    fun provideSongListViewModel(context: Context): SongListViewModel.Factory {
        val contentResolver = context.contentResolver
        val musicServiceConnection = provideMusicServiceConnection(context)
        return SongListViewModel.Factory(
            contentResolver,
            provideSongListRepository(context),
            musicServiceConnection
        )
    }

}