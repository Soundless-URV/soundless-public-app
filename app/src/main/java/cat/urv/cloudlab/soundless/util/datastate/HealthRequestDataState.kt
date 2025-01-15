package cat.urv.cloudlab.soundless.util.datastate

sealed class HealthRequestDataState<out R> {
    object HeartRateRequested: HealthRequestDataState<Nothing>()
    data class HeartRateObtained<out T>(val data: T): HealthRequestDataState<T>()
    data class HeartRateError(val error: Exception): HealthRequestDataState<Nothing>()

    object SleepRequested: HealthRequestDataState<Nothing>()
    data class SleepObtained<out T>(val data: T): HealthRequestDataState<T>()
    data class SleepError(val error: Exception): HealthRequestDataState<Nothing>()

    object HealthDataRequested: HealthRequestDataState<Nothing>()
    data class HealthDataObtained<out T>(val data: T): HealthRequestDataState<T>()
    data class HealthDataError(val error: Exception): HealthRequestDataState<Nothing>()
}
