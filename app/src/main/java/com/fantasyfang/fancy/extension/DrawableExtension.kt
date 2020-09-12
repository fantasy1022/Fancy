package com.fantasyfang.fancy.extension

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable

private fun Drawable.getBitmap(): Bitmap =
    Bitmap.createBitmap(
        intrinsicWidth,
        intrinsicHeight, Bitmap.Config.ARGB_8888
    ).apply {
        val canvas = Canvas(this)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
    }

