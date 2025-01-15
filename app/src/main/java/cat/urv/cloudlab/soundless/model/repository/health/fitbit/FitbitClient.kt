package cat.urv.cloudlab.soundless.model.repository.health.fitbit

import cat.urv.cloudlab.soundless.model.repository.health.HealthDataClient
import cat.urv.cloudlab.soundless.util.auth.FitbitAuthenticator
import cat.urv.cloudlab.soundless.util.datastate.HealthRequestDataState
import cat.urv.cloudlab.soundless.viewmodel.Measurement
import cat.urv.cloudlab.soundless.viewmodel.Timestamp
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A [HealthDataClient] that authenticates to Fitbit, making it able to request data from Fitbit's
 * Web API.
 *
 * To create a FitbitClient, you need a Fitbit user ID (identifies a user in Fitbit's Web API) and
 * a valid, current Fitbit Access Token (carries user's permissions). As in the version this was
 * written, you can obtain both items from a [FitbitAuthenticator] instance, which securely manages
 * these and other features.
 */
class FitbitClient(
    private val fitbitUserID: String,
    private val fitbitAccessToken: String
) : HealthDataClient {

    private val fitbitAPI: FitbitHealthAPI by lazy {
        val json = Json { ignoreUnknownKeys = true }  // Only take the JSON keys we want
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(FitbitAuthenticator.FITBIT_API_URL)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(FitbitHealthAPI::class.java)
    }

    /**
     * Calls Fitbit API and gets data corresponding to a list of measurements. Updates list with the
     * data from API and then returns the list.
     *
     * If the Fitbit API does not return any data, the flow emits a [NoHealthDataFromFitbit] error.
     *
     * @param measurements Initial list of [Measurement]s containing no health data
     * @return Updated list of [Measurement]s with health data
     */
    override suspend fun getHealthDataForMeasurements(
        measurements: List<Measurement>
    ): Flow<HealthRequestDataState<List<Measurement>>> = flow {

        emit(HealthRequestDataState.HealthDataRequested)

        var heartRate: FitbitHeartRateEntity? = null
        var sleep: FitbitSleepEntity? = null

        // Supposedly these will come ordered, but there is no certainty. Find if we could
        // remove loop and get measurements.first.timestamp / measurements.last.timestamp.
        var (start, end) = Pair<Timestamp, Timestamp>(System.currentTimeMillis(), 0)
        measurements.forEach {
            if (start >= it.timestamp) start = it.timestamp
            if (end <= it.timestamp) end = it.timestamp
        }

        // getHeartRate() and getSleep() will be launched to run in parallel. Whenever the two
        // are finished we will update the measurements.
        getHeartRate(start, end).onEach { state ->
            println("Heart Rate. State: $state")
            when (state) {
                is HealthRequestDataState.HeartRateError -> {
                    println("Heart Rate. Error: ${state.error}")
                    emit(HealthRequestDataState.HealthDataError(state.error))
                }
                is HealthRequestDataState.HeartRateObtained -> {
                    println("Heart Rate. Data: ${state.data}")
                    heartRate = state.data
                    if (heartRate != null && sleep != null) {
                        // Avoid concurrency problems from .getSleep()
                        val heartRateDuplicate = heartRate!!
                        heartRate = null
                        val sleepDuplicate = sleep!!
                        sleep = null

                        val isDataAvailable =
                            heartRateDuplicate.heartRateData.dataset.isNotEmpty() ||
                            sleepDuplicate.sleepPeriods.isNotEmpty()

                        if (isDataAvailable) {
                            println("Gonna merge Heart Rate and Sleep data...")
                            val merger = FitbitMapper()
                            val measurementList = merger.mapFromEntity(
                                Triple(heartRateDuplicate, sleepDuplicate, measurements)
                            )
                            println("Done merging Heart Rate and Sleep data.")
                            emit(HealthRequestDataState.HealthDataObtained(measurementList))
                        } else {
                            // User was not wearing the device or it did not produce any data
                            emit(HealthRequestDataState.HealthDataError(NoHealthDataFromFitbit()))
                        }
                    }
                }
                else -> {
                    // Do nothing
                }
            }
        }.collect()

        getSleep(start, end).onEach { state ->
            println("Sleep. State: $state")
            when (state) {
                is HealthRequestDataState.SleepError -> {
                    println("Sleep. Error: ${state.error}")
                    emit(HealthRequestDataState.HealthDataError(state.error))
                }
                is HealthRequestDataState.SleepObtained -> {
                    println("Sleep. Data: ${state.data}")
                    sleep = state.data
                    if (heartRate != null && sleep != null) {
                        // Avoid concurrency problems from .getHeartRate()
                        val heartRateDuplicate = heartRate!!
                        heartRate = null
                        val sleepDuplicate = sleep!!
                        sleep = null

                        val isDataAvailable =
                            heartRateDuplicate.heartRateData.dataset.isNotEmpty() ||
                            sleepDuplicate.sleepPeriods.isNotEmpty()

                        if (isDataAvailable) {
                            println("Gonna merge Heart Rate and Sleep data...")
                            val merger = FitbitMapper()
                            val measurementList = merger.mapFromEntity(
                                Triple(heartRateDuplicate, sleepDuplicate, measurements)
                            )
                            println("Done merging Heart Rate and Sleep data.")
                            emit(HealthRequestDataState.HealthDataObtained(measurementList))
                        } else {
                            // User was not wearing the device or it did not produce any data
                            emit(HealthRequestDataState.HealthDataError(NoHealthDataFromFitbit()))
                        }
                    }
                }
                else -> {
                    // Do nothing
                }
            }
        }.collect()
    }

    class NoHealthDataFromFitbit : Exception()

    /**
     * Get Heart rate from Fitbit API for a period that might span multiple days.
     *
     * Intraday granularity: 1 second
     *
     * @see FitbitHealthAPI.getHeartRate
     * @param start Start timestamp in milliseconds
     * @param end End timestamp in milliseconds
     */
    private fun getHeartRate(
        start: Timestamp,
        end: Timestamp
    ): Flow<HealthRequestDataState<FitbitHeartRateEntity>> = flow {
        try {
            val yearMonthDayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val hourMinFormatter = DateTimeFormatter.ofPattern("HH:mm")  // 24-hour clock (HH)

            val startDate =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault())
            val endDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault())

            val startHourMin = startDate.format(hourMinFormatter)
            val endHourMin = endDate.format(hourMinFormatter)

            val startYearMonthDay = startDate.format(yearMonthDayFormatter)
            val endYearMonthDay = endDate.format(yearMonthDayFormatter)

            val heartRate: FitbitHeartRateEntity = fitbitAPI.getHeartRate(
                fitbitUserID,
                startYearMonthDay,
                endYearMonthDay,
                "1sec",
                startHourMin,
                endHourMin,
                "Bearer $fitbitAccessToken"
            )
            emit(HealthRequestDataState.HeartRateObtained(heartRate))
        } catch (e: Exception) {
            emit(HealthRequestDataState.HeartRateError(e))
        }
    }

    /**
     * Get Sleep from Fitbit API for a period that might span multiple days.
     *
     * @see FitbitHealthAPI.getSleep
     * @param start Start timestamp in milliseconds.
     * @param start End timestamp in milliseconds.
     */
    private fun getSleep(
        start: Timestamp,
        end: Timestamp
    ): Flow<HealthRequestDataState<FitbitSleepEntity>> = flow {
        try {
            val yearMonthDayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val hourMinFormatter = DateTimeFormatter.ofPattern("HH:mm")  // 24-hour clock (HH)

            val startDate =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault())
            val endDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault())

            val startYearMonthDay = startDate.format(yearMonthDayFormatter)
            val endYearMonthDay = endDate.format(yearMonthDayFormatter)

            val heartRate: FitbitSleepEntity = fitbitAPI.getSleep(
                fitbitUserID,
                startYearMonthDay,
                endYearMonthDay,
                "Bearer $fitbitAccessToken"
            )
            emit(HealthRequestDataState.SleepObtained(heartRate))
        } catch (e: Exception) {
            emit(HealthRequestDataState.SleepError(e))
        }
    }

}