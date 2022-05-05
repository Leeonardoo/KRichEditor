package com.ebolo.krichtexteditor.utils

import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.TypedValue
import java.util.regex.Pattern

fun rgbToHex(rgb: String): String? {
    val c = Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)")
    val m = c.matcher(rgb)
    return if (m.matches()) {
        String.format("#%02x%02x%02x", Integer.valueOf(m.group(1)),
                Integer.valueOf(m.group(2)), Integer.valueOf(m.group(3)))
    } else null
}

internal fun Float.toDp(resources: Resources): Float {
    return this / (resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
}

internal fun Float.toPx(resources: Resources): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        resources.displayMetrics
    )
}