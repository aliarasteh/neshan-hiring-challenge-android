package org.neshan.component.binding

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.createBalloon
import org.neshan.component.R
import org.neshan.component.util.FontManager

/**
 * activates showing tool tip on view
 * */
@SuppressLint("ClickableViewAccessibility")
@BindingAdapter(value = ["tooltipText", "tooltipShowOnClick"], requireAll = false)
fun setViewTooltip(view: View, tooltipText: String?, tooltipShowOnClick: Boolean? = false) {
    val gestureDetector =
        GestureDetectorCompat(view.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                if (tooltipText != null && tooltipShowOnClick == true) {
                    view.showTooltipBalloon(tooltipText)
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (tooltipText != null)
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
 * shows tool tip on a View
 * */
fun View.showTooltipBalloon(tooltipText: String) {
    val balloon = createBalloon(context) {
        isVisibleArrow = false
        autoDismissDuration = 3000
        dismissWhenClicked = true
        dismissWhenLifecycleOnPause = true
        width = BalloonSizeSpec.WRAP
        height = BalloonSizeSpec.WRAP
        text = tooltipText
        balloonAnimation = BalloonAnimation.FADE
        lifecycleOwner = findViewTreeLifecycleOwner()
        textTypefaceObject = FontManager.getAppFont(context)
        setTextSizeResource(R.dimen.text_size_12)
        setAlpha(0.9f)
        setCornerRadiusResource(R.dimen.radius_7)
        setTextColorResource(R.color.colorWhite)
        setBackgroundColorResource(R.color.colorGrayDarker)
        setPaddingHorizontalResource(R.dimen.margin_15)
        setPaddingVerticalResource(R.dimen.margin_10)
        setMarginResource(R.dimen.margin_15)
    }
    balloon.showAtCenter(this)
}