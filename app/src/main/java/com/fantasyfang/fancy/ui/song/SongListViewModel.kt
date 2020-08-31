package com.fantasyfang.fancy.ui.song

import android.content.ContentResolver
import android.database.ContentObserver
import android.provider.MediaStore
import androidx.lifecycle.*
import com.fantasyfang.fancy.data.Song
import com.fantasyfang.fancy.extension.registerObserver
import com.fantasyfang.fancy.repository.SongListRepository
import kotlinx.coroutines.launch

class SongListViewModel(
    private val contentResolver: ContentResolver,
    private val songListRepository: SongListRepository
) : ViewModel() {

    private var contentObserver: ContentObserver? = null
    private val _songs = MutableLiveData<List<Song>>()
    val songs: LiveData<List<Song>> get() = _songs

    fun loadSongs() {
        viewModelScope.launch {
            val songList = querySongs()
            _songs.postValue(songList)
            if (contentObserver == null) {
                contentObserver = contentResolver.registerObserver(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                ) {
                    loadSongs()
                }
            }
        }
    }

    private suspend fun querySongs(): List<Song> =
        songListRepository.getSongs()

    override fun onCleared() {
        contentObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
    }

    class Factory(
        private val contentResolver: ContentResolver,
        private val songListRepository: SongListRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SongListViewModel(contentResolver, songListRepository) as T
        }
    }
}