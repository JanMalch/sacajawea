package io.github.janmalch.sacajawea.udp

import android.os.AsyncTask
import android.util.Log
import io.github.janmalch.sacajawea.listeners.Listener
import io.github.janmalch.sacajawea.media.AudioRecordSource
import io.github.janmalch.sacajawea.observable.Observable
import java.net.ServerSocket
import java.net.Socket

class UdpAudioServer : AsyncTask<Unit, Listener, Unit>() {

    private val server: ServerSocket = ServerSocket(0)

    private val audioSource = AudioRecordSource()
    private val connections = mutableListOf<UdpAudioServerConnection>()

    val newClients = Observable<Listener>()

    val port: Int
        get() = server.localPort

    var running: Boolean = false

    private fun cleanUp() {
        newClients.complete()
        audioSource.stop()
        server.close()
    }

    override fun onPostExecute(result: Unit?) {
        super.onPostExecute(result)
        cleanUp()
    }

    override fun onProgressUpdate(vararg values: Listener) {
        super.onProgressUpdate(*values)
        val it = values[0]
        newClients.next(it)
    }

    override fun doInBackground(vararg params: Unit?) {
        while (running) {
            val client = server.accept()

            // TODO: Clean-up, Handshake to exchange names
            publishProgress(Listener("Anonymous Listener", client.inetAddress.hostAddress))
            val task = UdpAudioServerConnection(client.inetAddress, port, audioSource.stream) // pass audioRecorder object
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            connections.add(task)
        }
    }

    override fun onCancelled(result: Unit?) {
        super.onCancelled(result)
        cleanUp()
    }

    override fun onPreExecute() {
        super.onPreExecute()
        audioSource.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        running = true
    }
}