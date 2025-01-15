package cat.urv.cloudlab.soundless.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class DeviceLocation @Inject constructor(val applicationContext: Context) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val sharedPreferences = applicationContext.getSharedPreferences(
        SHARED_PREFERENCES_FILENAME,
        Context.MODE_PRIVATE
    )

    init { update() }
    fun update() = requestLocationData()

    var lng: Float = DEFAULT_LNG_LAT
        get() = sharedPreferences.getFloat("lng", DEFAULT_LNG_LAT)
        private set

    var lat: Float = DEFAULT_LNG_LAT
        get() = sharedPreferences.getFloat("lat", DEFAULT_LNG_LAT)
        private set

    /**
     * By default, [DEFAULT_RADIUS].
     * IMPORTANT NOTE: a variance in longitude is not the same as a variance in latitude in terms of
     * distance in meters. For the sake of simplicity, this is overlooked here. Understand this can
     * lead to problems when it comes to map representations.
     * TODO: consider revisiting
     */
    val radius: Float = DEFAULT_RADIUS  // In lng/lat variance

    /**
     * Tries to obtain the last location in device. This location will be available if other apps
     * have been using location services. In case we cannot get this location data, we request new
     * data using requestNewLocationData().
     *
     * @see requestNewLocationData
     */
    private fun requestLocationData() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                // Manifest.permission.ACCESS_COARSE_LOCATION
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                var isMock = false

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                    if (location == null || location.isMock)
                        isMock = true
                }

                if (location == null || isMock ||
                    location.longitude.isNaN() || location.longitude.isInfinite() ||
                    location.latitude.isNaN() || location.latitude.isInfinite() ||
                    location.longitude > 180.0f || location.longitude < -180.0f ||
                    location.latitude > 90.0f || location.latitude < -90.0f
                ) {
                    requestNewLocationData()
                } else {
                    val (lng, lat) = randomLngLatWithinCircle(
                        location.longitude.toFloat(),
                        location.latitude.toFloat(),
                        radius
                    )
                    val editor = sharedPreferences.edit()
                    editor.putFloat("lng", lng)
                    editor.putFloat("lat", lat)
                    editor.apply()
                }
            }
            .addOnFailureListener {
                requestNewLocationData()
            }
        fusedLocationClient.lastLocation
    }

    /**
     * Function used to determine location if FusedLocationProvider returned no valid data.
     * Creates a formal request to get the device current location.
     */
    private fun requestNewLocationData() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            interval = 5000
            fastestInterval = 10
            numUpdates = 3
        }
        locationRequest.expirationTime = 6000
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper() as Looper
        )
    }

    /**
     * Anonymous class used as a callback container in requestNewLocationData().
     *
     * @see requestNewLocationData
     */
    private val locationCallback = object: LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            val (lng, lat) = randomLngLatWithinCircle(
                location.longitude.toFloat(),
                location.latitude.toFloat(),
                radius
            )
            val editor = sharedPreferences.edit()
            editor.putFloat("lng", lng)
            editor.putFloat("lat", lat)
            editor.apply()
        }
    }

    /**
     * Given a circle C with center ([lng], [lat]) and radius [radius], get a random point
     * ([lng], [lat]) within C.
     *
     * @param lng Longitude
     * @param lat Latitude
     * @param radius Circle radius in lng/lat variance
     */
    private fun randomLngLatWithinCircle(
        lng: Float,
        lat: Float,
        radius: Float
    ): Pair<Float, Float> {
        val lngSymbol = if (Random.nextBoolean()) 1 else -1
        val randomLng = lng + Random.nextFloat() * radius * lngSymbol

        val latSymbol = if (Random.nextBoolean()) 1 else -1
        val maxLat = sqrt(radius.pow(2) - (lng - randomLng).pow(2))
        val randomLat = lat + Random.nextFloat() * maxLat * latSymbol

        return Pair(randomLng, randomLat)
    }

    companion object {
        const val DEFAULT_LNG_LAT = 999.0F

        /**
         * Radius in lng/lat variance.
         */
        const val DEFAULT_RADIUS = 2e-4f
        const val SHARED_PREFERENCES_FILENAME = "shared_preferences_filename"
    }
}