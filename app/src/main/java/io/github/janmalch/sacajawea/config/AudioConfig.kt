package io.github.janmalch.sacajawea.config

import android.media.AudioFormat

object AudioConfig {
    val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    val SAMPLE_RATE = 8000 // Hertz
    val SAMPLE_INTERVAL = 20 // Milliseconds
    val SAMPLE_SIZE = 2 // Bytes
    val BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2 //Bytes
}