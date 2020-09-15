package com.fantasyfang.fancy.ui.nowplaying

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.fantasyfang.fancy.R
import com.fantasyfang.fancy.di.InjectorUtils
import com.fantasyfang.fancy.ui.nowplaying.NowPlayingViewModel.NowPlayingMetadata.Companion.timestampToMSS
import com.fantasyfang.fancy.ui.song.SongListViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.now_playing_fragment.*
import kotlinx.android.synthetic.main.section_now_playing_large.*
import kotlinx.android.synthetic.main.section_now_playing_small.*


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
            Observer { progress -> updateProgressBar(progress) })

        nowPlayingViewModel.mediaPosition.observe(viewLifecycleOwner,
            Observer { position -> nowDuration.text = timestampToMSS(requireContext(), position) })

        nowPlayingViewModel.mediaButtonRes.observe(viewLifecycleOwner,
            Observer {
                playPauseImage.setImageState(it, true)
                largePlayPauseButton.setImageState(it, true)
            })

        playPauseImage.setOnClickListener {
            nowPlayingViewModel.mediaMetadata.value?.let { songListViewModel.playMediaId(it.id) }
        }

        largePlayPauseButton.setOnClickListener {
            nowPlayingViewModel.mediaMetadata.value?.let { songListViewModel.playMediaId(it.id) }
        }

        largePreviousButton.setOnClickListener {
            nowPlayingViewModel.skipToPreviousSong()
        }

        largeNextButton.setOnClickListener {
            nowPlayingViewModel.skipToNextSong()
        }
    }

    private fun setBottomSheetBehavior() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetNowPlayingFragmentLayout)
        bottomSheetBehavior.isHideable = false

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                Log.d(TAG, "slideOffset:$slideOffset")
                sectionNowPlayingSmall.alpha = 1 - slideOffset
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> Log.d(TAG, "STATE_COLLAPSED")
                    BottomSheetBehavior.STATE_DRAGGING -> {
                        Log.d(TAG, "STATE_DRAGGING")
                        sectionNowPlayingSmall.visibility = View.VISIBLE
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        Log.d(TAG, "STATE_EXPANDED")
                        sectionNowPlayingSmall.visibility = View.GONE
                    }
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
            largeCover.setImageResource(R.drawable.ic_default_cover_icon)
            largeCover.setBackgroundResource(R.drawable.ic_default_cover_background)

            titleBackground.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.colorPrimary
                )
            )

            largeTitle.setTextColor(Color.WHITE)
            largeSubTitle.setTextColor(Color.WHITE)

        } else {
            Glide.with(view)
                .load(metadata.albumArtUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(smallCover)

            Glide.with(view)
                .load(metadata.albumArtUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(largeCover)

            Glide.with(view)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .load(metadata.albumArtUri)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onLoadCleared(placeholder: Drawable?) {
                        //Nothing
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        Palette.from(resource).generate { palette ->
                            if (palette == null) return@generate
                            setTitleColor(palette)
                        }
                    }
                })

        }

        smallTitle.text = metadata.title
        smallSubTitle.text = metadata.subtitle

        largeTitle.text = metadata.title
        largeSubTitle.text = metadata.subtitle

        totalDuration.text = metadata.duration
    }

    private fun updateProgressBar(progress: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            smallSeekBar.setProgress(progress, true)
            largeSeekBar.setProgress(progress, true)
        } else {
            smallSeekBar.progress = progress
            largeSeekBar.progress = progress
        }
    }

    private fun disableSeekInSmallSeekBar() {
        smallSeekBar.setOnTouchListener { _, _ -> true }
    }

    private fun setTitleColor(palette: Palette) {
        val bodyColor: Int = palette.getDominantColor(
            ContextCompat.getColor(requireContext(), android.R.color.black)
        )

        val titleTextColor =
            palette.getLightVibrantColor(
                ContextCompat.getColor(requireContext(), android.R.color.white)
            )

        val bodyTextColor =
            palette.getLightMutedColor(
                ContextCompat.getColor(requireContext(), android.R.color.white)
            )

        titleBackground.setBackgroundColor(bodyColor)
        largeTitle.setTextColor(titleTextColor)
        largeSubTitle.setTextColor(bodyTextColor)
    }


}