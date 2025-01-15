package cat.urv.cloudlab.soundless.model.repository.remote.firebase

import cat.urv.cloudlab.soundless.viewmodel.Measurement
import cat.urv.cloudlab.soundless.viewmodel.RecordingMetadata
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import kotlin.math.roundToInt

class FirebaseFunctionAPIClient {

    private lateinit var functions: FirebaseFunctions

    fun postRecording(metadata: RecordingMetadata, measurements: List<Measurement>): Task<Unit> {
        functions = FirebaseFunctions.getInstance(FUNCTION_REGION)

        val data = mapOf<String, Any>(
            "uuid" to metadata.uuid,
            "lng" to metadata.randomizedLng,
            "lat" to metadata.randomizedLat,
            "measurements" to measurements.map {
                mapOf<String, Any>(
                    "timestamp" to it.timestamp,
                    "dB" to it.dB.roundToInt(),
                    "heartRate" to it.heartRate,
                    "sleepStage" to it.sleepStage
                )
            }
        )
        return functions
            .getHttpsCallable(FUNCTION_NAME)
            .call(data)
            .continueWith { task ->
                val result = task.result?.data
                println("Result: $result")
            }
    }

    companion object {
        private const val FUNCTION_NAME = "storeRecording"
        private const val FUNCTION_REGION = "europe-west2"
    }
}
