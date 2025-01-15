package cat.urv.cloudlab.soundless.model.repository.local.room

import cat.urv.cloudlab.soundless.model.repository.EntityMapper
import cat.urv.cloudlab.soundless.viewmodel.Upload
import javax.inject.Inject

class UploadDatabaseMapper @Inject constructor():
    EntityMapper<UploadDatabaseEntity, Upload>
{
    override fun mapFromEntity(entity: UploadDatabaseEntity): Upload {
        return Upload(entity.uuid, entity.timestamp)
    }

    override fun mapToEntity(domainModel: Upload): UploadDatabaseEntity {
        return UploadDatabaseEntity(uuid = domainModel.uuid, timestamp = domainModel.timestamp)
    }

    fun mapFromEntityList(entities: List<UploadDatabaseEntity>): List<Upload> {
        return entities.map { mapFromEntity(it) }
    }

    fun mapToEntityList(domainModels: List<Upload>): List<UploadDatabaseEntity> {
        return domainModels.map { mapToEntity(it) }
    }

}