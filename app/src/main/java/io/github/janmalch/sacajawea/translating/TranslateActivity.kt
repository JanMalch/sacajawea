package io.github.janmalch.sacajawea.translating

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_translate.*
import android.net.ConnectivityManager
import android.view.View
import io.github.janmalch.sacajawea.R
import io.github.janmalch.sacajawea.afterTextChanged
import io.github.janmalch.sacajawea.bindToPreference
import io.github.janmalch.sacajawea.formatAsIpAddress


class TranslateActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translate)

        languages_list.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                resources.getStringArray(R.array.languages)
            )
        )

        name.bindToPreference(R.string.pref_name)
        languages_list.bindToPreference(R.string.pref_translate_language)

        name.afterTextChanged { updatePreview() }
        languages_list.afterTextChanged { updatePreview() }

        start_translating.setOnClickListener {
            startActivity(Intent(this, TranslatingActivity::class.java))
        }

        updatePreview()
        updateWiFiStatus()
    }

    private fun updateWiFiStatus() {
        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        if (mWifi.isConnected) {
            connect_to_wifi.visibility = View.GONE
            start_translating.visibility = View.VISIBLE
        } else {
            connect_to_wifi.visibility = View.VISIBLE
            start_translating.visibility = View.GONE
        }
    }

    private fun updatePreview() {
        preview_name.text = name.text.toString()
        preview_language.text = languages_list.text.toString()
        preview_ip_address.text = getIpAddress()
    }

    private fun getIpAddress(): String {
        val wifiMgr = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiMgr.connectionInfo.ipAddress.formatAsIpAddress
    }

}
