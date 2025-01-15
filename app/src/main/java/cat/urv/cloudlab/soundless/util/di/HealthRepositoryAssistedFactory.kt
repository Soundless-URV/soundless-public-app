package cat.urv.cloudlab.soundless.util.di

import cat.urv.cloudlab.soundless.model.repository.HealthRepository
import cat.urv.cloudlab.soundless.model.repository.health.HealthDataClient
import dagger.assisted.AssistedFactory

@AssistedFactory
interface HealthRepositoryAssistedFactory {

    /**
     * @param healthDataClient This parameter is assisted (Assisted Injection - Dagger), meaning
     * all [HealthRepository] parameters are injected at build time except for this one. At runtime,
     * and in a suitable Context, user instantiates a health data client and passes it to a
     * [HealthRepositoryAssistedFactory.create] call, providing just the last needed piece for the
     * [HealthRepository] to operate.
     */
    fun create(healthDataClient: HealthDataClient): HealthRepository

}