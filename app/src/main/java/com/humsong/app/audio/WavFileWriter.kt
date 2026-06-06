package com.humsong.app.audio

import java.io.File
import java.io.FileOutputStream

object WavFileWriter {
    fun writeMono16Bit(file: File, samples: ShortArray, sampleRate: Int): File {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { output ->
            val dataSize = samples.size * 2
            output.writeString("RIFF")
            output.writeIntLe(36 + dataSize)
            output.writeString("WAVE")
            output.writeString("fmt ")
            output.writeIntLe(16)
            output.writeShortLe(1)
            output.writeShortLe(1)
            output.writeIntLe(sampleRate)
            output.writeIntLe(sampleRate * 2)
            output.writeShortLe(2)
            output.writeShortLe(16)
            output.writeString("data")
            output.writeIntLe(dataSize)
            samples.forEach { output.writeShortLe(it.toInt()) }
        }
        return file
    }

    private fun FileOutputStream.writeString(value: String) {
        write(value.toByteArray(Charsets.US_ASCII))
    }

    private fun FileOutputStream.writeIntLe(value: Int) {
        write(value and 0xff)
        write(value shr 8 and 0xff)
        write(value shr 16 and 0xff)
        write(value shr 24 and 0xff)
    }

    private fun FileOutputStream.writeShortLe(value: Int) {
        write(value and 0xff)
        write(value shr 8 and 0xff)
    }
}
