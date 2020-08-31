package com.fantasyfang.fancy.repository

import com.fantasyfang.fancy.data.Song

interface SongListRepository {

    suspend fun getSongs(): List<Song>
}