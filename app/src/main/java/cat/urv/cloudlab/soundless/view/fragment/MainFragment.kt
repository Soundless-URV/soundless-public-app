package cat.urv.cloudlab.soundless.view.fragment

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.IBinder
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.transition.TransitionSet.ORDERING_TOGETHER
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import cat.urv.cloudlab.soundless.R
import cat.urv.cloudlab.soundless.databinding.FragmentMainBinding
import cat.urv.cloudlab.soundless.util.DeviceLocation
import cat.urv.cloudlab.soundless.util.datastate.RecordingDataState
import cat.urv.cloudlab.soundless.util.datastate.RepositoryDataState
import cat.urv.cloudlab.soundless.util.permissions.PermissionsHandler
import cat.urv.cloudlab.soundless.view.activity.MainActivity
import cat.urv.cloudlab.soundless.view.service.RecordingService
import cat.urv.cloudlab.soundless.viewmodel.Measurement
import cat.urv.cloudlab.soundless.viewmodel.recordingviewmodel.RecordingViewModel
import cat.urv.cloudlab.soundless.viewmodel.repositoryviewmodel.RepositoryStateEvent
import cat.urv.cloudlab.soundless.viewmodel.repositoryviewmodel.RepositoryViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment() {
    // View Binding
    private var _binding: FragmentMainBinding? = null
    private val binding
        get() = _binding!!

    // Injected classes and ViewModels
    @Inject lateinit var deviceLocation: DeviceLocation

    // Important: do not remove requireParentFragment(), otherwise new ViewModels are created
    private val repositoryViewModel: RepositoryViewModel by viewModels({ requireParentFragment() })
    private lateinit var recordingViewModel: RecordingViewModel

    // Recording Service
    @Inject lateinit var intentToStartRecordingService: Intent
    private lateinit var cancelFunction: () -> Unit
    private lateinit var stopFunction: () -> Unit
    private var boundToRecordingService = false

    // Other variables
    private var allPermissionsGranted = false
    private var recording = false

    private val connection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RecordingService.LocalBinder
            recordingViewModel = binder.getRecordingViewModel()

            cancelFunction = binder.getCancelFunction()
            stopFunction = binder.getStopFunction()
            boundToRecordingService = true

            // TODO check if this _binding != null is ok, we found a NullPointerException once
            if (_binding != null) with(binding) {
                recordButton.backgroundTintList =
                    ContextCompat.getColorStateList(activity as Activity, R.color.alert)
                recordButton.setImageResource(R.drawable.ic_baseline_stop_24)
                cancelRecordButton.show()
                showRecordingLabels()
            }

            subscribeRecordingObservers()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            boundToRecordingService = false

            // TODO check if this _binding != null is ok, we found a NullPointerException once
            if (_binding != null) with(binding) {
                recordButton.backgroundTintList =
                    ContextCompat.getColorStateList(activity as Activity, R.color.primary)
                recordButton.setImageResource(R.drawable.ic_mic_recording_on_24)
                cancelRecordButton.hide()
                hideRecordingLabels()
            }

            unsubscribeRecordingObservers()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        repositoryViewModel.setStateEvent(RepositoryStateEvent.IsRecordingEvent)
        _binding = FragmentMainBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        if (recording) {
            requireActivity().unbindService(connection)
            boundToRecordingService = false
        }
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (recording) {
            with(binding) {
                recordButton.backgroundTintList =
                    ContextCompat.getColorStateList(activity as Activity, R.color.alert)
                recordButton.setImageResource(R.drawable.ic_baseline_stop_24)
                cancelRecordButton.show()
                showRecordingLabels()
            }
        } else {
            with(binding) {
                recordButton.backgroundTintList =
                    ContextCompat.getColorStateList(activity as Activity, R.color.primary)
                recordButton.setImageResource(R.drawable.ic_mic_recording_on_24)
                cancelRecordButton.hide()
                hideRecordingLabels()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER

        (activity as MainActivity).showToolbar()  // as it may be hidden from tutorial fragments

        // All UI configuration (Views, toolbars, etc) is inside the following clause
        with(binding) {
            decibelsText.text = getString(R.string.main_dB, 30.00f)
            recordButton.setOnClickListener {
                recordButton.isClickable = false
                recording = !recording  // 'Toggle' flag
                allPermissionsGranted =
                    PermissionsHandler.checkSoundlessPermissions(activity as Activity)
                if (recording == ON && allPermissionsGranted) {
                    startRecording()
                } else if (recording == ON && !allPermissionsGranted) {
                    PermissionsHandler.requestSoundlessPermissions(activity as Activity)
                    recording = false
                } else if (recording == OFF && allPermissionsGranted) {
                    stopRecording()
                } else if (recording == OFF && !allPermissionsGranted) {
                    cancelRecording()
                }
                CoroutineScope(Dispatchers.Default).launch {
                    delay(500)
                    withContext(Dispatchers.Main) {
                        recordButton.isClickable = true
                    }
                }
            }
            cancelRecordButton.setOnClickListener {
                recording = false
                cancelRecording()
            }
        }

        // Create NotificationChannel if it does not exist
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.description = descriptionText
        val notificationManager =
            requireActivity().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        // Register permissions. All permissions have to be accepted in order for the app to
        // generate reports. Permission requests are not called in the Activity itself, but they are
        // called using its context. Results will be provided even when permissions have been denied
        // twice and Android does not allow any further permission requests.
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { true }) {
                // Correct permissions
                allPermissionsGranted = true
                startRecording()
            } else {
                // At least one invalid permission
                allPermissionsGranted = false
                binding.recordButton.performClick()
            }
        }

        // ViewModel observers
        subscribeRepositoryObservers()

        bindToService()
    }

    /**
     * Runs intent to connect to the ongoing service.
     */
    private fun bindToService() {
        // Bind to service
        Intent(requireActivity(), RecordingService::class.java).also { intent ->
            requireActivity().bindService(intent, connection, Context.BIND_ABOVE_CLIENT)
        }
        boundToRecordingService = true
    }

    /**
     * Start recording from service, and perform UI changes.
     */
    private fun startRecording() {
        repositoryViewModel.setStateEvent(RepositoryStateEvent.StartCollectingEvent)

        with(binding) {
            recordButton.backgroundTintList =
                ContextCompat.getColorStateList(activity as Activity, R.color.alert)
            recordButton.setImageResource(R.drawable.ic_baseline_stop_24)
            cancelRecordButton.show()
            showRecordingLabels()
        }

        // Start service (Started service)
        ContextCompat.startForegroundService(requireContext(), intentToStartRecordingService)
        bindToService()
    }

    /**
     * Stop recording service, and perform UI changes.
     */
    private fun stopRecording() {
        try {
            requireActivity().unbindService(connection)
            stopFunction.invoke()
        } catch (e: Exception) {
            // Not bound to service in the current context.
            // App might have been killed in the background.
        } finally {
            boundToRecordingService = false

            repositoryViewModel.setStateEvent(RepositoryStateEvent.StopCollectingEvent)
            with(binding) {
                recordButton.backgroundTintList =
                    ContextCompat.getColorStateList(activity as Activity, R.color.primary)
                recordButton.setImageResource(R.drawable.ic_mic_recording_on_24)

                cancelRecordButton.hide()
                hideRecordingLabels()

                Snackbar.make(
                    binding.recordButton,
                    if (allPermissionsGranted) R.string.snack_report else R.string.snack_no_report,
                    8000)
                .setActionTextColor(resources.getColor(R.color.secondary_light))
                .setAnchorView(R.id.recordButton)
                .show()
            }
        }
    }

    /**
     * Dismiss the current recording. Service will stop all processes cleanly and reports
     * will not be created. This method is very similar to stopRecording().
     */
    private fun cancelRecording() {
        try {
            requireActivity().unbindService(connection)
            cancelFunction.invoke()
        } catch (e: Exception) {
            // Not bound to service in the current context.
            // App might have been killed in the background.
        } finally {
            boundToRecordingService = false

            repositoryViewModel.setStateEvent(RepositoryStateEvent.CancelCollectingEvent)
            with(binding) {
                recordButton.backgroundTintList =
                    ContextCompat.getColorStateList(activity as Activity, R.color.primary)
                recordButton.setImageResource(R.drawable.ic_mic_recording_on_24)
                cancelRecordButton.hide()
                hideRecordingLabels()

                Snackbar.make(
                    binding.recordButton,
                    R.string.recording_cancelled,
                    8000)
                .setActionTextColor(resources.getColor(R.color.secondary_light))
                .setAnchorView(R.id.recordButton)
                .show()
            }
        }
    }

    /**
     * Recording labels: labels aside of the Cancel/Stop buttons that explain what the buttons do.
     * Start a transition in order to turn Recording labels from Visible to Invisible.
     */
    private fun hideRecordingLabels() {
        with(binding) {
            TransitionSet()
                .addTransition(
                    Fade(Fade.MODE_OUT)
                        .addTarget(R.id.textCancelRecordButton)
                        .setDuration(FADE_LENGTH_MILLIS)
                )
                .addTransition(
                    Fade(Fade.MODE_OUT)
                        .addTarget(R.id.textCancelRecordButton)
                        .setDuration(FADE_LENGTH_MILLIS)
                ).ordering = ORDERING_TOGETHER
            TransitionManager.beginDelayedTransition(parentConstraintLayout as ViewGroup)
            textCancelRecordButton.visibility = View.INVISIBLE
            textRecordButton.visibility = View.INVISIBLE
        }
    }

    /**
     * Recording labels: labels aside of the Cancel/Stop buttons that explain what the buttons do.
     * Start a transition in order to turn Recording labels from Invisible to Visible.
     */
    private fun showRecordingLabels() {
        with(binding) {
            TransitionSet()
                .addTransition(
                    Fade(Fade.MODE_IN)
                        .addTarget(R.id.textCancelRecordButton)
                        .setDuration(FADE_LENGTH_MILLIS)
                )
                .addTransition(
                    Fade(Fade.MODE_IN)
                        .addTarget(R.id.textCancelRecordButton)
                        .setDuration(FADE_LENGTH_MILLIS)
                ).ordering = ORDERING_TOGETHER
            TransitionManager.beginDelayedTransition(parentConstraintLayout as ViewGroup)
            textCancelRecordButton.visibility = View.VISIBLE
            textRecordButton.visibility = View.VISIBLE
        }
    }

    /**
     * Subscribe only to observers from the [RepositoryViewModel].
     * This is triggered when entering to the Fragment.
     */
    private fun subscribeRepositoryObservers() {
        repositoryViewModel.repositoryMetadataLiveData.observe(this as LifecycleOwner) { dataState ->
            when (dataState) {
                is RepositoryDataState.Error -> {
                    Toast.makeText(
                        activity as Activity,
                        getString(R.string.error_loading_data),
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    // Do nothing
                }
            }
        }
        repositoryViewModel.booleanLiveData.observe(this as LifecycleOwner) { dataState ->
            when (dataState) {
                is RepositoryDataState.Recording -> {
                    recording = ON
                    bindToService()
                    with(binding) {
                        recordButton.backgroundTintList =
                            ContextCompat.getColorStateList(activity as Activity, R.color.alert)
                        recordButton.setImageResource(R.drawable.ic_baseline_stop_24)
                        cancelRecordButton.show()
                        showRecordingLabels()
                    }
                }
                is RepositoryDataState.NotRecording -> {
                    recording = OFF
                    with(binding) {
                        recordButton.backgroundTintList =
                            ContextCompat.getColorStateList(activity as Activity, R.color.primary)
                        recordButton.setImageResource(R.drawable.ic_mic_recording_on_24)
                        cancelRecordButton.hide()
                        hideRecordingLabels()
                    }
                }
                else -> {
                    // Do nothing
                }
            }
        }
    }

    /**
     * Subscribe to events in the [RecordingService].
     * This is triggered when binding to the service, after starting it.
     */
    private fun subscribeRecordingObservers() {
        recordingViewModel.measurementLiveData.observe(this as LifecycleOwner) { dataState ->
            when (dataState) {
                is RecordingDataState.DBCollected<Measurement> -> {
                    // If we are receiving measurements, we are still recording!
                    // This may happen when app has been flushed from memory and repository
                    // savedStateHandle has been cleaned up. ViewModel should know this
                    if (recording == OFF) {
                        repositoryViewModel.setStateEvent(RepositoryStateEvent.ReceivedUnexpectedMeasurement)
                        recording = ON
                    }

                    if (dataState.data.dB > 0) {
                        binding.progressBar.progress = dataState.data.dB.toInt()
                        binding.decibelsText.text = getString(R.string.main_dB, dataState.data.dB)
                    }
                    with(binding) {
                        recordButton.backgroundTintList =
                            ContextCompat.getColorStateList(activity as Activity, R.color.alert)
                        recordButton.setImageResource(R.drawable.ic_baseline_stop_24)
                        cancelRecordButton.show()
                        showRecordingLabels()
                    }
                }
                else -> {
                    // Do nothing
                }
            }
        }
    }

    /**
     * Remove observers from the [RecordingViewModel] LiveData objects.
     */
    private fun unsubscribeRecordingObservers() {
        recordingViewModel.measurementLiveData.removeObservers(this as LifecycleOwner)
    }

    companion object {
        private const val OFF = false
        private const val ON  = true
        private const val CHANNEL_ID = "Recording notifications"
        private const val FADE_LENGTH_MILLIS = 200L
    }
}