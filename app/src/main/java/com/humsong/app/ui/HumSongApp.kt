package com.humsong.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HumSongApp(viewModel: HumSongViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startRecording() else viewModel.setError("需要麦克风权限才能录制哼唱。")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "哼唱作曲助手",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "按下录音，哼一小段旋律。停止后我会先用本地算法整理成简谱和基础和弦。",
            style = MaterialTheme.typography.bodyLarge
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = state.statusText, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        enabled = !state.isAnalyzing,
                        onClick = {
                            if (state.isRecording) viewModel.stopRecordingAndAnalyze()
                            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    ) {
                        Text(if (state.isRecording) "停止并分析" else "开始录音")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "建议 5-20 秒")
                }
            }
        }

        state.errorMessage?.let {
            ResultCard(title = "提示", content = it)
        }

        state.analysis?.let { analysis ->
            ResultCard(title = "调式", content = "${analysis.keyName} 调\n${analysis.keyDescription}")
            ResultCard(title = "简谱", content = analysis.jianpu)
            ResultCard(title = "推荐和弦", content = analysis.chords.joinToString(" - "))
            ResultCard(title = "创作建议", content = analysis.tip)
        }

        state.recordingPath?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        enabled = !state.isRecording && !state.isAnalyzing,
                        onClick = {
                            if (state.isPlaying) viewModel.stopPlayback()
                            else viewModel.playLastRecording()
                        }
                    ) {
                        Text(if (state.isPlaying) "停止播放" else "播放我的哼唱")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "已保存本次录音")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "第一版重点是打通创作流程。后续可以把音高识别模块替换成 Basic Pitch、TensorFlow Lite 或云端 AI。",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ResultCard(title: String, content: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = content, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
