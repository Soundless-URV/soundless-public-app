package cat.urv.cloudlab.soundless.model.repository.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import cat.urv.cloudlab.soundless.viewmodel.Timestamp

/**
 * This class determines which fields can be updated in the SQLite Measurement table in order to
 * modify just these fields, instead of the whole row.
 *
 * @see DatabaseDao.updateMeasurement
 */
@Entity
class MeasurementUpdater (
    @ColumnInfo(name = "uuid")
    var uuid: String,

    @ColumnInfo(name = "timestamp")
    var timestamp: Timestamp,

    @ColumnInfo(name = "heartRate")
    var heartRate: Int,

    @ColumnInfo(name = "sleepStage")
    var sleepStage: Int
)