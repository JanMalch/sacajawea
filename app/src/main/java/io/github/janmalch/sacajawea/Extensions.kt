package io.github.janmalch.sacajawea

import android.app.Activity
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import java.io.InputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.net.NetworkInterface.getByInetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder


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


infix fun Int.stepOf(maxSteps: Int): Int = Math.ceil(this.toDouble() * 100 / maxSteps).toInt()

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

val WifiP2pDevice.p2pConfig: WifiP2pConfig
    get() {
        val config = WifiP2pConfig()
        config.deviceAddress = this.deviceAddress
        return config
    }

val Int.formatAsIpAddress: String
    get() = String.format(
        "%d.%d.%d.%d", this and 0xff, this shr 8 and 0xff, this shr 16 and 0xff,
        this shr 24 and 0xff
    )

fun ViewGroup.inflate(layout: Int): View {
    return LayoutInflater.from(this.context)
        .inflate(layout, this, false)
}

fun <T, K> MutableList<T>.addAllAndDistinct(newValues: Collection<T>, distinctBy: (T) -> K) {
    val temp = this.toMutableList()
    this.clear()
    temp.addAll(newValues)
    this.addAll(temp.distinctBy(distinctBy))
}

fun <T, K> MutableList<T>.addAndDistinct(value: T, distinctBy: (T) -> K) {
    val temp = this.toMutableList()
    this.clear()
    temp.add(value)
    this.addAll(temp.distinctBy(distinctBy))
}

fun Context.toast(text: String, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, text, duration).show()
}

val Int.Seconds: Long
    get() = 1000L * this

val Int.Minutes: Long
    get() = 1000L * 60 * this

val Int.Hours: Long
    get() = 1000L * 60 * 60 * this

// TODO: remove
fun InputStream.convertToString(): String {
    val s = Scanner(this).useDelimiter("\\A")
    return if (s.hasNext()) s.next() else ""
}

val Context.TAG: String
    get() = this::class.java.canonicalName!!


fun Context.getPreferenceString(key: Int, defValue: String = ""): String {
    val sharedPref = this.getSharedPreferences("io.github.janmalch.sacajawea", Context.MODE_PRIVATE)
    return sharedPref.getString(this.getString(key), defValue)!!
}

fun ByteArray.toShortArray(): ShortArray {
    val shorts = ShortArray(this.size / 2)
    ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
    return shorts
}

fun ShortArray.toByteArray(): ByteArray {
    val bytes = ByteArray(this.size * 2)
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(this)
    return bytes
}
