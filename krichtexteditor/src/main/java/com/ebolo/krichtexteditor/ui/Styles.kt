package com.ebolo.krichtexteditor.ui

import android.widget.ImageView
import com.ebolo.krichtexteditor.utils.toPx
import kotlin.math.roundToInt

fun ImageView.actionImageViewSize() {
    val size = 42f.toPx(context.resources).roundToInt()
    with(layoutParams) {
        width = size
        height = size
    }
}