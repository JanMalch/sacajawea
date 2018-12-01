package io.github.janmalch.sacajawea.service

import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo

class TranslateService(
    private val name: String,
    private val language: String,
    private val SERVER_PORT: Int,
    private val mManager: WifiP2pManager,
    private val mChannel: WifiP2pManager.Channel
) {


    var running: Boolean = false
        private set

    fun registerService(listener: WifiP2pManager.ActionListener?) {
        unregisterService()
        println(SERVER_PORT.toString())
        //  Create a string map containing information about your service.
        val record: Map<String, String> = mapOf(
            "port" to SERVER_PORT.toString(),
            "name" to name,
            "language" to language,
            "available" to "visible"
        )

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("_$name", "_presence._tcp", record)
        mManager.addLocalService(mChannel, serviceInfo, listener)
        running = true
    }

    fun unregisterService() {
        mManager.clearLocalServices(mChannel, null)
        running = false
    }

}