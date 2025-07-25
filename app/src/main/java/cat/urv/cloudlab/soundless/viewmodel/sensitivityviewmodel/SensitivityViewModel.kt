package cat.urv.cloudlab.soundless.viewmodel.sensitivityviewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import cat.urv.cloudlab.soundless.model.repository.MainRepository
import cat.urv.cloudlab.soundless.model.sensitivity.SensitivityData
import cat.urv.cloudlab.soundless.util.datastate.RepositoryDataState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SensitivityViewModel @Inject constructor(
    application: Application,
    private val mainRepository: MainRepository
): AndroidViewModel(application) {

    private val _allRecordingsData = MutableLiveData<List<SensitivityData>>()
    val allRecordingsData: LiveData<List<SensitivityData>> get() = _allRecordingsData

    private val _sensitivityLevels = MutableLiveData<SensitivityResult>()
    val sensitivityLevels: LiveData<SensitivityResult> get() = _sensitivityLevels

    fun extractSensitivity() {
        viewModelScope.launch {
            val allRecordingsData = getMetadata()
            _sensitivityLevels.value = calculateSensitivity(allRecordingsData) as SensitivityResult
        }
    }

    private fun calculateSensitivity(allRecordingsData: List<SensitivityData>): Any {
        val heartDbSensitivityList = mutableListOf<Double>()
        val sleepDbSensitivityList = mutableListOf<Double>()
        val combinedDbSensitivityList = mutableListOf<Double>()

        var numberRecordings = 0
        var numberNightsNoise = 0
        var numberNightsHeart = 0
        var numberNightsSleep = 0

        for (recording in allRecordingsData) {
            // Delete recordings too shorts
            if (recording.isShortRecordings()) {
                println("Recording too short")
                continue
            }

            // Delete initial and last awake data
            recording.deleteAwakeData()

            // Calculate sensitivity for each recording
            val recordingData = calculateRecordingSensitivity(recording)

            heartDbSensitivityList.addAll(recordingData.heartListSensitivity)
            sleepDbSensitivityList.addAll(recordingData.sleepListSensitivity)
            combinedDbSensitivityList.addAll(recordingData.combinedListSensitivity)

            numberRecordings++
            if (recordingData.noiseIncidence) numberNightsNoise++
            if (recordingData.heartIncidence) numberNightsHeart++
            if (recordingData.sleepIncidence) numberNightsSleep++
        }

        return SensitivityResult(
            heartDbSensitivityList.average(),
            sleepDbSensitivityList.average(),
            combinedDbSensitivityList.average(),
            numberRecordings,
            numberNightsNoise,
            numberNightsHeart,
            numberNightsSleep
        )
    }

    private fun calculateRecordingSensitivity(recording: SensitivityData): SensitivityDataRecording {
        // Calculate zScores for dB, heart rate and sleep stage incidences
        val zScoresDb = recording.zScoresDB()
        val zScoresHr = recording.zScoresHR()
        val incidencesSleepStage = recording.incidencesSleep()

        // Get timestamps for each incidence
        val zScoresDbTimestamps = recording.getTimestampsIncidence(zScoresDb["signals"] ?: listOf())
        val zScoresHrTimestamps = recording.getTimestampsIncidence(zScoresHr["signals"] ?: listOf())
        val incidencesSleepStageTimestamps = recording.getTimestampsIncidence(incidencesSleepStage)

        // Match dB timestamps with heart rate and sleep stage timestamps
        val (heartDbIncidences, sleepDbIncidences, combinedDbIncidences) = recording.matchDBIncidencesTimestamps(zScoresDbTimestamps, zScoresHrTimestamps, incidencesSleepStageTimestamps)

        // Calculate sensitivity for this matches incidences
        val heartDbSensitivity = recording.getSensitivity(heartDbIncidences)
        val sleepDbSensitivity = recording.getSensitivity(sleepDbIncidences)
        val combinedDbSensitivity = recording.getSensitivity(combinedDbIncidences)

        return SensitivityDataRecording(
            heartDbSensitivity,
            sleepDbSensitivity,
            combinedDbSensitivity,
            zScoresDbTimestamps.isNotEmpty(),
            heartDbIncidences.isNotEmpty(),
            sleepDbIncidences.isNotEmpty()
        )
    }

    private suspend fun getMetadata() : List<SensitivityData> {
        val allRecordings = mutableListOf<SensitivityData>()
        mainRepository.getMetadata()
            .collect { dataState ->
                when (dataState) {
                    is RepositoryDataState.Success -> {
                        // Filter valid uuids
                        val recordingsMetadata = dataState.data.filter {
                            (!it.currentlyActive && it.isHealthDataAvailable)
                        }

                        for (recording in recordingsMetadata) {
                            val recordingData = getRecordingData(recording.uuid)
                            if (recordingData != null) {
                                allRecordings.add(recordingData)
                            }
                        }
                    }
                    is RepositoryDataState.Error -> {
                        // Handle error here
                    }

                    else -> {
                        // Handle error here
                    }
                }
            }
        _allRecordingsData.value = allRecordings
        return allRecordings
    }

    private suspend fun getRecordingData(uuid: String): SensitivityData? {
        var sensitivityData: SensitivityData? = null
        mainRepository.getMeasurementsForRecording(uuid)
            .collect { dataState ->  // Use collect instead of onEach and launchIn
                when (dataState) {
                    is RepositoryDataState.Success -> {
                        sensitivityData = SensitivityData(
                            dataState.data.map { it.dB.toDouble() },
                            dataState.data.map { it.heartRate.toDouble() },
                            dataState.data.map { it.sleepStage },
                            dataState.data.map { it.timestamp }
                        )
                    }
                    is RepositoryDataState.Error -> {
                        // Handle error here
                    }
                    else -> {
                        // Handle error here
                    }
                }
            }
        return sensitivityData
    }

    data class SensitivityResult(
        val heartSensitivity: Double,
        val sleepSensitivity: Double,
        val combinedSensitivity: Double,
        val numberTotalOfRecordings: Int,
        val numberNightsNoise: Int,
        val numberNightsHeart: Int,
        val numberNightsSleep: Int,
    )

    data class SensitivityDataRecording(
        val heartListSensitivity: List<Double>,
        val sleepListSensitivity: List<Double>,
        val combinedListSensitivity: List<Double>,
        val noiseIncidence: Boolean,
        val heartIncidence: Boolean,
        val sleepIncidence: Boolean,
    )
}