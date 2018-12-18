package io.github.janmalch.sacajawea.translating

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import io.github.janmalch.sacajawea.R
import io.github.janmalch.sacajawea.TAG
import io.github.janmalch.sacajawea.getPreferenceString
import io.github.janmalch.sacajawea.listening.Listener
import io.github.janmalch.sacajawea.media.AudioStream
import io.github.janmalch.sacajawea.observable.ServerSubject
import io.github.janmalch.sacajawea.net.AudioBroadcast
import io.github.janmalch.sacajawea.net.AudioSocket
import io.github.janmalch.sacajawea.wifi.WiFiActivity
import kotlinx.android.synthetic.main.activity_translating.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.roundToInt

class TranslatingActivity : WiFiActivity() {

    private val currentLanguage: String by lazy {
        getPreferenceString(R.string.pref_translate_language)
    }
    private val currentName: String by lazy {
        getPreferenceString(R.string.pref_name)
    }

    // private val volume = VolumeStream { tv_translating_sound.text = it }

    private val listeners = mutableMapOf<String, Listener>() // Listeners()
    private val REQUEST_MICROPHONE = 100

    private val threads = Executors.newCachedThreadPool()
    private val shutdown = CountDownLatch(1)
    private val sockets = mutableMapOf<String, AudioSocket>()

    // private val volume = VolumeStreamRunnable(shutdown)

    private lateinit var audioStream: AudioStream
    private lateinit var server: ServerSubject
    private lateinit var service: TranslateService

    private lateinit var overallRatingHandler: RatingHandler
    private lateinit var volumeRatingHandler: RatingHandler
    private lateinit var speedRatingHandler: RatingHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translating)
        stop_translating.setOnClickListener { stopTranslating() }

        overallRatingHandler = object : RatingHandler(this, translating_feedback_overall) {
            override fun calcProgress(): Int = (averageRating * 25).roundToInt()
            override fun calcHue(): Float = (averageRating * 25).toFloat()
        }

        volumeRatingHandler = object : RatingHandler(this, translating_feedback_volume) {
            override fun calcProgress(): Int = (averageRating * 25).roundToInt() + 50
            override fun calcHue(): Float = abs(averageRating.toFloat()) * -50 + 100
        }

        speedRatingHandler = object : RatingHandler(this, translating_feedback_speed) {
            override fun calcProgress(): Int = (averageRating * 25).roundToInt() + 50
            override fun calcHue(): Float = abs(averageRating.toFloat()) * -50 + 100
        }

        audioStream = AudioStream(shutdown)
        threads.submit(audioStream)

        server = ServerSubject()
        server.overallRatings.subscribe(overallRatingHandler::addRating)
        server.speedRatings.subscribe(speedRatingHandler::addRating)
        server.volumeRatings.subscribe(volumeRatingHandler::addRating)

        server.goodbyes.subscribe {
            sockets[it]?.shutdown()
            listeners.remove(it)
            requestGroupInfo()
        }

        server.handshakes.subscribe {
            listeners[it.listener.ip] = it.listener
            requestGroupInfo()
        }
        threads.submit(AudioBroadcast(server.port, audioStream))
        threads.submit(server)

        service = TranslateService(
            currentName,
            currentLanguage,
            server.port,
            mManager,
            mChannel
        )

        service.registerService(object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                updateStatus(-1)
            }

            override fun onFailure(reason: Int) {
                updateStatus(reason)
            }
        })

        checkPermissions()
        setupRecording()
    }

    private fun setupRecording() {
        // TODO: move code here (due to permissions, ...)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_MICROPHONE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    setupRecording()
                }
                return
            }
            else -> {
            }
        }
    }

    private fun createGroup() {
        Log.i(TAG, "createGroup")

        mManager.createGroup(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Device is ready to accept incoming connections from peers.
                Log.i(TAG, "createGroup::onSuccess ")
                requestGroupInfo()
            }

            override fun onFailure(reason: Int) {
                // toast("P2P group creation failed. Retry.")
                Log.e(TAG, "createGroup::onFailure $reason")
                updateStatus(reason)
            }
        })
    }

    private fun requestGroupInfo() {
        mManager.requestGroupInfo(mChannel) { group ->
            group?.also {
                Log.i(TAG, "onGroupInfoAvailable :: group" /* = $it*/)
                Log.i(TAG, "  |__ :: isGroupOwner = ${it.isGroupOwner}")
                Log.i(TAG, "  |__ :: Clients = ${it.clientList.size}")
                updateStatus(-1, it.clientList.size, listeners.size)
            }
        }
    }

    private fun stopTranslating() {
        shutdown.countDown()
        server.complete()
        // audioStream.close()
        // udpAudioServer.cancel(true)
        service.unregisterService()
        mManager.removeGroup(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "removeGroup::onSuccess")
            }

            override fun onFailure(reason: Int) {
                Log.i(TAG, "removeGroup::onFailure")
            }
        })
        threads.shutdown()
        finish()
    }

    override fun onBackPressed() {
        stopTranslating()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_MICROPHONE
            )
        }
    }


    // ------------------------------ WiFi Callbacks ------------------------------

    /*override fun onWifiP2PChanged(enabled: Boolean) {
        Log.i(TAG, "onWifiP2PChanged :: Enabled = $enabled")
    }

    override fun onDeviceWifiChanged() {
        Log.i(TAG, "onDeviceWifiChanged")
    }
*/
    override fun onPeersAvailable(peers: WifiP2pDeviceList?) {
        Log.i(TAG, "onPeersAvailable :: ${peers?.deviceList?.size ?: 0}")
        requestGroupInfo()
    }

    /*override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
        Log.i(TAG, "onConnectionInfoAvailable :: $info")
        info?.also {
            Log.i(TAG, "|__ :: ${info.groupOwnerAddress?.hostAddress}, ${info.groupFormed && info.isGroupOwner}")

            *//*if (info.groupOwnerAddress != null && audioGroupServer == null) {
                audioGroupServer = AudioGroupServer(info.groupOwnerAddress, this)
                handshakeServer.onAccept { socket -> audioGroupServer!!.addClient(socket.inetAddress).localPort }
                audioGroupServer!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                Log.i(TAG, "AudioGroupServer running on IP " + info.groupOwnerAddress)
            }*//*
        }
    }*/


    private fun updateStatus(error: Int, inWifiGroup: Int = 0, currentListeners: Int = 0) {
        if (error < 0 && server.running) {
            tv_translating_language.text = getString(
                R.string.translating_status,
                currentLanguage,
                currentListeners
            )
        } else {
            tv_translating_language.text = getString(R.string.service_registration_error)
        }

        tv_translating_clients.text = listeners.values.joinToString("\n")
    }

    /** register the BroadcastReceiver with the intent values to be matched  */
    override fun onResume() {
        super.onResume()
        // TODO: Clean Up?
        /*mManager.discoverPeers(mChannel, null)
         service.registerService(object : WifiP2pManager.ActionListener {
             override fun onSuccess() {
                 Log.i(TAG, "registerService::onSuccess")
             }

             override fun onFailure(reason: Int) {
                 Log.i(TAG, "registerService::onFailure $reason")
             }
         })*/
        createGroup()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.translating_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.translating_update -> {
                requestGroupInfo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}