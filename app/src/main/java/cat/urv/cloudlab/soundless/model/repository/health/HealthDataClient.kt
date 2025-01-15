package cat.urv.cloudlab.soundless.model.repository.health

import cat.urv.cloudlab.soundless.util.datastate.HealthRequestDataState
import cat.urv.cloudlab.soundless.viewmodel.Measurement
import cat.urv.cloudlab.soundless.viewmodel.Timestamp
import kotlinx.coroutines.flow.Flow

interface HealthDataClient {

    suspend fun getHealthDataForMeasurements(measurements: List<Measurement>): Flow<HealthRequestDataState<List<Measurement>>>

}