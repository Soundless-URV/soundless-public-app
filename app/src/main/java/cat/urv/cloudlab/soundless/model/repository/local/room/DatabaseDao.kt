package cat.urv.cloudlab.soundless.model.repository.local.room

import androidx.room.*

@Dao
interface DatabaseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMetadata(metadataDatabaseEntity: MetadataDatabaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMetadataList(metadataDatabaseEntity: List<MetadataDatabaseEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMeasurement(measurementDatabaseEntityList: MeasurementDatabaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMeasurementList(measurementDatabaseEntityList: List<MeasurementDatabaseEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUpload(uploadDatabaseEntity: UploadDatabaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUploadList(uploadDatabaseEntityList: List<UploadDatabaseEntity>): List<Long>

    @Query("SELECT * FROM metadata")
    suspend fun getMetadata(): List<MetadataDatabaseEntity>

    @Query("SELECT * FROM metadata WHERE uuid NOT IN (:uuidList)")
    suspend fun getMetadataExceptUuidList(uuidList: List<String>): List<MetadataDatabaseEntity>

    @Query("SELECT * FROM measurements WHERE uuid = :uuid")
    suspend fun getMeasurementList(uuid: String): List<MeasurementDatabaseEntity>

    @Query("SELECT * FROM uploads")
    suspend fun getUploads(): List<UploadDatabaseEntity>

    @Query("SELECT * FROM uploads WHERE uuid = :uuid")
    suspend fun getUpload(uuid: String): List<UploadDatabaseEntity>

    @Update(entity = MetadataDatabaseEntity::class)
    suspend fun updateMetadata(updater: MetadataUpdater)

    @Update(entity = MetadataDatabaseEntity::class)
    suspend fun updateMetadataList(updater: List<MetadataUpdater>): Int

    @Update(entity = MeasurementDatabaseEntity::class)
    suspend fun updateMeasurement(updater: MeasurementUpdater)

    @Update(entity = MeasurementDatabaseEntity::class)
    suspend fun updateMeasurementList(updater: List<MeasurementUpdater>): Int

    @Query("DELETE FROM metadata WHERE uuid = :uuid")
    suspend fun deleteMetadata(uuid: String): Int

    @Query("DELETE FROM metadata WHERE timestampEnd = (SELECT MAX(timestampEnd) FROM metadata)")
    suspend fun deleteLastMetadata(): Int

    @Query("DELETE FROM metadata")
    suspend fun deleteAllMetadata(): Int
}