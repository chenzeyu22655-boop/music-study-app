package com.humsong.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HumAudioRecorder {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val samples = mutableListOf<Short>()

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        samples.clear()
        val minBufferSize = AudioRecord.getMinBufferSize(
            SampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) return false

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 2
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return false
        }

        audioRecord = recorder
        recorder.startRecording()
        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ShortArray(minBufferSize)
            while (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    synchronized(samples) {
                        repeat(read) { index -> samples.add(buffer[index]) }
                    }
                }
            }
        }
        return true
    }

    suspend fun stop(): ShortArray = withContext(Dispatchers.IO) {
        val recorder = audioRecord
        if (recorder != null && recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            recorder.stop()
        }
        recordingJob?.join()
        recorder?.release()
        audioRecord = null
        synchronized(samples) { samples.toShortArray() }
    }

    companion object {
        const val SampleRate = 44_100
    }
}
