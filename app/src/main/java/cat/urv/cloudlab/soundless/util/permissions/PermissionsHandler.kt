package cat.urv.cloudlab.soundless.util.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

object PermissionsHandler {
    private val soundlessPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.RECORD_AUDIO
    )

    /**
     * Request the permissions specified in the *permissions* parameter.
     *
     * @param activity Activity from which permissions are requested
     */
    fun requestSoundlessPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(activity, soundlessPermissions, 0)
    }

    /**
     * Returns true if all permissions received from parameter have been granted, false otherwise.
     *
     * @param context Context from which permissions are checked
     */
    fun checkSoundlessPermissions(context: Context): Boolean {
        return !(soundlessPermissions.map {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }.contains(false))
    }
}