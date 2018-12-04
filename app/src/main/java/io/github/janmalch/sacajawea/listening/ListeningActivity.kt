package io.github.janmalch.sacajawea.listening

import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import io.github.janmalch.sacajawea.*
import io.github.janmalch.sacajawea.udp.UdpAudioClient
import io.github.janmalch.sacajawea.wifi.WiFiActivity
import kotlinx.android.synthetic.main.activity_listening.*


class ListeningActivity : WiFiActivity() {

    private lateinit var translator: Translator
    private var client: UdpAudioClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listening)
        stop_listening.setOnClickListener { stopListening() }

        updateProgress(0)
        translator = AppState.activeTranslator!! // TODO: intent.getParcelableExtra("translator") as Translator
        connect(translator.device.p2pConfig)
    }

    private fun updateProgress(step: Int) {
        listening_setup_progress.setProgress(step stepOf 6, true)
        if (step < 6) {
            tv_listening_status.text = getString(R.string.setting_up, step)
        } else {
            tv_listening_status.text = getString(
                R.string.listening_status,
                translator.name,
                translator.language
            )
        }
    }

    private fun stopListening() {
        client?.stop()
        finish()
    }

    private fun connect(config: WifiP2pConfig) {
        updateProgress(1)
        mManager.connect(mChannel, config, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                // WiFiDirectBroadcastReceiver notifies us. Ignore for now.
                Log.i(TAG, "connect::onSuccess")
                updateProgress(2)
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
                updateProgress(3)

                info.groupOwnerAddress?.also { host ->
                    client = UdpAudioClient(host.hostAddress, translator.port) { step ->
                        updateProgress(3 + step)
                    }
                    client!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }
            }
        }
    }

    override fun onBackPressed() {
        // super.onBackPressed()
        AppState.activeTranslator = null
        stopListening()
    }

}
