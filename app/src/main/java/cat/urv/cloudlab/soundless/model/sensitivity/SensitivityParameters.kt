package cat.urv.cloudlab.soundless.model.sensitivity

data class SensitivityParameters(
    val lagDB: Int = 20,
    val thresholdDB: Double = 3.0,
    val influenceDB: Double = 0.25,

    val lagHR: Int = 30,
    val thresholdHR: Double = 5.0,
    val influenceHR: Double = 1.0
)