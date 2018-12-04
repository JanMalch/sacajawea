package io.github.janmalch.sacajawea.listening

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.*
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE
import android.util.Log

class ListenerService(
    private val mManager: WifiP2pManager,
    private val mChannel: WifiP2pManager.Channel,
    // private val onDiscover: (device: WifiP2pDevice) -> Unit
    private val onDiscover: (translators: Map<String, Translator>) -> Unit
) {


    val translators = mutableMapOf<String, Translator>()

    fun startDiscovery() {
        stopDiscovery()

        Log.e("ListenerService::0", "Setting up ...")

        val serviceListener = WifiP2pManager.ServiceResponseListener { protocolType, responseData, srcDevice ->
            Log.i(
                "ListenerService::1",
                "onServiceAvailable: protocolType:" + protocolType + ", responseData: " + responseData.toString()
                        + ", WifiP2pDevice: " + srcDevice.toString()
            )
        }

        mManager.setServiceResponseListener(mChannel, serviceListener)

        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { fullDomain, record, device ->
            Log.i("ListenerService::2", "DnsSdTxtRecord available -$record -$fullDomain")

            translators[device.deviceAddress] = Translator(
                record["name"],
                record["language"],
                device,
                record["port"]!!.toInt()
            )
        }

        val servListener = WifiP2pManager.DnsSdServiceResponseListener { instanceName, registrationType, device ->
            // Update the device name with the human-friendly version from
            // the DnsTxtRecord, assuming one arrived.

            translators[device.deviceAddress]?.also {
                it.name = it.name ?: device.deviceName
            }
            onDiscover(translators)

            // Add to the custom adapter defined specifically for showing wifi devices.
            Log.d("ListenerService::3", "onBonjourServiceAvailable $instanceName")
        }

        mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener)

        val upnpListener = WifiP2pManager.UpnpServiceResponseListener {
            uniqueServiceNames, srcDevice ->
            Log.d("ListenerService::4", "uniqueServiceNames:" + uniqueServiceNames.toString() + ", WifiP2pDevice: "
                    + srcDevice.toString())
        }

        mManager.setUpnpServiceResponseListener(mChannel, upnpListener)

        addServiceRequest(WifiP2pServiceRequest.newInstance(WifiP2pServiceInfo.SERVICE_TYPE_ALL))
        addServiceRequest(WifiP2pDnsSdServiceRequest.newInstance())
        addServiceRequest(WifiP2pUpnpServiceRequest.newInstance())
        mManager.discoverServices(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("ListenerService::7", "discoverServices.onSuccess()")
            }

            override fun onFailure(reason: Int) {
                Log.d("ListenerService::8", "discoverServices.onFailure() $reason")
            }
        })
    }

    fun stopDiscovery() {
        mManager.clearServiceRequests(mChannel, null)
    }

    private fun addServiceRequest(request: WifiP2pServiceRequest) {
        mManager.addServiceRequest(mChannel, request, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("ListenerService::5", "addServiceRequest.onSuccess() for requests of type: " + request.javaClass.simpleName)
            }

            override fun onFailure(reason: Int) {
                Log.d("ListenerService::6", "addServiceRequest.onFailure: " + reason + ", for requests of type: "
                        + request.javaClass.simpleName
                )
            }
        })
    }
}

data class Translator(var name: String?, var language: String?, val device: WifiP2pDevice, val port: Int): Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readParcelable(WifiP2pDevice::class.java.classLoader),
        parcel.readInt()
    )

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.also {
            it.writeString(name)
            it.writeInt(port)
            it.writeParcelable(device, PARCELABLE_WRITE_RETURN_VALUE)
        }

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Translator> {
        override fun createFromParcel(parcel: Parcel): Translator {
            return Translator(parcel)
        }

        override fun newArray(size: Int): Array<Translator?> {
            return arrayOfNulls(size)
        }
    }

}

