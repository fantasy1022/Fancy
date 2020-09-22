package com.fantasyfang.fancy.ui.song

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.fantasyfang.fancy.R
import com.fantasyfang.fancy.data.Song
import com.fantasyfang.fancy.di.InjectorUtils
import com.fantasyfang.fancy.ui.nowplaying.NowPlayingFragment
import kotlinx.android.synthetic.main.fragment_song_list.*

private const val READ_EXTERNAL_STORAGE_REQUEST = 1

class SongListFragment : Fragment() {

    companion object {
        fun newInstance() = SongListFragment()
    }

    private val songListViewModel: SongListViewModel by viewModels() {
        InjectorUtils.provideSongListViewModel(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_song_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val songAdapter = SongListAdapter {
            clickSong(it)
        }

        with(songRecyclerView) {
            layoutManager = LinearLayoutManager(activity)
            adapter = songAdapter
        }
        songListViewModel.songs.observe(this.viewLifecycleOwner, Observer<List<Song>> { songs ->
            songAdapter.submitList(songs)
        })

        openMediaStore()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showSongs()
                }
            }
        }
    }

    fun playSongDirectly() {
        Handler().postDelayed({
            clickSong(songListViewModel.songs.value?.getOrNull(0) ?: return@postDelayed)
        }, 2000)
    }

    private fun haveStoragePermission() =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() {
        if (!haveStoragePermission()) {
            val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            requestPermissions(
                permissions,
                READ_EXTERNAL_STORAGE_REQUEST
            )
        }
    }

    private fun openMediaStore() {
        if (haveStoragePermission()) {
            showSongs()
        } else {
            requestPermission()
        }
    }

    private fun showSongs() {
        songListViewModel.loadSongs()
    }

    private fun clickSong(song: Song) {
        songListViewModel.playMedia(song)
        showNowPlaying()
    }

    private fun showNowPlaying() {
        with(parentFragmentManager.beginTransaction()) {
            val fragment = parentFragmentManager.findFragmentByTag(NowPlayingFragment.TAG)
            if (fragment == null) {
                add(
                    R.id.nav_host_fragment,
                    NowPlayingFragment.newInstance(),
                    NowPlayingFragment.TAG
                )
                commit()
            }
        }
    }
}