package io.github.janmalch.sacajawea.media

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.AsyncTask
import io.github.janmalch.sacajawea.config.AudioConfig
import io.github.janmalch.sacajawea.observable.Observable

class AudioRecordSource : AsyncTask<Unit, Unit, Unit>() {

    val stream = Observable<AudioPayload>()
    var running: Boolean = false
        private set

    private val audioRecorder = AudioRecord(
        MediaRecorder.AudioSource.MIC, AudioConfig.SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO, AudioConfig.AUDIO_FORMAT,
        AudioRecord.getMinBufferSize(
            AudioConfig.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 10
    )

    init {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
    }

    override fun onPreExecute() {
        super.onPreExecute()
        if (audioRecorder.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("Audio Recorder not initialized.")
        }
        running = true
    }

    override fun doInBackground(vararg params: Unit?) {
        var bytesRead: Int
        var bytesSent = 0
        val buf = ByteArray(AudioConfig.BUF_SIZE)

        audioRecorder.startRecording()
        while (running) {
            // Capture audio from the mic and transmit it
            bytesRead = audioRecorder.read(buf, 0, AudioConfig.BUF_SIZE)
            stream.next(AudioPayload(buffer = buf, length = bytesRead))
            bytesSent += bytesRead
            Thread.sleep(AudioConfig.SAMPLE_INTERVAL.toLong(), 0)
        }
    }

    fun stop() {
        running = false
    }

    fun cleanUp() {
        stream.complete()
        audioRecorder.stop()
        audioRecorder.release()
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

data class AudioPayload(val buffer: ByteArray, val length: Int)