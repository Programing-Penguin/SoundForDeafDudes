package com.example.soundalertfinal

data class MappedSound(val type: SoundType, val confidence: Float)

enum class SoundType(val display: String) {
    SIREN("Siren"),
    ALARM("Alarm / Smoke Detector"),
    DOORBELL("Doorbell / Knock"),
    BABY("Baby Cry"),
    HORN("Car Horn")
}

object SoundMapper {

    // Tune these thresholds as you test
    private const val THRESH_SIREN = 0.35f
    private const val THRESH_ALARM = 0.35f
    private const val THRESH_DOORBELL = 0.35f
    private const val THRESH_BABY = 0.35f
    private const val THRESH_HORN = 0.35f

    fun map(scores: YamnetScores): MappedSound? {
        // Find best label
        val labels = scores.labels
        val probs = scores.scores

        var bestIdx = 0
        var best = probs[0]
        for (i in 1 until probs.size) {
            if (probs[i] > best) {
                best = probs[i]
                bestIdx = i
            }
        }
        val bestLabel = labels.getOrNull(bestIdx)?.lowercase() ?: return null

        // Keyword mapping (simple & effective for MVP-final)
        // You can improve by checking top-K labels rather than only best.
        return when {
            // Siren
            bestLabel.contains("siren") && best >= THRESH_SIREN ->
                MappedSound(SoundType.SIREN, best)

            // Alarm / smoke detector
            (bestLabel.contains("smoke alarm") || bestLabel.contains("alarm")) && best >= THRESH_ALARM ->
                MappedSound(SoundType.ALARM, best)

            // Doorbell / knock
            (bestLabel.contains("doorbell") || bestLabel.contains("knock")) && best >= THRESH_DOORBELL ->
                MappedSound(SoundType.DOORBELL, best)

            // Baby
            (bestLabel.contains("baby") || bestLabel.contains("crying")) && best >= THRESH_BABY ->
                MappedSound(SoundType.BABY, best)

            // Horn
            bestLabel.contains("car horn") && best >= THRESH_HORN ->
                MappedSound(SoundType.HORN, best)

            else -> null
        }
    }
}
