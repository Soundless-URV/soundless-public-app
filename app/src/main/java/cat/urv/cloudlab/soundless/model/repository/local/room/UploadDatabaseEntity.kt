package cat.urv.cloudlab.soundless.model.repository.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "uploads",
    foreignKeys = [
        ForeignKey(
            entity = MetadataDatabaseEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["uuid"],
            onDelete = ForeignKey.CASCADE
        ),
    ])
class UploadDatabaseEntity
(
    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @ColumnInfo(name = "uuid")
    var uuid: String,

    @ColumnInfo(name = "timestamp")
    var timestamp: Long
) {
    constructor(uuid: String, timestamp: Long) : this(0, uuid, timestamp)
}