package io.github.janmalch.sacajawea.listening

import java.security.MessageDigest

class Listeners {

    private val map = mutableMapOf<String, Listener>()

    fun add(name: String, ip: String) {
        map[ip] = Listener(name, ip)
    }

    fun remove(ip: String) {
        map.remove(ip)
    }

    fun toList(): List<Listener> {
        return map.values.toList()
    }

    override fun toString(): String {
        return toList().joinToString("\n", "• ")
    }
}


data class Listener(val name: String, val ip: String) {

    val id: String

    init {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(ip.toByteArray())
        val messageDigest = md.digest()

        // Create Hex String
        val hexString = StringBuilder()
        for (aMessageDigest in messageDigest) {
            var h = Integer.toHexString(0xFF and aMessageDigest.toInt())
            while (h.length < 2)
                h = "0$h"
            hexString.append(h)
        }
        id = hexString.toString().substring(0, 6)
    }

    override fun toString(): String {
        return "$name\t#$id"
    }
}