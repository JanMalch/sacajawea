package io.github.janmalch.sacajawea.net

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import io.github.janmalch.sacajawea.config.AudioConfig
import io.github.janmalch.sacajawea.listening.messages.HandshakeMessage
import io.github.janmalch.sacajawea.listening.messages.sendMessages
import io.github.janmalch.sacajawea.observable.Subject
import java.io.Closeable
import java.net.*

class AudioClient(private val host: String, private val port: Int, private val name: String) : Runnable, Closeable {

    private val setupSubject = Subject<Int>()
    val setup = setupSubject.toObservable()

    @Volatile
    var running = false
        private set

    private val socket = DatagramSocket(port)
    private val track = AudioTrack(
        AudioManager.STREAM_MUSIC, AudioConfig.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
        AudioConfig.AUDIO_FORMAT, AudioConfig.BUF_SIZE, AudioTrack.MODE_STREAM
    )

    override fun run() {
        setupSubject.next(INITIALIZED)
        sendMessages(host, port, HandshakeMessage(name))
        setupSubject.next(HANDSHAKE_COMPLETE)

        track.play()
        val buf = ByteArray(AudioConfig.BUF_SIZE)
        setupSubject.next(RECEIVING_PACKETS)

        running = true
        while (running) {
            val packet = DatagramPacket(buf, AudioConfig.BUF_SIZE)
            try {
                socket.receive(packet)
            } catch (e: SocketException) {
                Log.e("AudioClient", "Exception while receiving audio packets", e)
                cleanUp()
                running = false
                return
            }
            track.write(packet.data, 0, AudioConfig.BUF_SIZE)
        }
        cleanUp()
    }

    override fun close() {
        cleanUp()
    }

    private fun cleanUp() {
        socket.takeIf { it.isConnected }?.disconnect()
        socket.takeIf { it.isClosed }?.close()

        track.stop()
        track.flush()
        track.release()
    }

    companion object {
        val INITIALIZED = 1
        val HANDSHAKE_COMPLETE = 2
        val RECEIVING_PACKETS = 3
    }
}