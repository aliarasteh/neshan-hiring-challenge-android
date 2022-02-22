package org.neshan.component.binding

import android.annotation.SuppressLint
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import androidx.databinding.BindingAdapter

/**
 * shows tool tip on a TextView
 * one common use case is when text is ellipsized and user wants to see full text
 * */
@SuppressLint("ClickableViewAccessibility")
@BindingAdapter(value = ["tooltipText", "tooltipShowOnClick"], requireAll = false)
fun setTextViewTooltip(view: TextView, tooltipText: String?, tooltipShowOnClick: Boolean? = false) {
    val gestureDetector = GestureDetectorCompat(view.context, object : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            if (tooltipText != null && tooltipShowOnClick == true && view.isEllipsized()) {
                view.showTooltipBalloon(tooltipText)
            }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (tooltipText != null && view.isEllipsized())
                view.showTooltipBalloon(tooltipText)
        }
    })
    view.setOnTouchListener { v, event ->
        gestureDetector.onTouchEvent(event)
        return@setOnTouchListener if (tooltipShowOnClick == true)
            true
        else
            (v.parent as View).onTouchEvent(event)
    }
}

/**
 * checks if text is larger than TextView space and ellipsized
 * */
fun TextView.isEllipsized(): Boolean {
    try {
        if (layout != null) {
            val lines: Int = layout.lineCount
            if (lines > 0 && layout.getEllipsisCount(lines - 1) > 0)
                return true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false
}
