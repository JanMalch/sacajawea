package io.github.janmalch.sacajawea

import android.app.Activity
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast

// --- ANDROID UI ELEMENTS ---

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
    })
}

fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

fun EditText.bindToPreference(
    prefKey: Int,
    default: String = "",
    activity: Activity = this.context as Activity,
    mode: Int = Context.MODE_PRIVATE
) {
    val sharedPref = activity.getSharedPreferences("io.github.janmalch.sacajawea", mode)
    val currentName = sharedPref.getString(activity.getString(prefKey), default)

    this.text = (currentName ?: "").toEditable()

    this.afterTextChanged {
        with(sharedPref.edit()) {
            putString(activity.getString(prefKey), it)
            apply()
        }
    }
}

fun SeekBar.onSeekBarRelease(onStopTrackingTouch: (SeekBar?) -> Unit) {
    this.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            onStopTrackingTouch(seekBar)
        }
    })
}

fun ViewGroup.inflate(layout: Int): View {
    return LayoutInflater.from(this.context)
        .inflate(layout, this, false)
}

// --- ANDROID WiFi P2P ---

val WifiP2pDevice.p2pConfig: WifiP2pConfig
    get() {
        val config = WifiP2pConfig()
        config.deviceAddress = this.deviceAddress
        return config
    }

// --- CONTEXT ---

fun Context.toast(text: String, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, text, duration).show()
}

val Context.TAG: String
    get() = this::class.java.canonicalName!!

fun Context.getPreferenceString(key: Int, defValue: String = ""): String {
    val sharedPref = this.getSharedPreferences("io.github.janmalch.sacajawea", Context.MODE_PRIVATE)
    return sharedPref.getString(this.getString(key), defValue)!!
}

// --- MATH ---

val Int.Seconds: Long
    get() = 1000L * this

infix fun Int.stepOf(maxSteps: Int): Int = Math.ceil(this.toDouble() * 100 / maxSteps).toInt()

val Int.formatAsIpAddress: String
    get() = String.format(
        "%d.%d.%d.%d", this and 0xff, this shr 8 and 0xff, this shr 16 and 0xff,
        this shr 24 and 0xff
    )
