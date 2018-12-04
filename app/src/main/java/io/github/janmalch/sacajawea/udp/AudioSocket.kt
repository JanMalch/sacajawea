package io.github.janmalch.sacajawea.udp

import io.github.janmalch.sacajawea.media.AudioPayload
import io.github.janmalch.sacajawea.observable.IObservable
import java.io.Closeable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.CountDownLatch

class AudioSocket(
    private val address: InetAddress,
    private val port: Int,
    private val audioStream: IObservable<AudioPayload>
) : Runnable, Closeable {

    private var socket: DatagramSocket? = null

    override fun run() {
        val shutdown = CountDownLatch(1)
        socket = DatagramSocket()

        audioStream.subscribe {
            socket?.send(DatagramPacket(it.buffer, it.length, address, port))
            // Thread.sleep(AudioConfig.SAMPLE_INTERVAL.toLong(), 0)
        }

        audioStream.completed { shutdown.countDown() }
        shutdown.await()
        close()
    }

    override fun close() {
        socket?.also {
            it.disconnect()
            it.close()
        }
    }
}