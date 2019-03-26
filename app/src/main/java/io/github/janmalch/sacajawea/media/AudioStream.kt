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

// TODO: IObservable by delegation?

class AudioStream
private constructor(
    private val shutdown: CountDownLatch,
    private val subject: Subject<AudioPayload>
) :
    IObservable<AudioPayload> by subject,
    Runnable,
    Closeable {

    constructor(shutdown: CountDownLatch): this(shutdown, Subject())


    init {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
    }

    private val buf = ByteArray(AudioConfig.BUF_SIZE)

    private var timer: Timer? = null

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

        timer = fixedRateTimer("AudioStream Timer",
            period = AudioConfig.SAMPLE_INTERVAL.toLong()
        ) {
            val length = audioRecorder.read(buf, 0, AudioConfig.BUF_SIZE)
            subject.next(AudioPayload(buffer = buf, length = length))
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

}

data class AudioPayload(val buffer: ByteArray, val length: Int)

