package cat.urv.cloudlab.soundless.util

import android.content.Context
import cat.urv.cloudlab.soundless.viewmodel.mapviewmodel.MapViewModel.MapDate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject

class FileManager @Inject constructor(val applicationContext: Context) {

    private val directory: File by lazy {
        applicationContext.filesDir
    }

    /**
     * @return true if all have been deleted, false if not
     */
    fun deleteOldFiles(mapDate: MapDate, location: String): Boolean {
        val fileToKeep = getLatestFileName(mapDate, location)
        return directory.listFiles()!!
            .filter {
                it.name.contains(mapDate.alias) &&
                it.name.contains(location) &&
                it.name != fileToKeep
            }
            .map { file -> file.delete() }
            .all { it }
    }

    fun getLatestFile(mapDate: MapDate, location: String): File? {
        val fileName = getLatestFileName(mapDate, location)
        if (fileName != null) {
            return getFile(fileName)
        }
        return null
    }

    private fun getFile(fileName: String): File {
        return File(directory, fileName)
    }

    private fun getLatestFileName(mapDate: MapDate, location: String): String? {
        val regex = "${location}_\\d+-\\d+-\\d+".toRegex()
        val onlyDateRegex = "\\d+-\\d+-\\d+".toRegex()
        return directory.listFiles()!!
            .filter {
                it.name.contains(location) &&
                it.name.contains(mapDate.alias) &&
                regex.find(it.name)?.value != null
            }
            .map { file -> file.name }
            .maxByOrNull { fileName ->
                val dateInFileName = onlyDateRegex.find(fileName)!!.value
                val (year, month, day) = dateInFileName.split("-")
                LocalDateTime.of(year.toInt(), month.toInt(), day.toInt(), 0, 0)
            }
    }

    fun allocateDestinationFile(fileName: String): File {
        return File(directory, fileName)
    }

}