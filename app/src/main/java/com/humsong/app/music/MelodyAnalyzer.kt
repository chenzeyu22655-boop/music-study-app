package com.humsong.app.music

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

class MelodyAnalyzer {
    fun analyze(samples: ShortArray, sampleRate: Int): SongAnalysis {
        val notes = detectNotes(samples, sampleRate)
        if (notes.isEmpty()) {
            return SongAnalysis(
                keyName = "C",
                keyDescription = "暂未识别到稳定调式，先按 C 调示例显示。",
                jianpu = "没有识别到稳定音高，请靠近麦克风再哼一次。",
                chords = listOf("C", "G", "Am", "F"),
                tip = "第一版适合清唱单旋律。哼唱时尽量少滑音，每个音稍微停稳一点。"
            )
        }

        val keyRoot = estimateMajorKey(notes)
        val keyName = noteName(keyRoot)
        val jianpu = JianpuConverter.toJianpu(notes, keyRoot)
        val chords = ChordRecommender.recommend(notes, keyRoot)
        val tip = if (notes.last().midi % 12 == keyRoot) {
            "结尾落在主音上，听感会比较稳定。可以把它发展成副歌第一句。"
        } else {
            "这段结尾还有继续感，适合作为主歌开头。下一句可以尝试回到 1，让旋律更完整。"
        }
        return SongAnalysis(
            keyName = keyName,
            keyDescription = "当前简谱按 $keyName 调显示，也就是数字 1 约等于 $keyName。八度高低按中央 C 附近作参考标记。",
            jianpu = jianpu,
            chords = chords,
            tip = tip
        )
    }

    private fun detectNotes(samples: ShortArray, sampleRate: Int): List<MelodyNote> {
        if (samples.size < sampleRate / 2) return emptyList()

        val frameSize = 2048
        val hopSize = 1024
        val detected = mutableListOf<Int>()
        var offset = 0
        while (offset + frameSize < samples.size) {
            val frame = samples.copyOfRange(offset, offset + frameSize)
            val rms = frame.rms()
            if (rms > 700) {
                val frequency = PitchDetector.detect(frame, sampleRate)
                if (frequency in 80.0..1000.0) {
                    detected.add(frequencyToMidi(frequency))
                }
            }
            offset += hopSize
        }

        if (detected.isEmpty()) return emptyList()

        val notes = mutableListOf<MelodyNote>()
        var current = detected.first()
        var duration = 0
        for (midi in detected) {
            if (abs(midi - current) <= 1) {
                current = ((current + midi) / 2.0).roundToInt()
                duration += 1
            } else {
                if (duration >= 2) notes.add(MelodyNote(current, duration))
                current = midi
                duration = 1
            }
        }
        if (duration >= 2) notes.add(MelodyNote(current, duration))
        return notes.take(32)
    }

    private fun estimateMajorKey(notes: List<MelodyNote>): Int {
        val pitchClasses = notes.map { it.midi % 12 }
        val candidates = (0..11).toList()
        return candidates.maxBy { root ->
            val scale = setOf(
                root,
                (root + 2) % 12,
                (root + 4) % 12,
                (root + 5) % 12,
                (root + 7) % 12,
                (root + 9) % 12,
                (root + 11) % 12
            )
            pitchClasses.count { it in scale } + pitchClasses.count { it == root } * 2
        }
    }

    private fun frequencyToMidi(frequency: Double): Int {
        return (69 + 12 * ln(frequency / 440.0) / ln(2.0)).roundToInt()
    }

    private fun noteName(pitchClass: Int): String {
        return listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")[pitchClass]
    }
}

private fun ShortArray.rms(): Double {
    var sum = 0.0
    for (sample in this) {
        sum += sample.toDouble().pow(2)
    }
    return kotlin.math.sqrt(sum / max(1, size))
}
