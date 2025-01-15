package cat.urv.cloudlab.soundless.util.datastate

sealed class MapDataState<out R> {
    data class MapUpdated<out T>(val file: T): MapDataState<T>()
    object NoMapAvailable: MapDataState<Nothing>()
    object NewFileAvailable: MapDataState<Nothing>()
    object NoState: MapDataState<Nothing>()
}
