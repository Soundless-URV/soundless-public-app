package cat.urv.cloudlab.soundless.util.datastate

sealed class RepositoryDataState<out R> {
    // Main states for Room, Retrofit...
    data class Success<out T>(val data: T): RepositoryDataState<T>()
    data class Error(val error: Exception): RepositoryDataState<Nothing>()
    object Loading: RepositoryDataState<Nothing>()

    // States for other parts of app
    object Recording: RepositoryDataState<Boolean>()
    object NotRecording: RepositoryDataState<Boolean>()
    data class SetSteps(val steps: Int): RepositoryDataState<Int>()
    data class Progress(val num: Int): RepositoryDataState<Int>()

    object NoState: RepositoryDataState<Nothing>()
}