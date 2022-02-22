package org.neshan.component.util

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import org.neshan.component.R

object FontManager {
    private val typeFaceMap = mutableMapOf<Int, Typeface>()

    fun getAppFont(context: Context): Typeface {
        return getFont(context, R.font.dana_fa_num_medium)
    }

    fun getFont(context: Context, resource: Int): Typeface {
        return if (typeFaceMap.containsKey(resource)) {
            typeFaceMap[resource]!!
        } else {
            val typeface = ResourcesCompat.getFont(context, resource) ?: Typeface.DEFAULT
            typeFaceMap[resource] = typeface
            typeface
        }
    }
}