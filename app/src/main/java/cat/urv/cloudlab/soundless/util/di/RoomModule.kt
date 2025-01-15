package cat.urv.cloudlab.soundless.util.di

import android.content.Context
import androidx.room.Room
import cat.urv.cloudlab.soundless.model.repository.local.room.DatabaseDao
import cat.urv.cloudlab.soundless.model.repository.local.room.Database
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Singleton
    @Provides
    fun provideSummaryDatabase(@ApplicationContext context: Context): Database {
        return Room.databaseBuilder(
            context,
            Database::class.java,
            Database.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Singleton
    @Provides
    fun provideSummaryDao(database: Database): DatabaseDao {
        return database.databaseDao()
    }

}