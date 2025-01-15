package cat.urv.cloudlab.soundless.viewmodel

import kotlinx.serialization.Serializable

/**
 * A serializable unit of information containing all data that concerns a recording as a whole.
 */
@Serializable
data class RecordingMetadata(
    var uuid: String,
    var randomizedLng: Float,
    var randomizedLat: Float,
    var uncertaintyRadius: Float,
    var frequency: Int,
    var currentlyActive: Boolean,
    var timestampStart: Timestamp,
    var timestampEnd: Timestamp,
    var isHealthDataAvailable: Boolean
)