package cat.urv.cloudlab.soundless.model.repository.health.fitbit

import cat.urv.cloudlab.soundless.model.repository.EntityMapper
import cat.urv.cloudlab.soundless.model.repository.health.SleepStageValues
import cat.urv.cloudlab.soundless.viewmodel.Measurement
import okhttp3.internal.format
import okhttp3.internal.toImmutableList
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Given a [FitbitHeartRateEntity] and a [FitbitSleepEntity], and a list of existing [Measurement]s,
 * this class uses methods to merge Sleep and Health data into the [Measurement]s properly.
 */
class FitbitMapper :
    EntityMapper<
        Triple<FitbitHeartRateEntity, FitbitSleepEntity, List<Measurement>>,    // inputs
        List<Measurement>>                                                      // output
{
    private val heartRateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val sleepFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd\'T\'HH:mm:ss.SSS")
    private val yyMMddFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val zoneId = ZoneId.systemDefault()

    override fun mapFromEntity(
        entity: Triple<FitbitHeartRateEntity, FitbitSleepEntity, List<Measurement>>
    ): List<Measurement> {

        val (heartRate, sleep, measurements) = entity
        updateMeasurementsWithHeartRate(measurements, heartRate)
        updateMeasurementsWithSleep(measurements, sleep)
        return measurements
    }

    private fun updateMeasurementsWithHeartRate(
        measurements: List<Measurement>,
        heartRate: FitbitHeartRateEntity
    ) {
        /** Heart rate **/

        val heartRateDataset = heartRate.heartRateData.dataset
        val dateTimesInDataset = heartRate.heartRateMetadata.map { it.dateTime }.toMutableList()

        val lastHeartRateDatumIndexForEachDay = getLastHeartRateDatumIndexForEachDay(
            dateTimesInDataset,
            heartRateDataset
        )

        // We iterate through Measurements and, for each one, we read HR and Sleep from entities
        val startingDayOfYear = getDatetimeWhenRecordingStarted(dateTimesInDataset).dayOfYear

        if (heartRateDataset.isNotEmpty()) measurements.map { measurement ->
            val measurementDayOfYear = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(measurement.timestamp),
                zoneId
            ).dayOfYear
            val day = measurementDayOfYear - startingDayOfYear  // 0, 1, 2...

            // Interval to search matching heart rate datum = (firstIndex, lastIndex)
            val firstIndex = lastHeartRateDatumIndexForEachDay[day]
            val lastIndex =
                if (day + 1 >= lastHeartRateDatumIndexForEachDay.size) heartRateDataset.lastIndex
                else lastHeartRateDatumIndexForEachDay[day + 1]

            // Binary Search because they are ordered
            val index = heartRateDataset.binarySearch(fromIndex = firstIndex, toIndex = lastIndex) { datum ->
                // Comparator function, returns 0 if values are equal, negative / positive if not
                val hhmmss = datum.time
                val yyyyMMdd = try {
                    dateTimesInDataset[day]
                } catch (e: IndexOutOfBoundsException) {
                    // Fitbit may have not provided the whole list of days properly
                    val dateTime = getDatetimeWhenRecordingStarted(dateTimesInDataset)
                        .plusDays(day.toLong())
                    val formattedDateTime = dateTime.format(yyMMddFormatter)
                    dateTimesInDataset.add(formattedDateTime)
                    formattedDateTime
                }
                val yyyyMMdd_hhmmss = "$yyyyMMdd $hhmmss"
                val localDateTime = LocalDateTime.parse(yyyyMMdd_hhmmss, heartRateFormatter)
                val heartRateTimestamp = localDateTime.atZone(zoneId).toEpochSecond()
                val measurementTimestamp = measurement.timestamp / 1000

                (heartRateTimestamp - measurementTimestamp).toInt()
            }.let {
                getIndexFromBinarySearchResult(it)
            }
            measurement.heartRate = heartRateDataset[index].value
        }
    }

    private fun updateMeasurementsWithSleep(
        measurements: List<Measurement>,
        sleep: FitbitSleepEntity
    ) {
        /** Sleep **/

        val sleepDataset = try {
            sleep.sleepPeriods
                .filter { it.type == SLEEP_TYPE_STAGES }
                .map { it.levels.data }
                .flatten()
                .sortedBy {
                    val localDateTime = LocalDateTime.parse(it.dateTime, sleepFormatter)
                    localDateTime.atZone(zoneId).toEpochSecond()
                }
        } catch (e: Exception) {
            emptyList()
        }

        if (sleepDataset.isNotEmpty()) measurements.map { measurement ->
            val measurementTimestamp = measurement.timestamp / 1000

            val index = sleepDataset.binarySearch { datum ->
                // Notice here the comparator function will rarely return 0 for an exact timestamp
                // match
                val localDateTime = LocalDateTime.parse(datum.dateTime, sleepFormatter)
                val sleepTimestamp = localDateTime.atZone(zoneId).toEpochSecond()
                (sleepTimestamp - measurementTimestamp).toInt()
            }.let {
                getIndexFromBinarySearchResult(it)
            }

            // However, if data was not really collected during this precise moment, we cannot
            // update the measurement...
            val localDateTime = LocalDateTime.parse(sleepDataset[index].dateTime, sleepFormatter)
            val sleepTimestamp = localDateTime.atZone(zoneId).toEpochSecond()

            if (measurementTimestamp >= sleepTimestamp &&
                measurementTimestamp <= (sleepTimestamp + sleepDataset[index].seconds)
            ) {
                measurement.sleepStage = when (sleepDataset[index].level) {
                    "wake" -> SleepStageValues.WAKE
                    "light" -> SleepStageValues.LIGHT
                    "deep" -> SleepStageValues.DEEP
                    "rem" -> SleepStageValues.REM
                    else -> SleepStageValues.UNKNOWN
                }.value
            }
        }
    }

    private fun getDatetimeWhenRecordingStarted(dateTimesInDataset: List<String>): LocalDateTime {
        return LocalDateTime.parse(
            dateTimesInDataset.first() + " 00:00:00",
            heartRateFormatter
        )
    }

    private fun getLastHeartRateDatumIndexForEachDay(
        dateTimesInDataset: MutableList<String>,
        heartRateDataset: List<HeartRateDatum>
    ): List<Int> {
        if (heartRateDataset.isEmpty()) {
            return listOf()
        }
        val lastDatasetIndexForEachDay = mutableListOf<Int>()
        lastDatasetIndexForEachDay.add(0)
        var currentDay = 0
        var currentDate = LocalDateTime.parse(
            "${dateTimesInDataset[currentDay]} 00:00:00",
            heartRateFormatter
        )
        for (i in heartRateDataset.indices) {
            val newDate = LocalDateTime.parse(
                "${dateTimesInDataset[currentDay]} ${heartRateDataset[i].time}",
                heartRateFormatter
            )
            val wasDayIncremented = newDate.hour < currentDate.hour
            if (wasDayIncremented) {
                println("Incrementing day! $currentDay Datetimes: $dateTimesInDataset")
                currentDay++
                lastDatasetIndexForEachDay.add(i)
                if (dateTimesInDataset.size <= currentDay) {
                    // Fitbit may have not provided the whole list of days properly
                    val dateTime = getDatetimeWhenRecordingStarted(dateTimesInDataset)
                        .plusDays(currentDay.toLong())
                    val formattedDateTime = dateTime.format(yyMMddFormatter)
                    dateTimesInDataset.add(formattedDateTime)
                }
                println("Day incremented! Current day: $currentDay Datetimes: $dateTimesInDataset")
            }
            currentDate = newDate
        }
        return lastDatasetIndexForEachDay.toImmutableList()
    }

    private fun getIndexFromBinarySearchResult(searchResult: Int): Int {
        val index: Int

        if (searchResult == -1) {
            // Binary search has NOT found an element matching the exact second.
            // In this case it will then return the inverted insertion point (-insertionPoint - 1).

            // If insertion point is 0 (and thus -insertionPoint - 1 is -1), that means recording
            // started before health data was collected, in which case we will presume the metric
            // (heart rate, sleep, ...) is the same as the first second
            index = 0
        } else if (searchResult < -1) {
            // Binary search has NOT found an element matching the exact second.
            // In this case it will then return the inverted insertion point (-insertionPoint - 1).

            // If insertion point is -1 or lower, that means we have a insertionPoint in positions
            // 1..n of the measurement array. So we compute the insertionPoint back and use it as
            // the index
            index = ((-1) * searchResult - 1) - 1
        } else {
            // Otherwise the binary search result is telling us that it found a register in the same
            // precise second, and it tells us in which index. Easiest case.
            index = searchResult
        }
        return index
    }

    /**
     * THIS METHOD IS NOT SUPPORTED AND ALWAYS THROWS AN EXCEPTION. DO NOT USE IT.
     * @throws [UnsupportedOperationException]
     */
    override fun mapToEntity(
        domainModel: List<Measurement>
    ): Triple<FitbitHeartRateEntity, FitbitSleepEntity, List<Measurement>> {
        throw UnsupportedOperationException()
    }

    companion object {
        const val SLEEP_TYPE_STAGES = "stages"
        const val SLEEP_TYPE_CLASSIC = "classic"
    }

}