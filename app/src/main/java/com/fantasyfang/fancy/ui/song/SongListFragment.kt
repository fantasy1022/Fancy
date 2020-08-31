package com.fantasyfang.fancy.ui.song

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.fantasyfang.fancy.R

class SongListFragment : Fragment() {

    companion object {
        fun newInstance() = SongListFragment()
    }

    private  val songListViewModel: SongListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.song_list_fragment, container, false)
    }

}