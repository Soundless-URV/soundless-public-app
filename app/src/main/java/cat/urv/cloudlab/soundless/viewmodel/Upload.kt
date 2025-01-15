package cat.urv.cloudlab.soundless.viewmodel

import kotlinx.serialization.Serializable

@Serializable
data class Upload(
    var uuid: String,
    var timestamp: Timestamp
)