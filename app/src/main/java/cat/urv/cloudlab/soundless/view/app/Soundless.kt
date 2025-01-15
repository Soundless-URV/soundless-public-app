package cat.urv.cloudlab.soundless.view.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Soundless: Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase app to perform Firebase App Check verification
        // This will be executed before running any other Firebase SDK
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        // Notice we have to enable checks in Firebase Cloud Functions for this to work
        // https://firebase.google.com/docs/app-check/cloud-functions
    }
}