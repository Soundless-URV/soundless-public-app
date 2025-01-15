package cat.urv.cloudlab.soundless.model.sensitivity

import cat.urv.cloudlab.soundless.viewmodel.Timestamp
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class SensitivityData (
    private var dBData: List<Double>,
    private var heartRateData: List<Double>,
    private var sleepStageData: List<Int>,
    var timestamp: List<Timestamp>,
){
    private val params = SensitivityParameters()

    // Extension function to calculate the standard deviation of a List<Float>
    private fun List<Double>.standardDeviation(): Double {
        val avg = average()
        val sum = fold(0.0) { acc, d -> acc + (d - avg).pow(2) }
        return sqrt(sum / size)
    }

    // Function to delete the initial and last awake data
    fun deleteAwakeData() {
        val firstNonAwakeIndex = sleepStageData.indexOfFirst { it > 0 }
        val lastNonAwakeIndex = sleepStageData.indexOfLast { it > 0 }

        if (firstNonAwakeIndex != -1 && lastNonAwakeIndex != -1) {
            dBData = dBData.subList(firstNonAwakeIndex, lastNonAwakeIndex + 1)
            heartRateData = heartRateData.subList(firstNonAwakeIndex, lastNonAwakeIndex + 1)
            sleepStageData = sleepStageData.subList(firstNonAwakeIndex, lastNonAwakeIndex + 1)
            timestamp = timestamp.subList(firstNonAwakeIndex, lastNonAwakeIndex + 1)
        }
    }

    fun isShortRecordings(): Boolean {
        return timestamp.size < 300
    }

    // Function to calculate the z-scores of the dB data
    fun zScoresDB(): Map<String, List<Double>> {
        val signals = MutableList(dBData.size) { 0.0 }
        val filteredY = dBData.toMutableList()
        val avgFilter = MutableList(dBData.size) { 0.0 }
        val stdFilter = MutableList(dBData.size) { 0.0 }

        avgFilter[params.lagDB - 1] = dBData.slice(0 until params.lagDB).average()
        stdFilter[params.lagDB - 1] = dBData.slice(0 until params.lagDB).standardDeviation()

        for (i in params.lagDB until dBData.size) {
            if (abs(dBData[i] - avgFilter[i-1]) > params.thresholdDB * stdFilter[i-1] && dBData[i] >= 45) {
                if (dBData[i] > avgFilter[i-1]) {
                    signals[i] = 1.0
                } else {
                    signals[i] = -1.0
                }

                filteredY[i] = params.influenceDB * dBData[i] + (1 - params.influenceDB) * filteredY[i-1]
                avgFilter[i] = filteredY.slice((i-params.lagDB+1)..i).average()
                stdFilter[i] = filteredY.slice((i-params.lagDB+1)..i).standardDeviation()
            } else {
                signals[i] = 0.0
                filteredY[i] = dBData[i]
                avgFilter[i] = filteredY.slice((i-params.lagDB+1)..i).average()
                stdFilter[i] = filteredY.slice((i-params.lagDB+1)..i).standardDeviation()
            }
        }

        return mapOf(
            "signals" to signals,
            "avgFilter" to avgFilter,
            "stdFilter" to stdFilter
        )
    }

    // Function to calculate the z-scores of the heart rate data
    fun zScoresHR(): Map<String, List<Double>> {
        val signals = MutableList(heartRateData.size) { 0.0 }
        val filteredY = heartRateData.toMutableList()
        val avgFilter = MutableList(heartRateData.size) { 0.0 }
        val stdFilter = MutableList(heartRateData.size) { 0.0 }

        avgFilter[params.lagHR - 1] = heartRateData.slice(0 until params.lagHR).average()
        stdFilter[params.lagHR - 1] = heartRateData.slice(0 until params.lagHR).standardDeviation()

        for (i in params.lagHR until heartRateData.size) {
            if (abs(heartRateData[i] - avgFilter[i-1]) > params.thresholdHR * stdFilter[i-1]) {
                if (heartRateData[i] > avgFilter[i-1]) {
                    signals[i] = 1.0
                } else {
                    signals[i] = -1.0
                }

                filteredY[i] = params.influenceHR * heartRateData[i] + (1 - params.influenceHR) * filteredY[i-1]
                avgFilter[i] = filteredY.slice((i-params.lagHR+1)..i).average()
                stdFilter[i] = filteredY.slice((i-params.lagHR+1)..i).standardDeviation()
            } else {
                signals[i] = 0.0
                filteredY[i] = heartRateData[i]
                avgFilter[i] = filteredY.slice((i-params.lagHR+1)..i).average()
                stdFilter[i] = filteredY.slice((i-params.lagHR+1)..i).standardDeviation()
            }
        }

        return mapOf(
            "signals" to signals,
            "avgFilter" to avgFilter,
            "stdFilter" to stdFilter
        )
    }

    // Function to calculate the incidences of deep sleep (an incidence is defined as a moment when the sleep stage changes from deep sleep or REM to Awake)
    fun incidencesSleep(): List<Double> {
        val dataList = mutableListOf<Double>()
        var position = 0
        var lastDeepSleepPosition = -1

        for (row in sleepStageData) {
            if (row >= 2) {
                dataList.add(0.0)
                lastDeepSleepPosition = position
            } else if (row <= 0 && lastDeepSleepPosition != -1 && position <= lastDeepSleepPosition + 4) {
                dataList.add(1.0)
            } else {
                dataList.add(0.0)
            }

            position += 1
        }

        return dataList
    }

    // Function to get the timestamps of the incidence of the signals
    // Incident is defined as the moment when the signal changes from 0 to 1
    fun getTimestampsIncidence(signals: List<Double>): List<Timestamp> {
        var signalsTimestamps = timestamp.zip(signals)

        signalsTimestamps = signalsTimestamps.filterIndexed { index, pair ->
            pair.second != 0.0 && (index == 0 || signals[index - 1] == 0.0)
        }

        return signalsTimestamps.map { it.first }
    }

    // Function to match the timestamps of the dB data with the timestamps of the heart rate and sleep stage data
    fun matchDBIncidencesTimestamps(
        dBIncidences: List<Timestamp>,
        heartRateIncidences: List<Timestamp>,
        sleepStageIncidences: List<Timestamp>
    ): Triple<List<Timestamp>, List<Timestamp>, List<Timestamp>> {
        val heartRateDBIncidences = mutableListOf<Timestamp>()
        val sleepStageDBIncidences = mutableListOf<Timestamp>()
        val combinedTimestamps = mutableListOf<Timestamp>()

        for (heartRateIncidence in heartRateIncidences) {
            for (dBIncidence in dBIncidences) {
                if (abs(heartRateIncidence - dBIncidence) <= 50000) {
                    if (!heartRateDBIncidences.contains(heartRateIncidence)) {
                        heartRateDBIncidences.add(heartRateIncidence)
                    }
                }
            }
        }

        for (sleepStageIncidence in sleepStageIncidences) {
            for (dBIncidence in dBIncidences) {
                if (abs(sleepStageIncidence - dBIncidence) <= 50000) {
                    if (!sleepStageDBIncidences.contains(sleepStageIncidence)) {
                        sleepStageDBIncidences.add(sleepStageIncidence)
                    }
                }
            }
        }

        for (sleepStageIncidence in sleepStageDBIncidences) {
            for (heartRateIncidence in heartRateDBIncidences) {
                if (abs(sleepStageIncidence - heartRateIncidence) <= 50000) {
                    combinedTimestamps.add(sleepStageIncidence)
                    break
                }
            }
        }

        return Triple(heartRateDBIncidences, sleepStageDBIncidences, combinedTimestamps)
    }

    // Sensitivity of heartRate and dB
    // Sensitivity of sleepStage and dB
    // Sensitivity of combined heartRate and sleepStage and dB
    fun getSensitivity (incidences: List<Timestamp>): List<Double> {
        val meanDb = mutableListOf<Double>()

        for (i in incidences.indices) {
            val currentTimestamp = incidences[i]
            val currentIndex = timestamp.indexOf(currentTimestamp)

            val dbCurrent = dBData[currentIndex]
            val dbPrevious = if (currentIndex > 0) dBData[currentIndex - 1] else dbCurrent
            val dbNext = if (currentIndex < dBData.size - 1) dBData[currentIndex + 1] else dbCurrent
            val dbNextNext =
                if (currentIndex < dBData.size - 2) dBData[currentIndex + 2] else dbCurrent

            val meanFourTimestamps = (dbCurrent + dbPrevious + dbNext + dbNextNext) / 4.0

            meanDb.add(meanFourTimestamps)
        }

        return meanDb
    }
}