package cat.urv.cloudlab.soundless.util.di

import android.content.Context
import android.content.Intent
import cat.urv.cloudlab.soundless.view.service.RecordingService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecordingServiceModule {

    @Singleton
    @Provides
    fun provideRecordingIntent(@ApplicationContext applicationContext: Context): Intent {
        return Intent(applicationContext, RecordingService::class.java)
    }
}