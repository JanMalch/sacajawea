package io.github.janmalch.sacajawea.media

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import io.github.janmalch.sacajawea.config.AudioConfig
import io.github.janmalch.sacajawea.observable.IObservable
import io.github.janmalch.sacajawea.observable.Subject
import java.io.Closeable
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.fixedRateTimer

class AudioStream(private val shutdown: CountDownLatch) : IObservable<AudioPayload>, Runnable, Closeable {

    init {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
    }

    private val buf = ByteArray(AudioConfig.BUF_SIZE)

    private var timer: Timer? = null
    private val subject = Subject<Int>()

    private val audioRecorder = AudioRecord(
        MediaRecorder.AudioSource.MIC, AudioConfig.SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO, AudioConfig.AUDIO_FORMAT,
        AudioRecord.getMinBufferSize(
            AudioConfig.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioConfig.AUDIO_FORMAT
        ) * 10
    )

    override fun run() {
        audioRecorder.startRecording()
        if (audioRecorder.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("Audio Recorder not initialized.")
        }

        timer = fixedRateTimer("AudioStream Timer", period = AudioConfig.SAMPLE_INTERVAL.toLong()) {
            subject.next(audioRecorder.read(buf, 0, AudioConfig.BUF_SIZE))
        }

        shutdown.await()
        close()
    }

    override fun close() {
        subject.complete()
        timer?.cancel()
        audioRecorder.stop()
        audioRecorder.release()
    }

    override fun subscribe(observer: (AudioPayload) -> Unit) {
        subject.subscribe { observer(AudioPayload(buffer = buf, length = it)) }
    }

    override fun completed(handler: () -> Unit) {
        subject.completed(handler)
    }
}

data class AudioPayload(val buffer: ByteArray, val length: Int)