package com.humsong.app.music

object ChordRecommender {
    fun recommend(notes: List<MelodyNote>, keyRoot: Int): List<String> {
        val rootName = noteName(keyRoot)
        val fourth = noteName((keyRoot + 5) % 12)
        val fifth = noteName((keyRoot + 7) % 12)
        val sixthMinor = noteName((keyRoot + 9) % 12) + "m"

        val lastDegree = (notes.last().midi % 12 - keyRoot + 12) % 12
        return if (lastDegree in setOf(0, 4, 7)) {
            listOf(rootName, fifth, sixthMinor, fourth)
        } else {
            listOf(sixthMinor, fourth, rootName, fifth)
        }
    }

    private fun noteName(pitchClass: Int): String {
        return listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")[pitchClass]
    }
}
