package io.github.janmalch.sacajawea.media

import android.media.MediaRecorder
import android.os.AsyncTask
import io.github.janmalch.sacajawea.Seconds
import java.util.*
import kotlin.concurrent.fixedRateTimer

class VolumeStream(private val progress: (String) -> Unit) : AsyncTask<Unit, String, Unit>() {
    private var mEMA = 0.0
    private val EMA_FILTER = 0.6
    private val mRecorder = MediaRecorder().apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        setOutputFile("/dev/null")
    }
    private var timer: Timer? = null

    var running: Boolean = false
        private set

    fun stop() {
        running = false
    }

    override fun onProgressUpdate(vararg values: String?) {
        super.onProgressUpdate(*values)
        progress(values[0] ?: "")
    }

    override fun onPreExecute() {
        super.onPreExecute()
        mRecorder.prepare()
        running = true
    }

    override fun doInBackground(vararg params: Unit?) {
        try {
            mRecorder.start()
        } catch (e: Exception) {
            cleanUp()
            return
        }
        timer = fixedRateTimer("VolumeStream Timer", period = 1.Seconds) {
            publishProgress("${soundDb()} dB")
        }
        while (running) {
        }
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

    private fun cleanUp() {
        try {
            mRecorder.stop()
        } catch (e: java.lang.Exception) {}
        mRecorder.release()
        timer?.cancel()
    }

    override fun onPostExecute(result: Unit?) {
        super.onPostExecute(result)
        cleanUp()
    }

    override fun onCancelled(result: Unit?) {
        super.onCancelled(result)
        cleanUp()
    }
}