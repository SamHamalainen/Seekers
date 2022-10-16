package com.example.seekers.general

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.util.Log
import android.util.Size
import android.view.WindowManager
import com.example.seekers.R
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.Timestamp
import com.google.maps.android.ktx.utils.withSphericalOffset
import io.github.g0dkar.qrcode.QRCode
import java.io.ByteArrayOutputStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.*

// Generate QRCode that will be read as a string when scanned
// uses https://github.com/g0dkar/qrcode-kotlin library
fun generateQRCode(data: String): Bitmap {
    val fileOut = ByteArrayOutputStream()

    QRCode(data)
        .render(cellSize = 50, margin = 25)
        .writeImage(fileOut)

    val imageBytes = fileOut.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

// Get correct avatar based on passed in value
fun getAvatarId(id: Int): Int {
    return when (id) {
        0 -> R.drawable.bee
        1 -> R.drawable.chameleon
        2 -> R.drawable.chick
        3 -> R.drawable.cow
        4 -> R.drawable.crab
        5 -> R.drawable.dog
        6 -> R.drawable.elephant
        7 -> R.drawable.fox
        8 -> R.drawable.koala
        9 -> R.drawable.lion
        10 -> R.drawable.penguin
        else -> R.drawable.whale
    }
}

// Check if email is in correct format using a regex.
fun isEmailValid(email: String): Boolean {
    val EMAIL_REGEX = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})"
    val result = EMAIL_REGEX.toRegex().matches(email)
    Log.d("validation", result.toString())
    return result
}

// Check if password is strong enough using a regex.
// Requires:
// - Minimum 8 characters
// - At least 1 capital letter
// - At least 1 number
fun isPasswordValid(password: String): Boolean {
    val PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$"
    val result = PASSWORD_REGEX.toRegex().matches(password)
    Log.d("validation", result.toString())
    return result
}

// Added custom function to Bitmap. Is used to convert a bitmap into a grayscaled version.
fun Bitmap.toGrayscale(): Bitmap {

    val matrix = ColorMatrix().apply { setSaturation(0f) }
    val filter = ColorMatrixColorFilter(matrix)
    val paint = Paint().apply { colorFilter = filter }
    Canvas(this).drawBitmap(this, 0f, 0f, paint)
    return this
}

// Converts seconds to a more readable format. Returns converted version as string
fun secondsToText(seconds: Int): String {
    if (seconds == 0) {
        return "Time's up!"
    }
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds - hours * 3600 - minutes * 60

    if (seconds < 3600) {
        return "${minutes.toTimeString()}:${secs.toTimeString()}"
    }

    return "${hours.toTimeString()}:${minutes.toTimeString()}:${secs.toTimeString()}"
}

// Converting Integers under 10 to "clock format" for example 8 would be 08
// otherwise just return Int as string
fun Int.toTimeString() = if (this < 10) "0$this" else this.toString()

// Get the bounds of the map so you are not able to go far away from playing area
fun getBounds(center: LatLng, radius: Int): LatLngBounds {
    val multiplier = cos(PI / 4)
    val sw = center.withSphericalOffset(radius.div(multiplier), 225.0)
    val ne = center.withSphericalOffset(radius.div(multiplier), 45.0)
    return LatLngBounds(sw, ne)
}

// Getting the corner coordinates within bounds.
// To put a darker polygon maximum size outside of the playing area
fun getCornerCoords(center: LatLng, radius: Int): List<LatLng> {
    val ne = center.withSphericalOffset(radius * 10.0, 45.0)
    val se = center.withSphericalOffset(radius * 10.0, 135.0)
    val sw = center.withSphericalOffset(radius * 10.0, 225.0)
    val nw = center.withSphericalOffset(radius * 10.0, 315.0)
    return listOf(ne, se, sw, nw)
}

// Getting the coordinates of the edge of the playing area circle
// Used for making a hole inside of the darker polygon outside the playing area
fun getCircleCoords(center: LatLng, radius: Int): List<LatLng> {
    val list = mutableListOf<LatLng>()
    (0..360).forEach {
        list.add(center.withSphericalOffset(radius.toDouble() + 1.0, it.toDouble()))
    }
    return list
}

// Setting the zoom level based on the bounds given
// source: https://stackoverflow.com/questions/6048975/google-maps-v3-how-to-calculate-the-zoom-level-for-a-given-bounds
fun getBoundsZoomLevel(bounds: LatLngBounds, mapDim: Size): Double {
    val WORLD_DIM = Size(256, 256)
    val ZOOM_MAX = 21.toDouble()

    fun latRad(lat: Double): Double {
        val sin = sin(lat * Math.PI / 180)
        val radX2 = ln((1 + sin) / (1 - sin)) / 2
        return max(min(radX2, Math.PI), -Math.PI) / 2
    }

    fun zoom(mapPx: Int, worldPx: Int, fraction: Double): Double {
        return floor(ln(mapPx / worldPx / fraction) / ln(2.0))
    }

    val ne = bounds.northeast
    val sw = bounds.southwest

    val latFraction = (latRad(ne.latitude) - latRad(sw.latitude)) / Math.PI

    val lngDiff = ne.longitude - sw.longitude
    val lngFraction = if (lngDiff < 0) {
        (lngDiff + 360) / 360
    } else {
        (lngDiff / 360)
    }

    val latZoom = zoom(mapDim.height, WORLD_DIM.height, latFraction)
    val lngZoom = zoom(mapDim.width, WORLD_DIM.width, lngFraction)

    return minOf(latZoom, lngZoom, ZOOM_MAX)
}

// Change the soft input mode between adjustPan and adjustResize
// Changes the way content is handled when soft keyboard is open
fun adjustContentWithKB(context: Context, isPan: Boolean = false) {
    if (isPan) {
        (context as Activity).window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    } else {
        (context as Activity).window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}

fun timeStampToTimeString(timestamp: Timestamp): String? {
    val localDateTime =
        timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return localDateTime.format(formatter)
}