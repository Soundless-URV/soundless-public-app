package cat.urv.cloudlab.soundless.model.repository

import cat.urv.cloudlab.soundless.model.repository.local.room.*
import cat.urv.cloudlab.soundless.model.repository.remote.firebase.FirebaseFunctionAPIClient
import cat.urv.cloudlab.soundless.util.datastate.RepositoryDataState
import cat.urv.cloudlab.soundless.viewmodel.Measurement
import cat.urv.cloudlab.soundless.viewmodel.RecordingMetadata
import cat.urv.cloudlab.soundless.viewmodel.Upload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainRepository(
    private val databaseDao: DatabaseDao,
    private val metadataDatabaseMapper: MetadataDatabaseMapper,
    private val measurementDatabaseMapper: MeasurementDatabaseMapper,
    private val uploadDatabaseMapper: UploadDatabaseMapper
) {

    suspend fun getMetadata(): Flow<RepositoryDataState<List<RecordingMetadata>>> = flow {
        emit(RepositoryDataState.Loading)

        try {
            // Get all from database
            val metadataInDB = databaseDao.getMetadata()
            emit(RepositoryDataState.Success(
                metadataDatabaseMapper.mapFromEntityList(metadataInDB))
            )
        } catch (e: Exception) {
            println(e)
            emit(RepositoryDataState.Error(e))
        }
    }

    suspend fun getMetadataExceptUuidList(uuidList: List<String>): Flow<RepositoryDataState<List<RecordingMetadata>>> = flow {
        emit(RepositoryDataState.Loading)

        try {
            // Get all from database
            val metadataInDB = databaseDao.getMetadataExceptUuidList(uuidList)
            emit(RepositoryDataState.Success(
                metadataDatabaseMapper.mapFromEntityList(metadataInDB))
            )
        } catch (e: Exception) {
            println(e)
            emit(RepositoryDataState.Error(e))
        }
    }

    suspend fun getUploads(): Flow<RepositoryDataState<List<Upload>>> = flow {
        emit(RepositoryDataState.Loading)

        try {
            val uploadsInDB = databaseDao.getUploads()
            emit(RepositoryDataState.Success(
                uploadDatabaseMapper.mapFromEntityList(uploadsInDB))
            )
        } catch (e: Exception) {
            println(e)
            emit(RepositoryDataState.Error(e))
        }
    }

    /**
     * Write a single [RecordingMetadata] locally.
     *
     * When creating a Metadata object, we want to have it in the local SQL database ASAP so that
     * SQL tables for Measurements and/or other data can have primary keys linking to their
     * corresponding Metadata. We will be updating these Metadata rows frequently, for example, with
     * new timestamps.
     *
     * When it comes to remote sources, we will only post Metadata objects when a recording has been
     * stopped (not cancelled). That's why we are only posting to the remote sources in the method
     * [updateMetadata], which gets called when the recording stops.
     */
    suspend fun postMetadata(metadata: RecordingMetadata): Flow<RepositoryDataState<Long>> = flow {
        emit(RepositoryDataState.Loading)

        try {
            val row = databaseDao.insertMetadata(metadataDatabaseMapper.mapToEntity(metadata))
            emit(RepositoryDataState.Success(row))
        } catch (e: Exception) {
            // This should never happen as long as Room's .insert() is not annotated with
            // @Throws(SQLiteException::class) or similar
            println(e)
            emit(RepositoryDataState.Error(e))
        }
    }

    suspend fun postMetadataList(metadataList: List<RecordingMetadata>): Flow<RepositoryDataState<List<Long>>> = flow {
        emit(RepositoryDataState.Loading)

        try {
            val rows = databaseDao.insertMetadataList(metadataDatabaseMapper.mapToEntityList(metadataList))
            emit(RepositoryDataState.Success(rows))
        } catch (e: Exception) {
            // This should never happen as long as Room's .insert() is not annotated with
            // @Throws(SQLiteException::class) or similar
            println(e)
            emit(RepositoryDataState.Error(e))
        }
    }

    /**
     * Write a single [Upload] to the local database only.
     */
    suspend fun postUpload(upload: Upload): Flow<RepositoryDataState<Long>> = flow {
        try {
            databaseDao.insertUpload(uploadDatabaseMapper.mapToEntity(upload))
        } catch (e: Exception) {
            println(e)
        }
    }

    suspend fun postUploadList(uploadList: List<Upload>): Flow<RepositoryDataState<List<Long>>> = flow {
        try {
            databaseDao.insertUploadList(uploadDatabaseMapper.mapToEntityList(uploadList))
        } catch (e: Exception) {
            println(e)
        }
    }

    /**
     * Remove a single [RecordingMetadata] from the local database only.
     */
    suspend fun deleteMetadata(uuid: String): Flow<RepositoryDataState<Int>> = flow {
        emit(RepositoryDataState.Loading)

        try {
            val row = databaseDao.deleteMetadata(uuid)
            emit(RepositoryDataState.Success(row))
        } catch (e: Exception) {
            println(e)
            emit(RepositoryDataState.Error(e))
        }
    }

    /**
     * Get all [Measurement]s associated with a [RecordingMetadata].
     */
    suspend fun getMeasurementsForRecording(
        recordingMetadata: RecordingMetadata
    ): Flow<RepositoryDataState<List<Measurement>>> = getMeasurementsForRecording(
        recordingMetadata.uuid
    )

    /**
     * Get all [Measurement]s associated with a [RecordingMetadata].
     */
    suspend fun getMeasurementsForRecording(
        uuid: String
    ): Flow<RepositoryDataState<List<Measurement>>> = flow {
        emit(RepositoryDataState.Loading)

        try {
            val measurements = databaseDao.getMeasurementList(uuid)
            if (measurements.isNotEmpty()) {
                emit(RepositoryDataState.Success(
                    measurementDatabaseMapper.mapFromEntityList(measurements))
                )
            } else {
                emit(RepositoryDataState.Error(Exception()))
            }
        } catch (e: Exception) {
            println(e)
            emit(RepositoryDataState.Error(e))
        }
    }

    /**
     * TODO: should refactor this method as its behaviour is obscure
     * Update a single [RecordingMetadata] to sources.
     *
     * @param flagParticipateInStudy is only relevant when [updateType] is
     * [MetadataUpdateType.STOP_RECORDING]. If it's not, you can set the flag to any value - it will
     * be ignored.
     */
    suspend fun updateMetadata(
        metadata: RecordingMetadata,
        updateType: MetadataUpdateType,
        flagParticipateInStudy: Boolean
    ): Flow<RepositoryDataState<RecordingMetadata>> = flow {
        val firebaseFunctionAPIClient = FirebaseFunctionAPIClient()

        emit(RepositoryDataState.Loading)

        try {
            if (updateType in setOf(
                    MetadataUpdateType.CANCEL_RECORDING,
                    MetadataUpdateType.STOP_RECORDING,
                    MetadataUpdateType.SET_RECORDING_NOT_ACTIVE)
            ) {
                metadata.currentlyActive = false
            }

            // To update to the local database, the type of update makes no difference. We just take
            // the metadata object and update its row.
            databaseDao.updateMetadata(
                MetadataUpdater(
                    metadata.uuid,
                    metadata.currentlyActive,
                    metadata.timestampEnd,
                    metadata.isHealthDataAvailable
                )
            )

            // However, when it comes to remote changes, the Update type is important
            when (updateType) {
                // If we cancel a recording, nothing will happen to the remote data
                MetadataUpdateType.CANCEL_RECORDING -> {
                    deleteMetadata(metadata.uuid).collect()
                }
                // If we stop, and user has chosen to participate in the study, then their data has
                // to be sent to the remote source
                in listOf(
                        MetadataUpdateType.STOP_RECORDING,
                        MetadataUpdateType.SET_HEALTH_AVAILABLE,
                        MetadataUpdateType.UPLOAD_RECORDING_ONLY) -> if (flagParticipateInStudy) {
                    getMeasurementsForRecording(metadata).onEach { state ->
                        when (state) {
                            is RepositoryDataState.Success -> {
                                firebaseFunctionAPIClient
                                    .postRecording(metadata, state.data)
                                    .addOnSuccessListener {
                                        println("Uploaded recording ${metadata.uuid}! Posting upload to DB...")
                                        val upload = Upload(
                                            metadata.uuid,
                                            System.currentTimeMillis()
                                        )
                                        CoroutineScope(Dispatchers.Default).launch {
                                            postUpload(upload).onEach {
                                                if (it is RepositoryDataState.Success) {
                                                    emit(RepositoryDataState.Success(metadata))
                                                }
                                            }.collect()
                                        }
                                    }
                                    .addOnFailureListener {
                                        println("Recording ${metadata.uuid} was not uploaded.")
                                        println(it.stackTraceToString())
                                    }
                            }
                            else -> {
                                // Do nothing
                            }
                        }
                    }.collect()
                }  // else (if NOT flagParticipateInStudy) we have data saved locally only

                // Updating timestamp only is done real-time in the local database
                MetadataUpdateType.UPDATE_TIMESTAMP -> {
                    emit(RepositoryDataState.Success(metadata))
                }
                // Same for setting zombie recordings to not active
                MetadataUpdateType.SET_RECORDING_NOT_ACTIVE -> {
                    emit(RepositoryDataState.Success(metadata))
                }
                else -> {
                    // Else clause is mandatory/recommended because of type SET_HEALTH_AVAILABLE
                    // even though it's covered in a case "when in listOf(... SET_HEALTH_AVAILABLE)"
                    emit(RepositoryDataState.Success(metadata))
                }
            }
        } catch (e: Exception) {
            println(e)
            emit(RepositoryDataState.Error(e))
        }
    }

    /**
     * Write a single [Measurement] to sources.
     */
    suspend fun postMeasurement(measurement: Measurement): Flow<RepositoryDataState<Long>> = flow {
        emit(RepositoryDataState.Loading)

        try {
            val row = databaseDao.insertMeasurement(
                measurementDatabaseMapper.mapToEntity(measurement)
            )
            emit(RepositoryDataState.Success(row))
        } catch (e: Exception) {
            println(e)
            emit(RepositoryDataState.Error(e))
        }
    }

    /**
     * Write a list of [Measurement]s to sources.
     */
    suspend fun postMeasurements(measurements: List<Measurement>): Flow<RepositoryDataState<List<Long>>> = flow {
        emit(RepositoryDataState.Loading)

        try {
            val rows = databaseDao.insertMeasurementList(
                measurementDatabaseMapper.mapToEntityList(measurements)
            )
            emit(RepositoryDataState.Success(rows))
        } catch (e: Exception) {
            println(e)
            emit(RepositoryDataState.Error(e))
        }
    }

    /**
     * Update [Measurement]s in sources.
     */
    suspend fun updateMeasurements(measurements: List<Measurement>): Flow<RepositoryDataState<Int>> = flow {
        emit(RepositoryDataState.Loading)

        try {
            val updaters = measurements.map {
                MeasurementUpdater(it.uuid, it.timestamp, it.heartRate, it.sleepStage)
            }
            val numUpdatedRows = databaseDao.updateMeasurementList(updaters)
            emit(RepositoryDataState.Success(numUpdatedRows))
        } catch (e: Exception) {
            println(e)
            emit(RepositoryDataState.Error(e))
        }
    }

    suspend fun deleteAllMetadata(): Flow<RepositoryDataState<Int>> = flow {
        emit(RepositoryDataState.Loading)

        try {
            val numDeletedRows = databaseDao.deleteAllMetadata()
            emit(RepositoryDataState.Success(numDeletedRows))
        } catch (e: Exception) {
            println(e)
            emit(RepositoryDataState.Error(e))
        }
    }

}