package io.github.janmalch.sacajawea

import android.net.wifi.p2p.WifiP2pManager

interface WiFiBroadcastCallbacks : WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {
    /**
     * WiFi is enabled
     */
    fun onWifiP2PChanged(enabled: Boolean)

    /**
     * Respond to new connection or disconnections.
     */
    // fun onConnectionChanged(info: WifiP2pInfo)

    /**
     * Respond to this device's wifi state changing
     */
    fun onDeviceWifiChanged()
}