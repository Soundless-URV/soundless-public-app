package cat.urv.cloudlab.soundless.model.repository

import cat.urv.cloudlab.soundless.model.repository.health.HealthDataClient
import cat.urv.cloudlab.soundless.util.datastate.HealthRequestDataState
import cat.urv.cloudlab.soundless.util.datastate.RepositoryDataState
import cat.urv.cloudlab.soundless.viewmodel.Measurement
import cat.urv.cloudlab.soundless.viewmodel.RecordingMetadata
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

class HealthRepository @AssistedInject constructor(
    @Assisted private val healthDataClient: HealthDataClient
) {

    /**
     * This function simply calls the health data client and maps its [HealthRequestDataState] to a
     * [RepositoryDataState]. It does so for an instance of [RecordingMetadata] to restrict calls
     * to a specific recording only (we'll only get Health data corresponding to the period
     * between [RecordingMetadata.timestampStart] and [RecordingMetadata.timestampEnd]).
     *
     * @param metadata The metadata for which we want to get health data.
     */
    suspend fun getHealthDataForMeasurements(measurements: List<Measurement>): Flow<RepositoryDataState<List<Measurement>>> = flow {
        emit(RepositoryDataState.Loading)
        try {
            healthDataClient.getHealthDataForMeasurements(measurements).onEach { state ->
                when (state) {
                    is HealthRequestDataState.HealthDataObtained -> {
                        emit(RepositoryDataState.Success(state.data))
                    }
                    is HealthRequestDataState.HealthDataError -> {
                        emit(RepositoryDataState.Error(state.error))
                    }
                    else -> {
                        // Do nothing
                    }
                }
            }.collect()
        } catch (e: Exception) {
            emit(RepositoryDataState.Error(e))
        }
    }

}