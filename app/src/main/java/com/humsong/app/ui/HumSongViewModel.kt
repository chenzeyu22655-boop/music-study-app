package com.humsong.app.ui

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.humsong.app.audio.HumAudioRecorder
import com.humsong.app.audio.WavFileWriter
import com.humsong.app.music.MelodyAnalyzer
import com.humsong.app.music.SongAnalysis
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HumSongUiState(
    val isRecording: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isPlaying: Boolean = false,
    val statusText: String = "准备好了，哼一句试试。",
    val analysis: SongAnalysis? = null,
    val recordingPath: String? = null,
    val errorMessage: String? = null
)

class HumSongViewModel(application: Application) : AndroidViewModel(application) {
    private val recorder = HumAudioRecorder()
    private val analyzer = MelodyAnalyzer()
    private var mediaPlayer: MediaPlayer? = null
    private val _state = MutableStateFlow(HumSongUiState())
    val state: StateFlow<HumSongUiState> = _state

    fun startRecording() {
        val started = recorder.start()
        if (!started) {
            _state.update {
                it.copy(
                    isRecording = false,
                    isAnalyzing = false,
                    statusText = "录音设备初始化失败",
                    errorMessage = "没有成功打开麦克风。请确认权限已允许，并尝试重新打开 App。"
                )
            }
            return
        }

        _state.update {
            it.copy(
                isRecording = true,
                isAnalyzing = false,
                statusText = "正在录音，保持单声部哼唱。",
                errorMessage = null
            )
        }
    }

    fun stopRecordingAndAnalyze() {
        _state.update {
            it.copy(
                isRecording = false,
                isAnalyzing = true,
                statusText = "正在分析旋律..."
            )
        }
        viewModelScope.launch {
            val samples = recorder.stop()
            val recordingFile = withContext(Dispatchers.IO) {
                saveRecording(samples)
            }
            val result = withContext(Dispatchers.Default) {
                analyzer.analyze(samples, HumAudioRecorder.SampleRate)
            }
            _state.update {
                it.copy(
                    isAnalyzing = false,
                    statusText = "分析完成，可以从这里继续创作。",
                    analysis = result,
                    recordingPath = recordingFile?.absolutePath,
                    errorMessage = null
                )
            }
        }
    }

    fun playLastRecording() {
        val path = state.value.recordingPath ?: return
        mediaPlayer?.release()
        val player = MediaPlayer()
        mediaPlayer = player
        player.setDataSource(path)
        player.setOnCompletionListener {
            it.release()
            if (mediaPlayer === it) mediaPlayer = null
            _state.update { current -> current.copy(isPlaying = false) }
        }
        player.prepare()
        player.start()
        _state.update { it.copy(isPlaying = true, statusText = "正在播放你的原始哼唱。") }
    }

    fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        _state.update { it.copy(isPlaying = false, statusText = "播放已停止。") }
    }

    fun setError(message: String) {
        _state.update { it.copy(errorMessage = message, statusText = "还不能录音") }
    }

    override fun onCleared() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onCleared()
    }

    private fun saveRecording(samples: ShortArray): File? {
        if (samples.isEmpty()) return null
        val directory = File(getApplication<Application>().filesDir, "recordings")
        val file = File(directory, "hum_${System.currentTimeMillis()}.wav")
        return WavFileWriter.writeMono16Bit(file, samples, HumAudioRecorder.SampleRate)
    }
}
