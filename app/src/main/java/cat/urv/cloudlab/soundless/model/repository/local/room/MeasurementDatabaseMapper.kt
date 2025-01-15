package cat.urv.cloudlab.soundless.model.repository.local.room

import cat.urv.cloudlab.soundless.model.repository.EntityMapper
import cat.urv.cloudlab.soundless.viewmodel.Measurement
import javax.inject.Inject

class MeasurementDatabaseMapper @Inject constructor():
    EntityMapper<MeasurementDatabaseEntity, Measurement>
{
    override fun mapFromEntity(entity: MeasurementDatabaseEntity): Measurement {
        return Measurement(
            uuid = entity.uuid,
            timestamp = entity.timestamp,
            dB = entity.dB,
            heartRate = entity.heartRate,
            sleepStage = entity.sleepStage
        )
    }

    override fun mapToEntity(domainModel: Measurement): MeasurementDatabaseEntity {
        return MeasurementDatabaseEntity(
            uuid = domainModel.uuid,
            timestamp = domainModel.timestamp,
            dB = domainModel.dB,
            heartRate = domainModel.heartRate,
            sleepStage = domainModel.sleepStage
        )
    }

    fun mapFromEntityList(entities: List<MeasurementDatabaseEntity>): List<Measurement> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(domainModels: List<Measurement>): List<MeasurementDatabaseEntity> {
        return domainModels.map { mapToEntity(it) }
    }

}