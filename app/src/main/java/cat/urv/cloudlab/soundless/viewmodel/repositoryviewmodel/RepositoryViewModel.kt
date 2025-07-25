package cat.urv.cloudlab.soundless.viewmodel.repositoryviewmodel


import android.app.Application
import androidx.lifecycle.*
import cat.urv.cloudlab.soundless.model.repository.HealthRepository
import cat.urv.cloudlab.soundless.model.repository.MainRepository
import cat.urv.cloudlab.soundless.model.repository.MetadataUpdateType
import cat.urv.cloudlab.soundless.model.repository.health.fitbit.FitbitClient
import cat.urv.cloudlab.soundless.model.repository.remote.firebase.RecordingHistoryUploaderClient
import cat.urv.cloudlab.soundless.util.DeviceLocation
import cat.urv.cloudlab.soundless.util.datastate.RepositoryDataState
import cat.urv.cloudlab.soundless.util.di.HealthRepositoryAssistedFactory
import cat.urv.cloudlab.soundless.viewmodel.Measurement
import cat.urv.cloudlab.soundless.viewmodel.RecordingMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject


/**
 * Manages all communication between the data and the UI via [LiveData] objects.
 *
 * Uses [RepositoryDataState] events to receive and send status updates on the data.
 *
 * This ViewModel extends [AndroidViewModel] so that we can use context in it without memory leaks.
 */
@HiltViewModel
class RepositoryViewModel @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val deviceLocation: DeviceLocation,
    private val mainRepository: MainRepository,
    private val healthRepositoryFactory: HealthRepositoryAssistedFactory
): AndroidViewModel(application) {

    /**
     * Recording variable - used to know whether any recording is active.
     * We are using a SavedStateHandle variable to be able to preserve state upon certain events.
     * SavedStateHandle is an Android Jetpack dictionary-like variable to store small state chunks.
     */
    private var recording: Boolean
        get() = savedStateHandle.get<Boolean>("recording") ?: false
        set(value) {
            savedStateHandle.set("recording", value)
            println("RECORDING SET TO: $recording")
        }

    /**
     * Data state for metadata from repositories.
     */
    private val _repositoryMetadataLiveData:
        MutableLiveData<RepositoryDataState<List<RecordingMetadata>>> = MutableLiveData()
    val repositoryMetadataLiveData: LiveData<RepositoryDataState<List<RecordingMetadata>>>
        get() = _repositoryMetadataLiveData

    /**
     * Boolean Live Data to ask questions about state to ViewModel (e.g. is the app recording?)
     */
    private val _booleanLiveData: MutableLiveData<RepositoryDataState<Boolean>> = MutableLiveData()
    val booleanLiveData: LiveData<RepositoryDataState<Boolean>>
        get() = _booleanLiveData

    /**
     * Int Live Data to ask questions that can be answered via an amount, mostly to represent
     * development in a long-running process.
     */
    private val _progressLiveData: MutableLiveData<RepositoryDataState<Int>> = MutableLiveData()
    val progressLiveData: LiveData<RepositoryDataState<Int>>
        get() = _progressLiveData

    private var counter: Int = 0

    /**
     * Manage INCOMING state changes. For example, if recording button is clicked on the UI, it will
     * report a state change here (mainStateEvent parameter). All the logic will then be run here.
     */
    fun setStateEvent(repositoryStateEvent: RepositoryStateEvent) {
        // Resilience to events re-creating Activity
        if (repositoryStateEvent is RepositoryStateEvent.StartCollectingEvent && recording) {
            return
        }

        viewModelScope.launch {
            when (repositoryStateEvent) {
                is RepositoryStateEvent.StartCollectingEvent -> {
                    deviceLocation.update()
                    recording = true
                }
                is RepositoryStateEvent.StopCollectingEvent -> {
                    recording = false
                }
                is RepositoryStateEvent.CancelCollectingEvent -> {
                    recording = false
                }
                is RepositoryStateEvent.IsRecordingEvent -> {
                    deviceLocation.update()
                    isRecording()
                        .onEach { dataState ->
                            _booleanLiveData.value = dataState
                        }
                        .launchIn(viewModelScope)
                }
                is RepositoryStateEvent.ReceivedUnexpectedMeasurement -> {
                    recording = true
                }
                is RepositoryStateEvent.GetMetadataEvent -> {
                    mainRepository.getMetadata()
                        .onEach { dataState ->
                            // First notify observers
                            _repositoryMetadataLiveData.value = dataState

                            // Then (asynchronously), reassure that there are no malformed / wrong
                            // metadata objects with recording active. This is a 'just-in-case'
                            // thing. We do it here to take advantage of the fact we already read
                            // data from DB
                            if (dataState is RepositoryDataState.Success) {
                                // Filter malformed
                                dataState.data.filter {
                                    // This is not an active recording!
                                    (it.currentlyActive && !recording)

                                    // ...(Should account for more conditions)...

                                // Update each malformed
                                }.forEach {
                                    mainRepository.updateMetadata(
                                        it, MetadataUpdateType.SET_RECORDING_NOT_ACTIVE,
                                        false  // This value is irrelevant here
                                    ).collect()
                                }
                            }
                        }
                        .launchIn(viewModelScope)
                }
                is RepositoryStateEvent.GetHealthDataEvent -> {
                    // Create a health repository to poll health data
                    val healthRepository =
                        healthRepositoryFactory.create(repositoryStateEvent.fitbitClient)

                    // We read all Metadata objects we have available
                    mainRepository.getMetadata()
                        .onEach { dataState ->
                            when (dataState) {
                                is RepositoryDataState.Success -> {
                                    val numRecordings = dataState.data.size
                                    if (numRecordings > 0) {
                                        // We decrement from size to 0 so that observers know how many
                                        // are remaining
                                        _progressLiveData.value = RepositoryDataState.SetSteps(
                                            dataState.data.size
                                        )
                                        counter = dataState.data.size

                                        dataState.data.forEach { recording ->
                                            println(recording)
                                            // If we have not polled health data for this recording,
                                            // then do so:
                                            if (!recording.isHealthDataAvailable) {
                                                try {
                                                    pollHealthMetricsForRecording(
                                                        healthRepository,
                                                        recording,
                                                        repositoryStateEvent.participateInStudy
                                                    )
                                                } catch (e: HealthRequestTimeoutException) {
                                                    // Process was lasting too much and was killed.
                                                } finally {
                                                    // The import process has to finish even if errors happen.
                                                    decrementCounterAndNotifyProgress()
                                                }
                                            } else {
                                                decrementCounterAndNotifyProgress()
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    // Do nothing
                                }
                            }
                        }.collect()
                }
                is RepositoryStateEvent.DeleteLocalMetadataEvent -> {
                    mainRepository.deleteAllMetadata().launchIn(viewModelScope)
                }
                is RepositoryStateEvent.DeleteOneRecordingEvent -> {
                    mainRepository.deleteMetadata(repositoryStateEvent.uuid).launchIn(viewModelScope)
                }
                is RepositoryStateEvent.UploadMissingRecordings -> {
                    mainRepository.getUploads().onEach { uploadDataState ->
                        if (uploadDataState is RepositoryDataState.Success) {
                            val successfulUploads = uploadDataState.data
                            println("successfulUploads $successfulUploads")
                            val successfulUuids = successfulUploads.map { it.uuid }
                            mainRepository
                                .getMetadataExceptUuidList(successfulUuids)
                                .onEach { metadataDataState ->
                                    if (metadataDataState is RepositoryDataState.Success) {
                                        val missingMetadata = metadataDataState.data.filter {
                                            !it.currentlyActive
                                        }
                                        println("missingMetadata $missingMetadata")
                                        missingMetadata.forEach {
                                            mainRepository.updateMetadata(
                                                it,
                                                MetadataUpdateType.UPLOAD_RECORDING_ONLY,
                                                true // This event is only handled if participateInStudy is true
                                            ).collect()
                                        }
                                    }
                                }.collect()
                        }
                    }.collect()
                }
                is RepositoryStateEvent.UploadHistory -> {
                    val client = RecordingHistoryUploaderClient()
                    mainRepository.getMetadata().onEach { state ->
                        if (state is RepositoryDataState.Success) {
                            val history = state.data
                            client.postHistory(history)
                        }
                    }.collect()
                }
                is RepositoryStateEvent.None -> {
                    // Do nothing
                }
            }
        }
    }

    /**
     * Given a [RecordingMetadata]:
     * 1. Look up in the repository to find all measurements corresponding to it
     * 2. If we successfully get them, then retrieve health data from API
     * 3. Then update measurements using [mainRepository] and mark [recording] with
     * [RecordingMetadata.isHealthDataAvailable] = true
     *
     * @throws HealthRequestTimeoutException if the request was killed because it was lasting for too long
     */
    private suspend fun pollHealthMetricsForRecording(
        healthRepository: HealthRepository,
        recording: RecordingMetadata,
        participateInStudy: Boolean
    ) {
        withTimeoutOrNull(HEALTH_IMPORT_TIMEOUT_MILLIS) {
            mainRepository.getMeasurementsForRecording(recording).onEach { state ->
                when (state) {
                    is RepositoryDataState.Success -> {
                        val measurementList = state.data
                        healthRepository.getHealthDataForMeasurements(measurementList)
                            .onEach { healthState ->
                                when (healthState) {
                                    // We got new health data that could be added to these measurements
                                    is RepositoryDataState.Success -> {
                                        println("getHealthDataForMeasurements: Success. ${healthState.data}")
                                        viewModelScope.launch {
                                            handleIncomingHealthData(
                                                healthState.data,
                                                recording,
                                                participateInStudy
                                            )
                                        }
                                    }
                                    is RepositoryDataState.Error -> {
                                        println("getHealthDataForMeasurements: Error. ${healthState.error}")
                                    }
                                    is RepositoryDataState.Loading -> {
                                        println("getHealthDataForMeasurements: Loading")
                                    }
                                    else -> {}
                                }
                            }.collect()
                    }
                    is RepositoryDataState.Error -> {
                        println("getMeasurementsForRecording: Error. ${state.error}")
                    }
                    is RepositoryDataState.Loading -> {
                        println("getMeasurementsForRecording: Loading")
                    }
                    else -> {}
                }
            }.collect()
        } ?: throw HealthRequestTimeoutException()
    }

    class HealthRequestTimeoutException: Exception()

    private suspend fun handleIncomingHealthData(
        data: List<Measurement>,
        recording: RecordingMetadata,
        participateInStudy: Boolean
    ) {
        if (data.all { it.heartRate == -1 }) {
            return
        }
        recording.isHealthDataAvailable = true
        mainRepository
            .updateMeasurements(data)
            .collect()
        mainRepository
            .updateMetadata(
                recording,
                MetadataUpdateType.SET_HEALTH_AVAILABLE,
                participateInStudy
            )
            .collect()
    }

    private fun decrementCounterAndNotifyProgress() {
        counter--
        _progressLiveData.let {
            it.value = RepositoryDataState.Progress(counter)
            if (counter == 0) {
                it.value = RepositoryDataState.Success(0)
                it.value = RepositoryDataState.NoState
            }
        }
    }

    /**
     * Determines if recording is currently active. Always emits a response.
     */
    private fun isRecording() = flow {
        if (recording) emit(RepositoryDataState.Recording)
        else emit(RepositoryDataState.NotRecording)
    }

    companion object {
        private const val HEALTH_IMPORT_TIMEOUT_MILLIS = 5000L
    }
}

/**
 * Defines all possible states coming from the UI to the ViewModel. MVI feature.
 */
sealed class RepositoryStateEvent {
    object StartCollectingEvent: RepositoryStateEvent()
    object StopCollectingEvent: RepositoryStateEvent()
    object CancelCollectingEvent: RepositoryStateEvent()
    object IsRecordingEvent: RepositoryStateEvent()
    object GetMetadataEvent: RepositoryStateEvent()
    data class GetHealthDataEvent(
        val fitbitClient: FitbitClient,
        val participateInStudy: Boolean): RepositoryStateEvent()

    data class DeleteOneRecordingEvent(val uuid: String): RepositoryStateEvent()

    object DeleteLocalMetadataEvent: RepositoryStateEvent()
    object None: RepositoryStateEvent()
    object UploadMissingRecordings : RepositoryStateEvent()
    object ReceivedUnexpectedMeasurement : RepositoryStateEvent()
    object UploadHistory : RepositoryStateEvent()
}