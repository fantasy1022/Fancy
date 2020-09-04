package com.fantasyfang.fancy.ui.song

import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.fantasyfang.fancy.R
import com.fantasyfang.fancy.data.Song
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_song_layout.view.*

class SongListAdapter(private val onClick: (Song) -> Unit) :
    ListAdapter<Song, SongViewHolder>(Song.DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.item_song_layout, parent, false)
        return SongViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val mediaStoreSong = getItem(position)
        holder.itemView.songLayout.tag = mediaStoreSong

        holder.itemView.songNameText.text = mediaStoreSong.title
        holder.itemView.artistNameText.text = mediaStoreSong.artistName

        Glide.with(holder.itemView.context)
            .asBitmap()
            .load(mediaStoreSong.coverPath)
            .thumbnail(0.33f)
            .centerCrop()
            .error(R.drawable.ic_default_cover_icon)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {
                    //Do nothing
                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    holder.itemView.songCoverImage.setImageBitmap(resource)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    holder.itemView.songCoverImage.setBackgroundResource(R.drawable.ic_default_cover_background)
                    holder.itemView.songCoverImage.setImageDrawable(errorDrawable)
                }
            })
    }
}


class SongViewHolder(private val view: View, onClick: (Song) -> Unit) :
    RecyclerView.ViewHolder(view), LayoutContainer {

    override val containerView: View?
        get() = view

    init {
        view.songLayout.setOnClickListener {
            val song = view.songLayout.tag as? Song ?: return@setOnClickListener
            onClick(song)
        }
    }
}
