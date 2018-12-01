package io.github.janmalch.sacajawea

import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import io.github.janmalch.sacajawea.service.Translator
import io.github.janmalch.sacajawea.udp.UdpAudioClient
import kotlinx.android.synthetic.main.activity_listening.*


class ListeningActivity : WiFiActivity() {

    private lateinit var translator: Translator
    private var client: UdpAudioClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listening)
        stop_listening.setOnClickListener { stopListening() }

        translator = AppState.activeTranslator!! // TODO: intent.getParcelableExtra("translator") as Translator
        tv_listening_status.text = getString(
            R.string.listening_status,
            translator.name,
            translator.language
        )
        connect(translator.device.p2pConfig)
    }

    private fun stopListening() {
        client?.stop()
    }

    private fun connect(config: WifiP2pConfig) {
        listening_setup_progress.setProgress(1 stepOf 6, true)
        mManager.connect(mChannel, config, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                // WiFiDirectBroadcastReceiver notifies us. Ignore for now.
                Log.i(TAG, "connect::onSuccess")
                listening_setup_progress.setProgress(2 stepOf 6, true)
            }

            override fun onFailure(reason: Int) {
                toast("Connect failed. Retry.")
                Log.e(TAG, "connect::onFailure $reason")
            }
        })
    }

    // ------------------------------ WiFi Callbacks ------------------------------

    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {

        info?.also {
            if (info.groupFormed && info.groupOwnerAddress?.hostAddress !== null) {
                tv_listening_status.text = getString(
                    R.string.listening_status,
                    translator.name,
                    translator.language
                )
                listening_setup_progress.setProgress(3 stepOf 6, true)

                info.groupOwnerAddress?.also { host ->
                    client = UdpAudioClient(host.hostAddress, translator.port) { step ->
                        listening_setup_progress.setProgress((3 + step) stepOf 6, true)
                    }
                    client!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }
            }
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        stopListening()
        AppState.activeTranslator = null
        finish()
    }

}
