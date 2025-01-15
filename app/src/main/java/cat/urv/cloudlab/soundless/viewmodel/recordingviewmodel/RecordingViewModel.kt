package cat.urv.cloudlab.soundless.viewmodel.recordingviewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cat.urv.cloudlab.soundless.model.repository.MainRepository
import cat.urv.cloudlab.soundless.model.repository.MetadataUpdateType
import cat.urv.cloudlab.soundless.util.DeviceLocation
import cat.urv.cloudlab.soundless.util.datastate.RecordingDataState
import cat.urv.cloudlab.soundless.util.datastate.RepositoryDataState
import cat.urv.cloudlab.soundless.viewmodel.Measurement
import cat.urv.cloudlab.soundless.viewmodel.RecordingMetadata
import cat.urv.cloudlab.soundless.viewmodel.Upload
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import java.lang.Integer.min
import java.util.*
import kotlin.math.roundToInt


class RecordingViewModel constructor(
    val deviceLocation: DeviceLocation,
    val mainRepository: MainRepository
): ViewModel() {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _measurementLiveData: MutableLiveData<RecordingDataState<Measurement>> = MutableLiveData()
    val measurementLiveData: LiveData<RecordingDataState<Measurement>>
        get() = _measurementLiveData

    private val queue: Deque<Measurement> = ArrayDeque()

    /**
     * Every X seconds, we take [Measurement]s, summarize them altogether, and save the summary
     * only, so as to save space.
     *
     * This variable has the amount of [Measurement]s that we are taking for the summary.
     *
     * At first, we summarize [Measurement]s faster (and thus save more values to DB). Then, we
     * summarize slower. This is done to avoid punishing short recordings too much.
     */
    private val howManyMeasurementsAreSummarized = listOf(1, 2, 4, 8, 10)

    /**
     * Index for [howManyMeasurementsAreSummarized]. Incremented every time the queue is summarized.
     */
    private var howManyMeasurementsIndex = 0

    /**
     * Read 'summarizing right now'.
     */
    private var summarizingQueue = false

    // TODO when recording is stopped, queue must be cleared
    fun setStateEvent(recordingStateEvent: RecordingStateEvent) {
        when (recordingStateEvent) {
            is RecordingStateEvent.DBEvent -> run {
                val lastMeasurement = recordingStateEvent.measurement

                // Post to live data so that UI can be updated
                _measurementLiveData.postValue(RecordingDataState.DBCollected(lastMeasurement))

                // And then stage value in queue to be processed and saved to repository
                // Notice we only store the average of X values
                val numMeasurements = howManyMeasurementsAreSummarized[howManyMeasurementsIndex]
                if (!summarizingQueue && queue.size == numMeasurements) {
                    popFromQueueAndSave(numMeasurements)

                    // Increment index, do not surpass list size
                    howManyMeasurementsIndex = min(
                        howManyMeasurementsIndex + 1,
                        howManyMeasurementsAreSummarized.size - 1
                    )
                } else {
                    queue.addLast(lastMeasurement)
                }
            }
            is RecordingStateEvent.SaveRemainingData -> run {
                if (queue.size > 3 && !summarizingQueue) {
                    popFromQueueAndSave(numMeasurements = queue.size)
                }
            }
            is RecordingStateEvent.MetadataCreateEvent -> run {
                repositoryScope.launch {
                    mainRepository.postMetadata(recordingStateEvent.recordingMetadata).collect()
                }
            }
            is RecordingStateEvent.MetadataUpdateEvent -> run {
                // If not UPDATE_TIMESTAMP (which would trigger way too many times), try to update
                // location if needed, i.e. if current location is invalid
                if (recordingStateEvent.updateType != MetadataUpdateType.UPDATE_TIMESTAMP) {
                    val (lng, lat) = Pair(
                        recordingStateEvent.recordingMetadata.randomizedLng,
                        recordingStateEvent.recordingMetadata.randomizedLat
                    )
                    if (lng.isNaN() || lng.isInfinite() || lat.isNaN() || lat.isInfinite() ||
                        lng > 180.0f || lng < -180.0f || lat > 90.0f || lat < -90.0f)
                    {
                        recordingStateEvent.recordingMetadata.randomizedLng = deviceLocation.lng
                        recordingStateEvent.recordingMetadata.randomizedLat = deviceLocation.lat
                    }
                }
                repositoryScope.launch {
                    mainRepository.updateMetadata(
                        recordingStateEvent.recordingMetadata,
                        recordingStateEvent.updateType,
                        recordingStateEvent.flagParticipateInStudy
                    ).collect()
                }
            }
        }
    }

    /**
     * Take the first [numMeasurements] values from the queue, summarize them in a single
     * [Measurement], and then send the summary to the repository.
     *
     * This method can be run even if other elements are getting inserted to the queue meanwhile.
     */
    private fun popFromQueueAndSave(numMeasurements: Int) {
        if (numMeasurements < 1) return

        summarizingQueue = true

        repositoryScope.launch {
            // TODO Average Heart Rate and most frequent Sleep stage are useless as of now. Remove?
            var avgDB = 0.0F
            var avgHeartRate = 0.0F
            val sleepStages = mutableMapOf<Int, Int>()

            val uuid = queue.first.uuid
            var timestamp: Long = -1

            // No problem if new values are added to the queue while this is happening
            repeat(numMeasurements) {
                queue.removeFirst().let {
                    avgDB += it.dB
                    avgHeartRate += it.heartRate
                    sleepStages.merge(it.sleepStage, 1, Int::plus)
                    timestamp = it.timestamp
                }
            }

            summarizingQueue = false

            avgDB /= numMeasurements
            avgHeartRate /= numMeasurements
            val mostFrequentSleepStage = sleepStages.maxByOrNull {
                it.value
            }?.key ?: -1

            val avgMeasurement = Measurement(
                uuid = uuid,
                timestamp = timestamp,
                dB = avgDB,
                heartRate = avgHeartRate.roundToInt(),
                sleepStage = mostFrequentSleepStage
            )
            println(avgMeasurement)
            mainRepository.postMeasurement(avgMeasurement).collect()
        }
    }

    sealed class RecordingStateEvent {
        data class DBEvent(val measurement: Measurement): RecordingStateEvent()
        data class MetadataCreateEvent(
            val recordingMetadata: RecordingMetadata,
            val flagParticipateInStudy: Boolean
        ): RecordingStateEvent()
        data class MetadataUpdateEvent(
            val recordingMetadata: RecordingMetadata,
            val updateType: MetadataUpdateType,
            val flagParticipateInStudy: Boolean
        ): RecordingStateEvent()
        object SaveRemainingData: RecordingStateEvent()
    }

}
