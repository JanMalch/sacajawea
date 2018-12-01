package io.github.janmalch.sacajawea

import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.github.janmalch.sacajawea.receivers.WiFiDirectBroadcastReceiver

abstract class WiFiActivity : AppCompatActivity(), WiFiBroadcastCallbacks {
    protected lateinit var mChannel: WifiP2pManager.Channel
    protected lateinit var mManager: WifiP2pManager
    protected lateinit var mReceiver: WiFiDirectBroadcastReceiver
    protected val mIntentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        mChannel = mManager.initialize(this, mainLooper, null)
    }

    protected fun discoverPeers() {
        mManager.discoverPeers(mChannel, null) // TODO: null?
    }

    /** register the BroadcastReceiver with the intent values to be matched  */
    override fun onResume() {
        super.onResume()
        mReceiver = WiFiDirectBroadcastReceiver(mManager, mChannel, this)
        registerReceiver(mReceiver, mIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver)
    }

    override fun onWifiP2PChanged(enabled: Boolean) {
    }

    override fun onDeviceWifiChanged() {
    }

    override fun onPeersAvailable(peers: WifiP2pDeviceList?) {
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
    }
}