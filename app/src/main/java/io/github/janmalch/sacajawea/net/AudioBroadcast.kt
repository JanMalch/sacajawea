package io.github.janmalch.sacajawea.net

import io.github.janmalch.sacajawea.media.AudioPayload
import io.github.janmalch.sacajawea.observable.IObservable
import java.io.Closeable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.CountDownLatch

class AudioBroadcast(
    private val port: Int,
    private val audioStream: IObservable<AudioPayload>
) : Runnable, Closeable {

    private val socket = DatagramSocket()

    override fun run() {
        val shutdown = CountDownLatch(1)
        audioStream.subscribe {
            val packet = DatagramPacket(it.buffer, it.length, InetAddress.getByName("192.168.49.255"), port)
            socket.takeIf { s -> !s.isClosed }?.send(packet)
        }
        audioStream.completed { shutdown.countDown() }
        shutdown.await()
        close()
    }

    override fun close() {
        socket.disconnect()
        socket.close()
    }
}