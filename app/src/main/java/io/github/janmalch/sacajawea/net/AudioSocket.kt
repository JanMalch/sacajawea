package io.github.janmalch.sacajawea.net

import android.util.Log
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

    private val socket = DatagramSocket()
    private val shutdown = CountDownLatch(1)

    override fun run() {
        audioStream.subscribe {
            Log.i("AudioSocket", address.hostAddress)
            val packet = DatagramPacket(it.buffer, it.length, InetAddress.getByName("192.168.49.255"), port)
            socket.takeIf { s -> !s.isClosed }?.send(packet)
        }
        audioStream.completed { shutdown.countDown() }
        shutdown.await()
        close()
    }

    fun shutdown() {
        shutdown.countDown()
    }

    override fun close() {
        Log.d("AudioSocket", "Closing $address")
        socket.disconnect()
        socket.close()
    }
}