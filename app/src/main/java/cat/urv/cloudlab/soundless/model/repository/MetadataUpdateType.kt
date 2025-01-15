package cat.urv.cloudlab.soundless.model.repository

enum class MetadataUpdateType {
    CANCEL_RECORDING,
    STOP_RECORDING,
    UPLOAD_RECORDING_ONLY,
    UPDATE_TIMESTAMP,
    SET_HEALTH_AVAILABLE,
    SET_RECORDING_NOT_ACTIVE
}