package io.github.janmalch.sacajawea

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import io.github.janmalch.sacajawea.listeners.Listener
import io.github.janmalch.sacajawea.service.TranslateService
import io.github.janmalch.sacajawea.udp.UdpAudioServer
import kotlinx.android.synthetic.main.activity_translating.*

class TranslatingActivity : WiFiActivity() {

    private val currentLanguage: String by lazy {
        getPreferenceString(R.string.pref_translate_language)
    }
    private val currentName: String by lazy {
        getPreferenceString(R.string.pref_name)
    }

    private val udpAudioServer = UdpAudioServer()
    private val listeners = mutableMapOf<String, Listener>() // Listeners()
    private val REQUEST_MICROPHONE = 100

    private lateinit var service: TranslateService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translating)
        stop_translating.setOnClickListener { stopTranslating() }

        udpAudioServer.newClients.subscribe { listeners[it.id] = it }
        udpAudioServer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        service = TranslateService(currentName, currentLanguage, udpAudioServer.port, mManager, mChannel)

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
        // TODO: AudioStream into UdpServer
        // Log.i(TAG, "setup recording ...")
        // recorder.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
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
                updateStatus(-1, it.clientList.size)
            }

        }
    }

    private fun stopTranslating() {
        udpAudioServer.cancel(true)
        service.unregisterService()
        mManager.removeGroup(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "removeGroup::onSuccess")
            }

            override fun onFailure(reason: Int) {
                Log.i(TAG, "removeGroup::onFailure")
            }
        })
        finish()
    }

    override fun onBackPressed() {
        stopTranslating()
        // finish()
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


    private fun updateStatus(error: Int, currentListeners: Int = 0) {
        if (error < 0 && udpAudioServer.running) {
            tv_translating_language.text = getString(
                R.string.translating_status,
                currentLanguage,
                currentListeners
            )
        } else {
            tv_translating_language.text = getString(R.string.service_registration_error)
        }
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