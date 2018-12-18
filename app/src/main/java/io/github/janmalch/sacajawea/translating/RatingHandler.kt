package io.github.janmalch.sacajawea.translating

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.ProgressBar
import io.github.janmalch.sacajawea.R
import io.github.janmalch.sacajawea.observable.Rating
import io.github.janmalch.sacajawea.toast
import kotlin.math.max

abstract class RatingHandler(
    private val activity: Activity,
    private val progressBar: ProgressBar,
    private val minProgress: Int = 10
) {

    private val ratings = mutableMapOf<String, Rating>()

    protected val averageRating: Double
        get() = ratings.values
            .map { r -> r.rating }
            .average()

    fun update() {
        activity.runOnUiThread {
            val color = Color.HSVToColor(calcHSVColor())
            progressBar.progressTintList = ColorStateList.valueOf(color)
            progressBar.progress = max(calcProgress(), minProgress)
        }
    }

    abstract fun calcProgress(): Int

    protected open fun calcHue(): Float {
        return 200F
    }

    protected open fun calcHSVColor(): FloatArray {
        return floatArrayOf(calcHue(), 0.77F, 0.51F)
    }

    fun addRating(rating: Rating) {
        ratings[rating.ip] = rating
        update()
    }

}