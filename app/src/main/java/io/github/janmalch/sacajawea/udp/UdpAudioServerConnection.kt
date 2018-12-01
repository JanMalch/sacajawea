package io.github.janmalch.sacajawea.udp


import android.os.AsyncTask
import android.util.Log
import io.github.janmalch.sacajawea.config.AudioConfig
import io.github.janmalch.sacajawea.media.AudioPayload
import io.github.janmalch.sacajawea.observable.Observable
import java.io.IOException
import java.net.*


class UdpAudioServerConnection(private val address: InetAddress, private val port: Int, private val audioStream: Observable<AudioPayload>) : AsyncTask<Unit, Unit, Unit>() {

    private val LOG_TAG = "AudioCall"
    private var streaming: Boolean = true


    override fun doInBackground(vararg params: Unit?) {
        try {
            // Create a socket and start recording
            Log.i(LOG_TAG, "Packet destination: " + address.toString())
            val socket = DatagramSocket()

            audioStream.subscribe {
                val packet = DatagramPacket(it.buffer, it.length, address, port)
                socket.send(packet)
                Thread.sleep(AudioConfig.SAMPLE_INTERVAL.toLong(), 0)
            }

            audioStream.completed { streaming = false }

            while (streaming) {
            }

            socket.disconnect()
            socket.close()
            streaming = false
        } catch(e: InterruptedException) {

            Log.e(LOG_TAG, "InterruptedException: " + e.toString());
            streaming = false;
        }
        catch(e: SocketException) {

            Log.e(LOG_TAG, "SocketException: " + e.toString());
            streaming = false;
        }
        catch(e: UnknownHostException) {

            Log.e(LOG_TAG, "UnknownHostException: " + e.toString());
            streaming = false;
        }
        catch(e: IOException) {

            Log.e(LOG_TAG, "IOException: " + e.toString());
            streaming = false;
        }

    }
}