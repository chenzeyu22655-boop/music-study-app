package com.humsong.app.music

import kotlin.math.abs

object JianpuConverter {
    private val majorScale = listOf(0, 2, 4, 5, 7, 9, 11)

    fun toJianpu(notes: List<MelodyNote>, keyRoot: Int): String {
        return notes.mapIndexed { index, note ->
            val degree = toDegree(note.midi, keyRoot)
            val lengthMark = if (note.durationFrames >= 6) " -" else ""
            val bar = if ((index + 1) % 4 == 0 && index != notes.lastIndex) " |" else ""
            "$degree$lengthMark$bar"
        }.joinToString(" ")
    }

    private fun toDegree(midi: Int, keyRoot: Int): String {
        val pitchClass = (midi % 12 - keyRoot + 12) % 12
        val nearest = majorScale.minBy { abs(it - pitchClass) }
        val degree = majorScale.indexOf(nearest) + 1
        val accidental = when {
            pitchClass == nearest -> ""
            pitchClass > nearest -> "#"
            else -> "b"
        }
        val octave = (midi - 60) / 12
        val dots = when {
            octave > 0 -> "'".repeat(octave)
            octave < 0 -> ",".repeat(abs(octave))
            else -> ""
        }
        return "$accidental$degree$dots"
    }
}
