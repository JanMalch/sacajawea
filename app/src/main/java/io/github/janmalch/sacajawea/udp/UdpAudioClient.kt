package io.github.janmalch.sacajawea.udp

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.AsyncTask
import android.util.Log
import io.github.janmalch.sacajawea.config.AudioConfig
import java.io.IOException
import java.net.*


class UdpAudioClient (private val host: String, private val port: Int, private val progress: (Int) -> Unit) : AsyncTask<Unit, Int, Unit>() {

    private val LOG_TAG = "AudioCall"
    // private val SAMPLE_INTERVAL = 20 // Milliseconds
    // private val SAMPLE_SIZE = 2 // Bytes
    // private val BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2 //Bytes
    var running = false
        private set
    private val socket = DatagramSocket(port)
    // private var track: AudioTrack? = null
    private val track = AudioTrack(
        AudioManager.STREAM_MUSIC, AudioConfig.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
        AudioConfig.AUDIO_FORMAT, AudioConfig.BUF_SIZE, AudioTrack.MODE_STREAM
    )

    fun stop() {
        running = false
    }

    override fun onPreExecute() {
        super.onPreExecute()
        running = true
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        progress(values[0]!!) // TODO
    }

    override fun doInBackground(vararg params: Unit?) {
        val socketx = Socket()
        publishProgress(1)

        socketx.use {
            it.bind(null)
            it.connect(InetSocketAddress(host, port), 500)
            it.getOutputStream().write("Jan\n".toByteArray())
            it.getOutputStream().flush()
        }

        publishProgress(2)
        Log.i(LOG_TAG, "Receive thread started.")
        /*val track = AudioTrack(
            AudioManager.STREAM_MUSIC, AudioConfig.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
            AudioConfig.AUDIO_FORMAT, AudioConfig.BUF_SIZE, AudioTrack.MODE_STREAM
        )*/
        track.play()
        try {
            // Define a socket to receive the audio
            // var socket = DatagramSocket(port)
            val buf = ByteArray(AudioConfig.BUF_SIZE)

            /*val name = "Jan".toByteArray()
            val namePacket = DatagramPacket(name, name.size, InetAddress.getByName(host), port)
            socket.send(namePacket)*/

            publishProgress(3)
            while (running) {
                // Play back the audio received from packets
                val packet = DatagramPacket(buf, AudioConfig.BUF_SIZE)
                socket.receive(packet)
                Log.i(LOG_TAG, "Packet received: " + packet.length)
                track.write(packet.data, 0, AudioConfig.BUF_SIZE)
            }
            // Stop playing back and release resources
            running = false
            return
        } catch (e: SocketException) {

            Log.e(LOG_TAG, "SocketException: " + e.toString())
            running = false
        } catch (e: IOException) {

            Log.e(LOG_TAG, "IOException: " + e.toString())
            running = false
        }

    }

    private fun cleanUp() {
        socket.disconnect()
        socket.close()

        track.stop()
        track.flush()
        track.release()
    }

    override fun onCancelled(result: Unit?) {
        super.onCancelled(result)
        cleanUp()
    }


    override fun onPostExecute(result: Unit?) {
        super.onPostExecute(result)
        cleanUp()
    }

}