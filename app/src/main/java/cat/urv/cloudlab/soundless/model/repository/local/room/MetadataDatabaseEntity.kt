package cat.urv.cloudlab.soundless.model.repository.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metadata")
class MetadataDatabaseEntity
(
    @field:PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "uuid")
    var uuid: String,

    @ColumnInfo(name = "randomizedLng")
    var randomizedLng: Float,

    @ColumnInfo(name = "randomizedLat")
    var randomizedLat: Float,

    @ColumnInfo(name = "uncertaintyRadius")
    var uncertaintyRadius: Float,

    @ColumnInfo(name = "frequency")
    var frequency: Int,

    @ColumnInfo(name = "currentlyActive")
    var currentlyActive: Boolean,

    @ColumnInfo(name = "timestampStart")
    var timestampStart: Long,

    @ColumnInfo(name = "timestampEnd")
    var timestampEnd: Long,

    @ColumnInfo(name = "isHealthDataAvailable")
    var isHealthDataAvailable: Boolean
)