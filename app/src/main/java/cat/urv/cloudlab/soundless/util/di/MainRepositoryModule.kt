package cat.urv.cloudlab.soundless.util.di

import cat.urv.cloudlab.soundless.model.repository.MainRepository
import cat.urv.cloudlab.soundless.model.repository.local.room.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MainRepositoryModule {

    @Singleton
    @Provides
    fun provideMainRepository(
        databaseDao: DatabaseDao,
        metadataDatabaseMapper: MetadataDatabaseMapper,
        measurementDatabaseMapper: MeasurementDatabaseMapper,
        uploadDatabaseMapper: UploadDatabaseMapper
    ): MainRepository {
        return MainRepository(
            databaseDao,
            metadataDatabaseMapper,
            measurementDatabaseMapper,
            uploadDatabaseMapper
        )
    }
}