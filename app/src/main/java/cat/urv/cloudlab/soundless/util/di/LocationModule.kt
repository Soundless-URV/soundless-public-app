package cat.urv.cloudlab.soundless.util.di

import android.content.Context
import cat.urv.cloudlab.soundless.util.DeviceLocation
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Singleton
    @Provides
    fun provideDeviceLocation(@ApplicationContext applicationContext: Context): DeviceLocation {
        return DeviceLocation(applicationContext)
    }
}