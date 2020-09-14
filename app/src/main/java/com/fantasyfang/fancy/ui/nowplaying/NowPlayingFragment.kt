package com.fantasyfang.fancy.ui.nowplaying

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.fantasyfang.fancy.R
import com.fantasyfang.fancy.di.InjectorUtils
import com.fantasyfang.fancy.ui.song.SongListViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.now_playing_fragment.*


class NowPlayingFragment : Fragment() {
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private val songListViewModel: SongListViewModel by viewModels {
        InjectorUtils.provideSongListViewModel(requireContext())
    }

    private val nowPlayingViewModel: NowPlayingViewModel by viewModels {
        InjectorUtils.provideNowPlayingViewModel(requireContext())
    }

    companion object {
        const val TAG = "NowPlayingFragment"
        fun newInstance() = NowPlayingFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.now_playing_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setBottomSheetBehavior()
        disableSeekInSmallSeekBar()

        // Attach observers to the LiveData coming from this ViewModel
        nowPlayingViewModel.mediaMetadata.observe(viewLifecycleOwner,
            Observer { mediaItem -> updateUI(view, mediaItem) })

        nowPlayingViewModel.mediaPlayProgress.observe(viewLifecycleOwner,
            Observer { progress ->
                updateProgressBar(progress)
            })

        nowPlayingViewModel.mediaButtonRes.observe(viewLifecycleOwner,
            Observer {
                playPauseImage.setImageState(it, true)
            })

        playPauseImage.setOnClickListener {
            nowPlayingViewModel.mediaMetadata.value?.let { songListViewModel.playMediaId(it.id) }
        }
    }

    private fun setBottomSheetBehavior() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetNowPlayingFragmentLayout)
        bottomSheetBehavior.isHideable = false

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                Log.d(TAG, "slideOffset:$slideOffset")
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> Log.d(TAG, "STATE_COLLAPSED")
                    BottomSheetBehavior.STATE_DRAGGING -> Log.d(TAG, "STATE_DRAGGING")
                    BottomSheetBehavior.STATE_EXPANDED -> Log.d(TAG, "STATE_EXPANDED")
                    BottomSheetBehavior.STATE_HIDDEN -> Log.d(TAG, "STATE_HIDDEN")
                    BottomSheetBehavior.STATE_HALF_EXPANDED -> Log.d(TAG, "STATE_HALF_EXPANDED")
                    BottomSheetBehavior.STATE_SETTLING -> Log.d(TAG, "STATE_SETTLING")
                }
            }
        })
    }

    private fun updateUI(view: View, metadata: NowPlayingViewModel.NowPlayingMetadata) {
        if (metadata.albumArtUri == Uri.EMPTY) {
            smallCover.setImageResource(R.drawable.ic_default_cover_icon)
            smallCover.setBackgroundResource(R.drawable.ic_default_cover_background)
        } else {
            Glide.with(view)
                .load(metadata.albumArtUri)
                .into(smallCover)
        }

        smallTitle.text = metadata.title
        smallSubTitle.text = metadata.subtitle
    }

    private fun updateProgressBar(progress: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            smallSeekBar.setProgress(progress, true)
        } else {
            smallSeekBar.progress = progress
        }
    }

    private fun disableSeekInSmallSeekBar() {
        smallSeekBar.setOnTouchListener { _, _ -> true }
    }

}