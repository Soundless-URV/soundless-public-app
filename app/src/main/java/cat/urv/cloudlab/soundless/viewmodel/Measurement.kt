package cat.urv.cloudlab.soundless.viewmodel

/**
 * Timestamp in milliseconds. Type alias for Long.
 */
typealias Timestamp = Long

/**
 * Tuple containing all values that represent a single instant of measurements.
 */
data class Measurement(
    var uuid: String,
    var timestamp: Timestamp,
    var dB: Float,
    var heartRate: Int,
    var sleepStage: Int
)