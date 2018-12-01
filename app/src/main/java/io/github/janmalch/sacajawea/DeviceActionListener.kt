package io.github.janmalch.sacajawea

import android.net.wifi.p2p.WifiP2pConfig

interface DeviceActionListener {
    fun cancelDisconnect()
    fun connect(config: WifiP2pConfig)
    fun disconnect()
}