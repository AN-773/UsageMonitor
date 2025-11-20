package sau.odev.usagemonitor.appusagemanager.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

fun Drawable.toBitmap(): Bitmap = if (this is BitmapDrawable) {
    bitmap
} else {
    generateBitmapFromDrawable(this)
}

fun generateBitmapFromDrawable(drawable: Drawable): Bitmap {
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_4444)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
