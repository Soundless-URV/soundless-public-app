package cat.urv.cloudlab.soundless.util.datastate

sealed class RecordingDataState<out R> {
    data class DBCollected<out T>(val data: T): RecordingDataState<T>()
    data class RecordingMetadataCollected<out T>(val data: T): RecordingDataState<T>()
}