package cat.urv.cloudlab.soundless.model.repository.health

enum class SleepStageValues(val value: Int) {
    WAKE(0),
    LIGHT(1),
    DEEP(2),
    REM(3),
    UNKNOWN(-1)
}