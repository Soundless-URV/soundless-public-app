package cat.urv.cloudlab.soundless.util.di

import android.content.Context
import cat.urv.cloudlab.soundless.util.FileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object FileManagerModule {

    @Singleton
    @Provides
    fun provideFileManager(@ApplicationContext applicationContext: Context): FileManager {
        return FileManager(applicationContext)
    }
}