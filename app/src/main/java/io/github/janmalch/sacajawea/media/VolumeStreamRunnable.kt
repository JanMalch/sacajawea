package io.github.janmalch.sacajawea.media

import android.media.MediaRecorder
import android.util.Log
import io.github.janmalch.sacajawea.Seconds
import io.github.janmalch.sacajawea.observable.Subject
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.fixedRateTimer

class VolumeStreamRunnable(private val shutdown: CountDownLatch) : Runnable {
    private var mEMA = 0.0
    private val EMA_FILTER = 0.6
    private val mRecorder = MediaRecorder().apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        setOutputFile("/dev/null")
    }
    private var timer: Timer? = null
    private var recorderRunning = false

    private val subject = Subject<String>()
    val progress = subject.toObservable()

    override fun run() {
        try {
            mRecorder.prepare()
            mRecorder.start()
        } catch (e: Exception) {
            Log.e("VolumeStream", "Error in start up", e)
            close()
            return
        }
        recorderRunning = true
        timer = fixedRateTimer("VolumeStream Timer", period = 1.Seconds) {
            subject.next("${soundDb()} dB")
        }
        shutdown.await()
        close()
    }

    fun close() {
        timer?.cancel()
        if (recorderRunning) {
            mRecorder.stop()
            recorderRunning = false
        }
        mRecorder.release()
    }

    private fun soundDb(ampl: Double = getAmplitude()): Double {
        return 20 * Math.log10(getAmplitudeEMA() / ampl)
    }

    private fun getAmplitude(): Double {
        return mRecorder.maxAmplitude.toDouble()
    }

    private fun getAmplitudeEMA(): Double {
        val amp = getAmplitude()
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA
        return mEMA
    }

}