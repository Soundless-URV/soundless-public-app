package cat.urv.cloudlab.soundless.model.repository.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.util.TableInfo

@Entity(
    tableName = "measurements",
    primaryKeys = ["timestamp"],
    foreignKeys = [
        ForeignKey(
            entity = MetadataDatabaseEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["uuid"],
            onDelete = ForeignKey.CASCADE
        ),
    ])
class MeasurementDatabaseEntity
(
    @ColumnInfo(name = "uuid")
    var uuid: String,

    @ColumnInfo(name = "timestamp")
    var timestamp: Long,

    @ColumnInfo(name = "dB")
    var dB: Float,

    @ColumnInfo(name = "heartRate")
    var heartRate: Int,

    @ColumnInfo(name = "sleepStage")
    var sleepStage: Int
)