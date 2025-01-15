package cat.urv.cloudlab.soundless.model.repository.local.room

import cat.urv.cloudlab.soundless.model.repository.EntityMapper
import cat.urv.cloudlab.soundless.viewmodel.RecordingMetadata
import javax.inject.Inject

class MetadataDatabaseMapper @Inject constructor():
    EntityMapper<MetadataDatabaseEntity, RecordingMetadata>
{
    override fun mapFromEntity(entity: MetadataDatabaseEntity): RecordingMetadata {
        return RecordingMetadata(
            uuid = entity.uuid,
            randomizedLng = entity.randomizedLng,
            randomizedLat = entity.randomizedLat,
            uncertaintyRadius = entity.uncertaintyRadius,
            frequency = entity.frequency,
            currentlyActive = entity.currentlyActive,
            timestampStart = entity.timestampStart,
            timestampEnd = entity.timestampEnd,
            isHealthDataAvailable = entity.isHealthDataAvailable
        )
    }

    override fun mapToEntity(domainModel: RecordingMetadata): MetadataDatabaseEntity {
        return MetadataDatabaseEntity(
            uuid = domainModel.uuid,
            randomizedLng = domainModel.randomizedLng,
            randomizedLat = domainModel.randomizedLat,
            uncertaintyRadius = domainModel.uncertaintyRadius,
            frequency = domainModel.frequency,
            currentlyActive = domainModel.currentlyActive,
            timestampStart = domainModel.timestampStart,
            timestampEnd = domainModel.timestampEnd,
            isHealthDataAvailable = domainModel.isHealthDataAvailable
        )
    }

    fun mapFromEntityList(entities: List<MetadataDatabaseEntity>): List<RecordingMetadata> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(domainModels: List<RecordingMetadata>): List<MetadataDatabaseEntity> {
        return domainModels.map { mapToEntity(it) }
    }

}