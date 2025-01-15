package cat.urv.cloudlab.soundless.model.repository.health.fitbit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * IMPORTANT NOTICE:
 *
 * Generally we will get both "sleep stages" and "classic sleep" in the same response.
 * https://dev.fitbit.com/build/reference/web-api/sleep/get-sleep-log-by-date-range/#Types-of-sleep-data
 * We can differentiate the type via SleepPeriod.type.
 *
 * There are several reasons why we may ONLY get classic sleep information instead:
 * - The sleep wasn't long enough to calculate sleep stages. The minimum is around 3 hours.
 * - The sleep data was manually entered.
 */

@Serializable
data class FitbitSleepEntity(
    @SerialName("sleep") val sleepPeriods: List<SleepPeriod>
)

@Serializable
data class SleepPeriod(
    val startTime: String,
    val endTime: String,
    val levels: SleepLevels,
    val type: String  // classic / stages
    // Other data in JSON is ignored
)

@Serializable
data class SleepLevels(
    val data: List<SleepDatum>
    // Other data in JSON is ignored
)

@Serializable
data class SleepDatum(
    val dateTime: String,
    val level: String,
    val seconds: Int
)