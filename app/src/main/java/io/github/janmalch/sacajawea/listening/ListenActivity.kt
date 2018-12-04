package io.github.janmalch.sacajawea.listening

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import io.github.janmalch.sacajawea.*
import io.github.janmalch.sacajawea.wifi.WiFiActivity
import kotlinx.android.synthetic.main.activity_listen.*


class ListenActivity : WiFiActivity() {

    private val deviceList = mutableListOf<Translator>()
    private val listener: ListenerService by lazy {
        ListenerService(mManager, mChannel) {
            timeout?.apply { removeCallbacks(null) }
            deviceList.clear()
            deviceList.addAll(it.values)
            listen_recycler.adapter?.notifyDataSetChanged()
            listen_swipe_container.isRefreshing = false
        }
    }

    private var timeout: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listen)

        listen_recycler.layoutManager = LinearLayoutManager(this)
        listen_recycler.adapter = DeviceRVAdapter(deviceList) {
            timeout?.apply { removeCallbacks(null) }
            AppState.activeTranslator = it
            val i = Intent(this, ListeningActivity::class.java)
            startActivity(i)
        }

        listen_swipe_container.setOnRefreshListener(::startDiscovery)
        listen_swipe_container.setColorSchemeResources(
            R.color.primary,
            R.color.google_green,
            R.color.google_yellow,
            android.R.color.holo_red_light
        )
    }

    override fun onResume() {
        super.onResume()
        listen_swipe_container.post(::startDiscovery)
    }

    private fun startDiscovery() {
        listen_swipe_container.isRefreshing = true
        deviceList.clear()
        listen_recycler.adapter?.notifyDataSetChanged()
        timeout = Handler().apply {
            postDelayed({
                listen_swipe_container.isRefreshing = false
                toast("Operation timed out. No devices found.")
            }, 30.Seconds)
        }
        listener.startDiscovery()
    }

    // ------------------------------ WiFi Callbacks ------------------------------

    /*override fun onWifiP2PChanged(enabled: Boolean) {
        // Log.i(TAG, "onWifiP2PChanged :: Enabled = $enabled")
    }

    override fun onDeviceWifiChanged() {
        // Log.i(TAG, "onDeviceWifiChanged")
    }

    override fun onPeersAvailable(peers: WifiP2pDeviceList?) {
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
    }*/

    // ------------------------------ Activity life cycles etc. ------------------------------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.listen_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.listen_scan_qr -> {
                toast("Not yet implemented")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
