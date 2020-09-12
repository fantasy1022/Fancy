package com.fantasyfang.fancy.ui.song

import android.content.ContentResolver
import android.database.ContentObserver
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.*
import com.fantasyfang.fancy.data.Song
import com.fantasyfang.fancy.extension.*
import com.fantasyfang.fancy.media.MusicServiceConnection
import com.fantasyfang.fancy.repository.SongListRepository
import kotlinx.coroutines.launch

private const val TAG = "SongListViewModel"

class SongListViewModel(
    private val contentResolver: ContentResolver,
    private val songListRepository: SongListRepository,
    private val musicServiceConnection: MusicServiceConnection
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

    fun playMedia(mediaItem: Song, pauseAllowed: Boolean = true) {
        val nowPlaying = musicServiceConnection.nowPlaying.value
        val transportControls = musicServiceConnection.transportControls

        val isPrepared = musicServiceConnection.playbackState.value?.isPrepared ?: false
        if (isPrepared && mediaItem.id.toString() == nowPlaying?.id) {
            musicServiceConnection.playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying ->
                        if (pauseAllowed) transportControls.pause() else Unit
                    playbackState.isPlayEnabled -> transportControls.play()
                    else -> {
                        Log.w(
                            TAG, "Playable item clicked but neither play nor pause are enabled!" +
                                    " (mediaId=${mediaItem.id.toString()})"
                        )
                    }
                }
            }
        } else {
            transportControls.playFromMediaId(mediaItem.id.toString(), null)
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
        private val songListRepository: SongListRepository,
        private val musicServiceConnection: MusicServiceConnection
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SongListViewModel(
                contentResolver,
                songListRepository,
                musicServiceConnection
            ) as T
        }
    }
}