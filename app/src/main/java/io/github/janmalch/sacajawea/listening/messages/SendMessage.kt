package io.github.janmalch.sacajawea.listening.messages

import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

fun sendMessages(host: String, port: Int, vararg messages: Message) {
    Socket().use {
        it.bind(null)
        it.connect(InetSocketAddress(host, port), 500)
        // TODO: refactor to ObjectStream
        messages.forEach { message -> it.getOutputStream().write("${message.getMessage()}\n".toByteArray()) }
        it.getOutputStream().flush()
    }
}

class Messenger(private val host: String, private val port: Int) {
    private val threads = Executors.newCachedThreadPool()

    fun send(vararg messages: Message) {
        threads.submit { sendMessages(host, port, *messages) }
    }
}