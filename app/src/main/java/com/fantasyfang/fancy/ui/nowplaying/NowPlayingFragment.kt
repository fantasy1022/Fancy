package com.fantasyfang.fancy.ui.nowplaying

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.fantasyfang.fancy.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.now_playing_fragment.*


private const val TAG = "NowPlayingFragment"

class NowPlayingFragment : Fragment() {
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    companion object {
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

}