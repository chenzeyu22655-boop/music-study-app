package com.humsong.app.music

data class MelodyNote(
    val midi: Int,
    val durationFrames: Int
)

data class SongAnalysis(
    val keyName: String,
    val keyDescription: String,
    val jianpu: String,
    val chords: List<String>,
    val tip: String
)
