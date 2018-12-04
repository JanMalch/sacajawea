package io.github.janmalch.sacajawea.observable

import android.os.AsyncTask
import android.util.Log
import io.github.janmalch.sacajawea.listening.Listener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class ServerSubject : Subject<HandshakeResult>(), Runnable {

    private val server = ServerSocket(0)

    val port: Int
        get() = server.localPort

    @Volatile
    var running = false
        private set

    override fun run() {
        running = true
        while (running) {
            try {
                Log.d("ServerSubject", "Accepting next client ...")
                val socket = server.accept()
                AsyncTask.execute {
                    val input = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val name = input.readLine()
                    val listener =
                        Listener(name.trim(), socket.inetAddress.hostAddress)
                    next(HandshakeResult(listener, socket))
                    // next(socket)
                }

            } catch (e: SocketException) {
                running = false
            } catch (e: Exception) {
                Log.e("ServerSubject", "Unknown error while accepting new clients: $e")
                running = false
            }
        }
    }

    override fun complete() {
        super.complete()
        running = false
        server.close()
    }
}

data class HandshakeResult(val listener: Listener, val client: Socket)