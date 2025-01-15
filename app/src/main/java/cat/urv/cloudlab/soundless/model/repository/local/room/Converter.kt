package cat.urv.cloudlab.soundless.model.repository.local.room

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converter {

    @kotlinx.serialization.ExperimentalSerializationApi
    @TypeConverter
    fun fromListToJSON(list: List<Pair<Int, Int>>): String {
        return Json.encodeToString(list)
    }

    @kotlinx.serialization.ExperimentalSerializationApi
    @TypeConverter
    fun fromJSONToList(json: String): List<Pair<Int, Int>> {
        return Json.decodeFromString(json)
    }
}