package io.github.janmalch.sacajawea.listening

import android.app.AlertDialog
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.widget.RatingBar
import io.github.janmalch.sacajawea.*
import io.github.janmalch.sacajawea.listening.messages.*
import io.github.janmalch.sacajawea.net.AudioClient
import io.github.janmalch.sacajawea.wifi.WiFiActivity
import kotlinx.android.synthetic.main.activity_listening.*
import java.util.concurrent.Executors


class ListeningActivity : WiFiActivity() {

    private val currentName: String by lazy {
        getPreferenceString(R.string.pref_name)
    }

    private lateinit var translator: Translator
    // private var client: UdpAudioClient? = null
    private var client: AudioClient? = null
    private val threads = Executors.newFixedThreadPool(1)
    private lateinit var messenger: Messenger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listening)
        stop_listening.setOnClickListener { stopListening() }

        listening_feedback_speed.onSeekBarRelease {
            it?.progress?.also { progress ->
                messenger.send(SpeedRatingMessage(progress - 2))
            }
        }
        listening_feedback_volume.onSeekBarRelease {
            it?.progress?.also { progress ->
                messenger.send(VolumeRatingMessage(progress - 2))
            }
        }
        listening_feedback_overall.onSeekBarRelease {
            it?.progress?.also { progress ->
                messenger.send(OverallRatingMessage(progress))
            }
        }

        updateProgress(0)
        translator = intent.getParcelableExtra("translator")
        connect(translator.device.p2pConfig)
    }

    private fun updateProgress(step: Int) {
        runOnUiThread {
            listening_setup_progress.progress = step stepOf 6

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
    }

    private fun showRatingDialog() {
        var dialog: AlertDialog? = null

        dialog = AlertDialog.Builder(this)
            .setTitle("Thanks for listening!")
            .setView(layoutInflater.inflate(R.layout.dialog_stop_listening, null))
            .setPositiveButton("Rate & Disconnect") { _, _ ->
                val rating = dialog?.findViewById<RatingBar>(R.id.listening_rating)?.rating ?: 0F
                messenger.send(OverallRatingMessage(rating.toInt()), GoodbyeMessage())
                // threads.submit(SendRating(translator.hostAddress ?: "", translator.port, rating))
                client?.close()
                threads.shutdown()
                finish()
                println("Rate & Disconnect $rating")
            }
            .setNegativeButton("Disconnect") { _, _ ->
                client?.close()
                threads.shutdown()
                finish()
                println("Disconnect")
            }
            .create()

        dialog.show()
    }

    private fun stopListening() {
        // showRatingDialog()
        // client?.stop()

        client?.close()
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

    private var wasConnected = false

    private fun notifyOfEnd() {
        AlertDialog.Builder(this)
            .setTitle("Thanks for listening!")
            .setMessage("${translator.name} has closed this session. You will now return to the translator overview.")
            .setPositiveButton("Leave") { _, _ -> stopListening() }
            .setCancelable(false)
            .setOnDismissListener { }
            .create()
            .show()
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {

        info?.takeIf { wasConnected && !it.groupFormed }?.also { notifyOfEnd() }

        Log.i(TAG, "onConnectionInfoAvailable $info")
        info?.also {
            if (info.groupFormed && info.groupOwnerAddress?.hostAddress != null) {
                updateProgress(3)

                info.groupOwnerAddress?.also { host ->
                    client = AudioClient(host.hostAddress, translator.port, currentName)
                    translator.hostAddress = host.hostAddress
                    messenger = Messenger(translator.hostAddress ?: "", translator.port)
                    client?.setup?.subscribe { step -> updateProgress(3 + step) }
                    threads.submit(client)
                    wasConnected = true
                    /*client = UdpAudioClient(host.hostAddress, translator.port) { step ->
                        updateProgress(3 + step)
                    }
                    client!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)*/
                }
            }
        }
    }

    override fun onBackPressed() {
        // super.onBackPressed()
        stopListening()
    }

}
