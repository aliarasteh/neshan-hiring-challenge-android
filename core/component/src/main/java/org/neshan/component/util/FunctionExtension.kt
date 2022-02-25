package org.neshan.component.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import org.neshan.component.R
import org.neshan.component.view.snackbar.SnackBar
import org.neshan.component.view.snackbar.SnackBarType
import org.neshan.data.model.error.GeneralError
import org.neshan.data.model.error.NetworkError
import org.neshan.data.model.error.SimpleError

fun Drawable.toBitmap(): Bitmap {

    if (this is BitmapDrawable) {
        return this.bitmap
    }

    val bitmap: Bitmap = Bitmap.createBitmap(
        this.intrinsicWidth,
        this.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)

    return bitmap

}

fun showError(rootView: View, error: GeneralError) {
    when (error) {
        is NetworkError -> {
            SnackBar.make(rootView, R.string.network_connection_error, SnackBarType.ERROR).show()
        }
        is SimpleError -> {
            SnackBar.make(rootView, error.errorMessage, SnackBarType.ERROR).show()
        }
        is UnknownError -> {
            SnackBar.make(rootView, R.string.unknown_error, SnackBarType.ERROR).show()
        }
    }
}
