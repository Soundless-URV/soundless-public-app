package cat.urv.cloudlab.soundless.model.repository.remote.firebase

import cat.urv.cloudlab.soundless.viewmodel.RecordingMetadata
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions

class RecordingHistoryUploaderClient {

    private lateinit var functions: FirebaseFunctions

    fun postHistory(history: List<RecordingMetadata>): Task<Unit> {
        functions = FirebaseFunctions.getInstance(FUNCTION_REGION)

        val data = mapOf<String, List<String>>(
            "history" to history.map { it.uuid }
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
        private const val FUNCTION_NAME = "storeHistory"
        private const val FUNCTION_REGION = "europe-west2"
    }
}