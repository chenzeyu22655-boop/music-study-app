package com.humsong.app.music

object PitchDetector {
    fun detect(frame: ShortArray, sampleRate: Int): Double {
        val minLag = sampleRate / 1000
        val maxLag = sampleRate / 80
        var bestLag = -1
        var bestScore = Double.NEGATIVE_INFINITY

        for (lag in minLag..maxLag) {
            var score = 0.0
            var index = 0
            while (index + lag < frame.size) {
                score += frame[index] * frame[index + lag].toDouble()
                index += 1
            }
            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }

        return if (bestLag > 0) sampleRate.toDouble() / bestLag else 0.0
    }
}
