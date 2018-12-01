package io.github.janmalch.sacajawea.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import io.github.janmalch.sacajawea.WiFiBroadcastCallbacks

class WiFiDirectBroadcastReceiver(
    private val mManager: WifiP2pManager,
    private val mChannel: WifiP2pManager.Channel,
    private val mHandler: WiFiBroadcastCallbacks
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                mHandler.onWifiP2PChanged(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                /*mManager.requestPeers(mChannel) { peers: WifiP2pDeviceList? ->
                    // Handle peers list
                    peers?.also { mHandler.onReceivePeerList(peers.deviceList) }
                }*/
                mManager.requestPeers(mChannel, mHandler)
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Respond to new connection or disconnections
                /*mManager.requestConnectionInfo(mChannel) {
                    mHandler.onConnectionChanged(it)
                }*/mManager.requestConnectionInfo(mChannel, mHandler)
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Respond to this device's wifi state changing
                mHandler.onDeviceWifiChanged()
            }
        }
    }
}