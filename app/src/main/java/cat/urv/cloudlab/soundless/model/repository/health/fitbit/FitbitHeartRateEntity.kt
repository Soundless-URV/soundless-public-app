package cat.urv.cloudlab.soundless.model.repository.health.fitbit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FitbitHeartRateEntity(
    @SerialName("activities-heart") val heartRateMetadata: List<HeartRateMetadata>,
    @SerialName("activities-heart-intraday") val heartRateData: HeartRateData
)

@Serializable
data class HeartRateMetadata(
    val dateTime: String
    // Other data in JSON is ignored
)

@Serializable
data class HeartRateData(
    val dataset: List<HeartRateDatum>
    // Other data in JSON is ignored
)

@Serializable
data class HeartRateDatum(
    val time: String,
    val value: Int
)