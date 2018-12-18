package io.github.janmalch.sacajawea.observable

import android.os.AsyncTask
import android.util.Log
import io.github.janmalch.sacajawea.listening.Listener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class ServerSubject : Runnable {

    private val server = ServerSocket(0)

    private val overallRatingSubject = Subject<Rating>()
    val overallRatings = overallRatingSubject.toObservable()

    private val volumeRatingSubject = Subject<Rating>()
    val volumeRatings = volumeRatingSubject.toObservable()

    private val speedRatingSubject = Subject<Rating>()
    val speedRatings = speedRatingSubject.toObservable()

    private val handshakeSubject = Subject<HandshakeResult>()
    val handshakes = handshakeSubject.toObservable()

    private val goodbyeSubject = Subject<String>()
    val goodbyes = goodbyeSubject.toObservable()

    val port: Int
        get() = server.localPort

    @Volatile
    var running = false
        private set

    override fun run() {
        running = true
        while (running) {
            try {
                val socket = server.accept()
                AsyncTask.execute {
                    val input = BufferedReader(InputStreamReader(socket.getInputStream()))

                    input.forEachLine {action ->
                        when {
                            action.startsWith("NAME: ") -> {
                                val name = action.removePrefix("NAME: ").trim()
                                val listener = Listener(name, socket.inetAddress.hostAddress)
                                handshakeSubject.next(HandshakeResult(listener, socket))
                            }
                            action.startsWith("RATING: ") -> {
                                val received = action.removePrefix("RATING: ").trim()
                                val rating = Rating(received.toInt(), socket.inetAddress.hostAddress)
                                overallRatingSubject.next(rating)
                            }
                            action == "GOODBYE" -> {
                                println("GoodBye from " + socket.inetAddress.hostAddress)
                                goodbyeSubject.next(socket.inetAddress.hostAddress)
                            }
                            action.startsWith("SPEED: ") -> {
                                val received = action.removePrefix("SPEED: ").trim()
                                val rating = Rating(received.toInt(), socket.inetAddress.hostAddress)
                                speedRatingSubject.next(rating)
                            }
                            action.startsWith("VOLUME: ") -> {
                                val received = action.removePrefix("VOLUME: ").trim()
                                val rating = Rating(received.toInt(), socket.inetAddress.hostAddress)
                                volumeRatingSubject.next(rating)
                            }
                        }
                    }
                }

            } catch (e: SocketException) {
                Log.d("ServerSubject", "SocketException occurred", e)
                running = false
            } catch (e: Exception) {
                Log.e("ServerSubject", "Unknown error while accepting new clients", e)
                running = false
            }
        }
    }

    fun complete() {
        overallRatingSubject.complete()
        goodbyeSubject.complete()
        handshakeSubject.complete()
        running = false
        server.close()
    }
}

data class HandshakeResult(val listener: Listener, val client: Socket)
data class Rating(val rating: Int, val ip: String)