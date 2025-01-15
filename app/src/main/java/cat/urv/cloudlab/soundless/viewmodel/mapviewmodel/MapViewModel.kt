package cat.urv.cloudlab.soundless.viewmodel.mapviewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cat.urv.cloudlab.soundless.util.FileManager
import cat.urv.cloudlab.soundless.util.datastate.MapDataState
import cat.urv.cloudlab.soundless.viewmodel.mapviewmodel.MapViewModel.MapStateEvent.DownloadNewFile
import cat.urv.cloudlab.soundless.viewmodel.mapviewmodel.MapViewModel.MapStateEvent.GetMapFile
import cat.urv.cloudlab.soundless.viewmodel.mapviewmodel.MapViewModel.MapStateEvent.CancelDownload
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2
import com.google.firebase.storage.ktx.storage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject


@HiltViewModel
class MapViewModel @Inject constructor(private val fileManager: FileManager) : ViewModel() {

    private val _fileUpdatesLiveData: MutableLiveData<MapDataState<File>> = MutableLiveData()
    val fileUpdatesLiveData: LiveData<MapDataState<File>>
        get() = _fileUpdatesLiveData

    private val _newFileAvailableLiveData: MutableLiveData<MapDataState<String>> = MutableLiveData()
    val newFileAvailableLiveData: LiveData<MapDataState<String>>
        get() = _newFileAvailableLiveData

    private val storage = Firebase.storage(BUCKET_URL).reference

    fun setStateEvent(mapStateEvent: MapStateEvent) {
        when (mapStateEvent) {
            is GetMapFile -> {
                val mapDate = mapStateEvent.mapDate
                val location = mapStateEvent.location

                val latestLocalFile = fileManager.getLatestFile(mapDate, location)
                if (latestLocalFile != null) {
                    // Notify observers with latest file
                    _fileUpdatesLiveData.postValue(MapDataState.MapUpdated(latestLocalFile))
                } else {
                    _fileUpdatesLiveData.postValue(MapDataState.NoMapAvailable)
                }

                // New files available in bucket? Check it out and notify observers
                findLastFileInBucket(mapDate, location) { latestBucketFileName ->
                    if (latestLocalFile == null) {
                        // If no local files, any new file in bucket is surely the newest
                        _newFileAvailableLiveData.postValue(MapDataState.NewFileAvailable)
                    } else {
                        // Compare the two files, see if the file in bucket is indeed new
                        val latestLocalFileDate = extractDateFromFileName(latestLocalFile.name)
                        val latestBucketFileDate = extractDateFromFileName(latestBucketFileName)
                        if (latestLocalFileDate != null &&
                            latestBucketFileDate != null &&
                            latestLocalFileDate < latestBucketFileDate)
                        {
                            _newFileAvailableLiveData.postValue(MapDataState.NewFileAvailable)
                        }
                    }
                }
            }
            is DownloadNewFile -> {
                val mapDate = mapStateEvent.mapDate
                val location = mapStateEvent.location

                findLastFileInBucket(mapDate, location) { fileName ->
                    val cloudFile = storage.child(fileName)
                    val localDestinationFile = fileManager.allocateDestinationFile(fileName)

                    cloudFile.getFile(localDestinationFile)
                        .addOnSuccessListener {
                            _fileUpdatesLiveData.postValue(
                                MapDataState.MapUpdated(localDestinationFile)
                            )
                            fileManager.deleteOldFiles(mapDate, location)
                        }
                    _newFileAvailableLiveData.postValue(MapDataState.NoState)
                }
            }
            is CancelDownload -> {
                _newFileAvailableLiveData.postValue(MapDataState.NoState)
            }
        }
    }

    private fun extractDateFromFileName(fileName: String): LocalDateTime? {
        val regex = "\\d+-\\d+-\\d+".toRegex()
        val date = fileName.let {
            val dateInFileName = regex.find(it)?.value
            if (dateInFileName == null) return@let null
            val (year, month, day) = dateInFileName.split("-")
            LocalDateTime.of(year.toInt(), month.toInt(), day.toInt(), 0, 0)
        }
        return date
    }

    private fun findLastFileInBucket(
        mapDate: MapDate,
        location: String,
        onSuccessListener: (String) -> Unit
    ) {
        storage.listAll()
            .addOnSuccessListener { (objects, _) ->
                val regex = "${location}_\\d+-\\d+-\\d+".toRegex()
                val fileName = objects
                .filter {
                    it.name.contains(location) &&
                    it.name.contains(mapDate.alias) &&
                    regex.find(it.name)?.value != null
                }
                .map { file -> file.name to extractDateFromFileName(file.name) }
                .filter { (fileName, date) -> date != null }
                .maxByOrNull { (fileName, date) -> date!! }
                ?.first
                if (fileName != null) onSuccessListener(fileName)
            }
    }

    enum class MapDate(val alias: String) {
        MAP_OF_YESTERDAY("yesterday"),
        MAP_OF_LAST_WEEK("week"),
        MAP_OF_LAST_MONTH("month"),
        DEFAULT("yesterday")
    }

    companion object {
        private const val BUCKET_URL = "gs://soundless-maps"
    }

    sealed class MapStateEvent {
        data class GetMapFile(val mapDate: MapDate, val location: String): MapStateEvent()
        data class DownloadNewFile(val mapDate: MapDate, val location: String): MapStateEvent()
        data class CancelDownload(val cancel: Boolean): MapStateEvent()
    }
}