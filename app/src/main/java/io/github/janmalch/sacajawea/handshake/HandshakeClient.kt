package io.github.janmalch.sacajawea.handshake

import android.os.AsyncTask
import android.util.Log
import io.github.janmalch.sacajawea.convertToString
import java.net.InetSocketAddress
import java.net.Socket

class HandshakeClient(private val onPortReceived: (String) -> Unit) : AsyncTask<InetSocketAddress, Unit, String>() {

    override fun doInBackground(vararg params: InetSocketAddress?): String {
        val socket = Socket()

        return socket.use {
            it.bind(null)
            it.connect(params[0], 500)
            Log.i("HandshakeClient", params[0]!!.address.toString())
            it.getInputStream().convertToString()
        }
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        result?.also { onPortReceived(result) }
    }
}