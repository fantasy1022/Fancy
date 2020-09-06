package com.fantasyfang.fancy.di

import android.content.Context
import com.fantasyfang.fancy.repository.SongListRepositoryImpl
import com.fantasyfang.fancy.ui.song.SongListViewModel

/**
 * Static methods used to inject classes needed for various Activities and Fragments.
 */
object InjectorUtils {

    fun provideSongListRepository(context: Context) =
        SongListRepositoryImpl(context.contentResolver)

    fun provideSongListViewModel(context: Context): SongListViewModel.Factory {
        val contentResolver = context.contentResolver
        return SongListViewModel.Factory(contentResolver, provideSongListRepository(context))
    }

}