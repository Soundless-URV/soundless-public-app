package cat.urv.cloudlab.soundless.model.repository.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cat.urv.cloudlab.soundless.viewmodel.Measurement

@Database(entities = [
    MetadataDatabaseEntity::class,
    MeasurementDatabaseEntity::class,
    UploadDatabaseEntity::class
], version = 8)
//@TypeConverters(Converter::class)
abstract class Database: RoomDatabase() {
    abstract fun databaseDao(): DatabaseDao

    companion object {
        const val DATABASE_NAME: String = "recording_db"
    }
}