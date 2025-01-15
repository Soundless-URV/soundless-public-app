package cat.urv.cloudlab.soundless.model.repository.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import cat.urv.cloudlab.soundless.viewmodel.Timestamp

/**
 * This class determines which fields can be updated in the SQLite Metadata table in order to modify
 * just these fields, instead of the whole row.
 *
 * TODO split into different updaters
 *
 * @see DatabaseDao.updateMetadata
 */
@Entity
class MetadataUpdater (
    @ColumnInfo(name = "uuid")
    var uuid: String,

    @ColumnInfo(name = "currentlyActive")
    var currentlyActive: Boolean,

    @ColumnInfo(name = "timestampEnd")
    var timestampEnd: Long,

    @ColumnInfo(name = "isHealthDataAvailable")
    var isHealthDataAvailable: Boolean
)