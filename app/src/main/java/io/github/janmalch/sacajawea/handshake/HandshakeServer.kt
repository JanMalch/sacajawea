package io.github.janmalch.sacajawea.handshake

import android.os.AsyncTask
import android.util.Log
import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class HandshakeServer (private val onHandshakeFinished: (HandshakeResult) -> Unit) : AsyncTask<Unit, Unit, Unit>(), Closeable {

    private val server: ServerSocket = ServerSocket(0)


    private var acceptHandler: (Socket) -> Int? = { null }

    var running: Boolean = false
        private set

    val port: Int
        get() = server.localPort

    fun onAccept(handler: (Socket) -> Int?) {
        this.acceptHandler = handler
    }

    override fun onPreExecute() {
        super.onPreExecute()
        if (port != -1) {
            running = true
        } else {
            Log.e("HandshakeServer", "Unable to start handshake server, got no port")
        }
    }

    override fun doInBackground(vararg params: Unit?) {
        Log.i("HandshakeServer", "HandshakeServer is running: $running")
        while (running) {
            val client = server.accept()
            Log.i("HandshakeServer", "Accepted ${client.inetAddress}")
            // val audioStream = audioGroupServer.addClient(client.inetAddress)
            val result = acceptHandler(client)
            Log.i("HandshakeServer", "Accepted ${client.inetAddress} for port $result")

            result?.also { data ->
                onHandshakeFinished(HandshakeResult(client.inetAddress, result))
                client.getOutputStream().write(data.toString(10).toByteArray())
                client.getOutputStream().flush()
            }
            client.close()
        }
        Log.i("HandshakeServer", "HandshakeServer has stopped.")
    }

    override fun onCancelled(result: Unit?) {
        super.onCancelled(result)
        running = false
    }

    override fun onPostExecute(result: Unit?) {
        super.onPostExecute(result)
        close()
    }

    override fun close() {
        server.close()
    }
}

data class HandshakeResult(val address: InetAddress, val port: Int)