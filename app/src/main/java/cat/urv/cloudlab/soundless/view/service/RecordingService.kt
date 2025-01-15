package cat.urv.cloudlab.soundless.view.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import cat.urv.cloudlab.soundless.R
import cat.urv.cloudlab.soundless.model.repository.MainRepository
import cat.urv.cloudlab.soundless.model.repository.MetadataUpdateType
import cat.urv.cloudlab.soundless.util.DeviceLocation
import cat.urv.cloudlab.soundless.viewmodel.Measurement
import cat.urv.cloudlab.soundless.viewmodel.RecordingMetadata
import cat.urv.cloudlab.soundless.viewmodel.recordingviewmodel.RecordingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject
import kotlin.math.log10

/**
 * Foreground, started, bound service where recordings are performed.
 *
 * RecordingService extends LifecycleService in order to have a ViewModel associated. ViewModel will
 * observe all raw data coming from the recordings and then structure the data (make DataPackets,
 * ExposureStage, manage states,...). The ViewModel will then be observed by the View every time it
 * binds to the service (i.e. when UI is active).
 */
@AndroidEntryPoint
class RecordingService: LifecycleService() {
    private lateinit var recordingViewModel: RecordingViewModel
    @Inject lateinit var deviceLocation: DeviceLocation
    @Inject lateinit var mainRepository: MainRepository

    // TODO this variable ought to be shared with other parts of the app via an Object or SharedPreferences
    private val frequency = 1000

    // Recording variables
    private lateinit var mediaRecorder: MediaRecorder
    private var recording = false
    private var recordingMustBeCancelled = false
    private lateinit var recordingMetadata: RecordingMetadata

    // Variable set in SettingsFragment to tell if data is sent to remote sources for analysis
    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences(
            getString(R.string.shared_preferences_filename),
            Context.MODE_PRIVATE
        )
    }
    private val flagParticipateInStudy: Boolean
        // By default, flag is active (in the UI too). But if user changes settings, they are saved
        // in SharedPreferences
        get() = run {
            false
        }

    // Binder given to clients of this Service
    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    /**
     * Start recording.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // We initialize the ViewModel this way because we do not need a separate lifecycle
        // Notice ViewModel is not strictly necessary and we could use observable liveData
        recordingViewModel = RecordingViewModel(deviceLocation, mainRepository)

        // Create Intent that will be sent when notification is clicked. In this case the Intent
        // will make us return to the MainActivity (without recreating it, if possible).
        // The following notification code has been adapted from the original
        // showRecordingNotification() called in MainFragment when recording started.
        val returnToAppIntent = packageManager.getLaunchIntentForPackage(packageName)
        val returnToAppPendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, returnToAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // 'Sticky' by default, as the service will start as a Foreground Service and thus user
        // cannot dismiss the notification. So no need to build it using .setOngoing(true)
        val recordingStickyNotification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_content))
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.logo_soundless_white))
            .setSmallIcon(R.drawable.ic_mic_recording_on_24)
            .setWhen(System.currentTimeMillis())
            .setUsesChronometer(true)
            .setContentIntent(returnToAppPendingIntent)
            .build()

        // Notification ID cannot be 0.
        startForeground(ONGOING_NOTIFICATION_ID, recordingStickyNotification)

        /****************************************/
        recordAudio()
        /****************************************/

        // "This mode makes sense for things that will be explicitly started and stopped to run for
        // arbitrary periods of time, such as a service performing background music playback."
        return Service.START_STICKY
    }

    private fun recordAudio() {
        recording = true
        val uuid = UUID.randomUUID().toString()
        recordingMetadata = RecordingMetadata(
            uuid,
            deviceLocation.lng,
            deviceLocation.lat,
            deviceLocation.radius,
            FREQUENCY,
            true,
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            false
        )
        recordingViewModel.setStateEvent(
            RecordingViewModel.RecordingStateEvent.MetadataCreateEvent(
                recordingMetadata,
                flagParticipateInStudy
            )
        )

        val filename = "recording_$uuid"
        val recordingFile = File(applicationContext.filesDir, filename)
        val recordingFilePath = recordingFile.canonicalPath

        initializeMediaRecorder(recordingFilePath)
        mediaRecorder.prepare()

        CoroutineScope(Default).launch {
            mediaRecorder.start()

            var skip = true
            runWhileRecording(frequency = frequency) {
                val maxAmplitude = mediaRecorder.maxAmplitude
                val dBFS: Float = 20 * log10(maxAmplitude / 255.0F)
                val dBSPL: Float = 94.0F + dBFS + mediaRecorder.activeMicrophones[0].sensitivity

                val now = System.currentTimeMillis()
                if (skip) skip = false
                else recordingViewModel.setStateEvent(
                    RecordingViewModel.RecordingStateEvent.DBEvent(
                        Measurement(
                            uuid,
                            now,
                            if (dBSPL < 30.0f) 30.0f else dBSPL,
                            -1,
                            -1
                        )
                    )
                )
                recordingMetadata.timestampEnd = now
                recordingViewModel.setStateEvent(
                    RecordingViewModel.RecordingStateEvent.MetadataUpdateEvent(
                        recordingMetadata,
                        MetadataUpdateType.UPDATE_TIMESTAMP,
                        flagParticipateInStudy
                    )
                )
                recordingFile.delete()
            }

            // Some data has not been summarized and saved yet. Save it
            recordingViewModel.setStateEvent(
                RecordingViewModel.RecordingStateEvent.SaveRemainingData
            )

            try {
                mediaRecorder.stop()
            } catch (e: Exception) {
                // Stop failed because it could not record any audio at all.
                // This could happen when stop() is immediately called after start(), which happens
                // whenever the user clicks the record button twice in MainFragment.
            } finally {
                recordingMetadata.currentlyActive = false
                val metadataUpdateType = if (recordingMustBeCancelled) {
                    MetadataUpdateType.CANCEL_RECORDING
                } else {
                    MetadataUpdateType.STOP_RECORDING
                }
                recordingMustBeCancelled = false
                recordingViewModel.setStateEvent(
                    RecordingViewModel.RecordingStateEvent.MetadataUpdateEvent(
                        recordingMetadata,
                        metadataUpdateType,
                        flagParticipateInStudy
                    )
                )

                // Always release mediaRecorder resources
                mediaRecorder.release()
                recordingFile.delete()
                stopSelf()
            }
        }
    }

    /**
     * Uses all configuration presets to instantiate the global MediaRecorder object.
     */
    private fun initializeMediaRecorder(recordingFileName: String) {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
            // setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            // setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setAudioEncoder(AudioFormat.ENCODING_PCM_FLOAT)
            setOutputFile(recordingFileName)
            setAudioSamplingRate(AUDIO_SAMPLING_RATE)
        }
    }

    /**
     * This is a custom decorator to reduce the boilerplate that comes with our coroutines.
     * It runs the body of a [function] every [frequency] milliseconds, while [recording] is true.
     * This does NOT guarantee that the execution of the [function] parameter runs in less than
     * [frequency] milliseconds - longer executions can take place.
     */
    private suspend inline fun runWhileRecording(
        frequency: Int,
        function: () -> Unit
    ) {
        var start: Long; var end: Long; var waitingTime: Long
        while (recording) {
            start = System.currentTimeMillis()
            function()
            end = System.currentTimeMillis()
            waitingTime = frequency - (end - start)
            if (waitingTime > 0) delay(waitingTime)
        }
    }

    override fun onDestroy() {
        recording = false
        super.onDestroy()
    }

    inner class LocalBinder: Binder() {
        fun getRecordingViewModel(): RecordingViewModel = recordingViewModel

        fun getCancelFunction(): () -> Unit = {
            recordingMustBeCancelled = true
            recording = false
            stopSelf()
        }

        fun getStopFunction(): () -> Unit = {
            recording = false
            stopSelf()
        }
    }

    companion object {
        private const val CHANNEL_ID = "Recording notifications"
        private const val ONGOING_NOTIFICATION_ID = 1
        private const val AUDIO_SAMPLING_RATE: Int = 8000
        private const val FREQUENCY = 1000
    }
}