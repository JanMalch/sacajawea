package io.github.janmalch.sacajawea.listening.messages

import java.io.Serializable

fun String.prepend(prefix: String): String = prefix + this

sealed class Message(val payload: Serializable?) {
    abstract fun getMessage(): String
}

class HandshakeMessage(payload: String) : Message(payload) {
    override fun getMessage(): String = "NAME: $payload"
}

class GoodbyeMessage : Message(null) {
    override fun getMessage(): String = "GOODBYE"
}

class SpeedRatingMessage(payload: Int) : Message(payload) {
    override fun getMessage(): String = "SPEED: $payload"
}

class VolumeRatingMessage(payload: Int) : Message(payload) {
    override fun getMessage(): String = "VOLUME: $payload"
}

class OverallRatingMessage(payload: Int) : Message(payload) {
    override fun getMessage(): String = "RATING: $payload"
}

