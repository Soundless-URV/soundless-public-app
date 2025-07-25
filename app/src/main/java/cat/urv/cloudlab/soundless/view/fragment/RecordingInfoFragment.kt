package cat.urv.cloudlab.soundless.view.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import cat.urv.cloudlab.soundless.R
import cat.urv.cloudlab.soundless.databinding.FragmentRecordingInfoBinding
import cat.urv.cloudlab.soundless.model.repository.MainRepository
import cat.urv.cloudlab.soundless.util.datastate.RepositoryDataState
import cat.urv.cloudlab.soundless.view.other.CustomDialog
import cat.urv.cloudlab.soundless.viewmodel.Measurement
import cat.urv.cloudlab.soundless.viewmodel.Timestamp
import cat.urv.cloudlab.soundless.viewmodel.repositoryviewmodel.RepositoryStateEvent
import cat.urv.cloudlab.soundless.viewmodel.repositoryviewmodel.RepositoryViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong


@AndroidEntryPoint
class RecordingInfoFragment : Fragment() {
    private var _binding: FragmentRecordingInfoBinding? = null
    private val binding
        get() = _binding!!

    @Inject lateinit var mainRepository: MainRepository
    private val viewModel: RepositoryViewModel by viewModels()

    // Get receiving arguments from navigation component
    private val args: RecordingInfoFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uuid = args.uuid
        CoroutineScope(Dispatchers.Main).launch {
            mainRepository.getMeasurementsForRecording(uuid).onEach { state ->
                when (state) {
                    is RepositoryDataState.Success -> {
                        generateInfoTable(state.data)
                        generateChart(state.data)
                    }
                    is RepositoryDataState.Error -> {
                        displayErrorMessage()
                    }
                    else -> {}
                }
            }.collect()
        }

        with(binding){
            deleteRecordingButton.setOnClickListener {
                CustomDialog.showCustomDialog(requireActivity()) {
                    setTitle(R.string.delete_recording_question)
                    setMessage(R.string.delete_recording_message)
                    setPositiveButton(R.string.delete_recordings_confirm) { _, _ ->
                        deleteRecordingLocally(uuid)
                        CustomDialog.showCustomDialog(requireActivity()) {
                            setMessage(R.string.delete_recording_successful)
                            // Refresh Fragment using Navigation component in order to clean up all
                            // Dialogs and avoid invalid state errors. There should be better
                            // alternatives to this. TODO look into why app crashes sometimes
                            refreshFragment()
                        }

                    }
                    setNegativeButton(R.string.delete_all_data_cancel) { _, _ ->
                    }
                }
            }
        }
    }

    private fun displayErrorMessage() {
        with(binding) {
            contentScrollView.visibility = View.INVISIBLE
            notEnoughDataTextView.visibility = View.VISIBLE
            notEnoughDataTextView.text = "Error!"
            notEnoughDataTextView.setTextColor(Color.RED)
        }
    }

    /**
     * Launch a navigation action to the Fragment itself.
     */
    private fun refreshFragment() {
        val navHostFragment = requireActivity().supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(R.id.action_nav_recording_info_to_nav_reports)
    }

    private fun deleteRecordingLocally(uuid: String) {
        viewModel.setStateEvent(RepositoryStateEvent.DeleteOneRecordingEvent(uuid))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingInfoBinding.inflate(layoutInflater)
        return binding.root
    }

    sealed class Formatter: IndexAxisValueFormatter() {

        class ShortDateXAxisValueFormatter(private val initialTimestamp: Timestamp): Formatter() {
            private val pattern = "mm:ss"
            private val sdf = SimpleDateFormat(pattern)
            init {
                sdf.timeZone = TimeZone.getTimeZone(ZoneId.of("Europe/Madrid"))
            }
            override fun getFormattedValue(value: Float): String {
                return sdf.format(value * 1000 + initialTimestamp)
            }
        }

        class LongDateXAxisValueFormatter(private val initialTimestamp: Timestamp): Formatter() {
            private val pattern = "HH:mm"
            private val sdf = SimpleDateFormat(pattern)
            init {
                sdf.timeZone = TimeZone.getTimeZone(ZoneId.of("Europe/Madrid"))
            }
            override fun getFormattedValue(value: Float): String {
                return sdf.format(value * 1000 + initialTimestamp)
            }
        }

    }

    private fun generateChart(measurements: List<Measurement>) {
        val initialTimestamp = measurements.first().timestamp
        measurements.forEach { it.timestamp = (it.timestamp - initialTimestamp) / 1000 }

        if (measurements.size < 4) {
            with(binding) {
                tableLayout.visibility = View.GONE
                notEnoughDataTextView.visibility = View.VISIBLE
            }
        } else {
            with(binding) {
                tableLayout.visibility = View.VISIBLE
                notEnoughDataTextView.visibility = View.GONE

                val combinedData = CombinedData()
                val chartList: MutableList<ILineDataSet> = ArrayList()

                // dB chart
                var dBEntries = getDBEntriesFromMeasurements(measurements)

                // If there are only 1 value
                if (dBEntries.size == 1){
                    val anotherValue : Entry = dBEntries[0].copy()
                    anotherValue.x = 2.0F
                    dBEntries = dBEntries + anotherValue
                }

                val dBLineDataSet = LineDataSet(dBEntries, getString(R.string.dB))
                dBLineDataSet.color = requireContext().getColor(R.color.primary_dark)
                dBLineDataSet.valueTextSize = 0f  // No labels
                dBLineDataSet.lineWidth = 1.6f
                dBLineDataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER  // Line: round edges
                dBLineDataSet.setDrawCircles(false)
                chartList.add(dBLineDataSet)

                // Heart rate chart
                if (measurements.any { it.heartRate != -1 }) {
                    val validTimestamps = dBEntries.map { it.x.roundToLong() }.toHashSet()
                    val onlyMeasurementsInDBGraph = measurements.filter { measurement ->
                        measurement.timestamp in validTimestamps
                    }
                    var heartRateEntries =
                        getHeartRateEntriesFromMeasurements(onlyMeasurementsInDBGraph)

                    // If there are only 1 value
                    if (heartRateEntries.size == 1){
                        val anotherValue : Entry = heartRateEntries[0].copy()
                        anotherValue.x = 2.0F
                        heartRateEntries = heartRateEntries + anotherValue
                    }

                    val heartRateLineDataSet =
                        LineDataSet(heartRateEntries, getString(R.string.heart_rate))
                    heartRateLineDataSet.color = requireContext().getColor(R.color.alert)
                    heartRateLineDataSet.valueTextSize = 0f  // No labels
                    heartRateLineDataSet.lineWidth = 1.6f
                    heartRateLineDataSet.mode =
                        LineDataSet.Mode.HORIZONTAL_BEZIER // Line: round edges
                    heartRateLineDataSet.setDrawCircles(false)
                    chartList.add(heartRateLineDataSet)
                }

                // UI tweaks
                val labelFormatter = Formatter.LongDateXAxisValueFormatter(initialTimestamp)
                chart.xAxis.granularity = 1f
                chart.xAxis.valueFormatter = labelFormatter
                chart.description.isEnabled = false
                chart.xAxis.isEnabled = true
                chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                chart.axisRight.isEnabled = false
                chart.setTouchEnabled(false)  // Block touch gestures
                chart.isHighlightPerTapEnabled = false
                chart.isHighlightPerDragEnabled = false

                // Finally render the chart
                val lineData = LineData(chartList)
                combinedData.setData(lineData)
                chart.data = combinedData
                chart.invalidate()
            }
        }
    }

    private fun getDBEntriesFromMeasurements(measurements: List<Measurement>): List<Entry> {
        val dBPairs = measurements.map {
            it.timestamp.toFloat() to it.dB
        }
        val dBEntries: MutableList<Entry> = ArrayList()
        var previousDB = 0F
        val variabilityInDB =
            if (isLongRecording(measurements)) DB_VARIABILITY_BIG
            else DB_VARIABILITY_SMALL
        dBPairs.forEach { pair ->
            val currentDB = pair.second
            if ((abs(currentDB - previousDB) >= variabilityInDB)) {
                dBEntries.add(Entry(pair.first, pair.second))
            }
            previousDB = currentDB
        }
        return dBEntries
    }

    private fun getHeartRateEntriesFromMeasurements(measurements: List<Measurement>): List<Entry> {
        if (measurements.isEmpty()) return listOf()
        val heartRateEntries = measurements.map {
            Entry(it.timestamp.toFloat(), it.heartRate.toFloat())
        }
        return heartRateEntries
    }

    private fun isLongRecording(data: List<Measurement>) =
        (data.last().timestamp - data.first().timestamp) / SECONDS_IN_HOUR >= NUM_HOURS_LONG_RECORDING


    private fun generateInfoTable(data: List<Measurement>) {
        if (data.size >= 4) {
            with(binding) {

                // Start date
                val simpleDateFormat = SimpleDateFormat("EEE dd/MM/yyyy HH:mm:ss")
                infoStartDateValue.text = simpleDateFormat.format(data.first().timestamp)

                // End date
                infoEndDateValue.text = simpleDateFormat.format(data.last().timestamp)

                // Average heart rate
                val filteredDataHeartRate = data
                    .filter { it.heartRate > 0 }
                infoHeartRateAvgValue.text =
                    if (filteredDataHeartRate.isEmpty()) {
                        getString(R.string.not_available)
                    } else {
                        val averageHeartRate = filteredDataHeartRate
                            .map { it.heartRate }
                            .average()
                        if (!averageHeartRate.isNaN()) "${averageHeartRate.roundToInt()} bpm"
                        else getString(R.string.not_available)
                    }

                // Heart rate peak
                val peakHeartRate = data.maxOf { it.heartRate }
                infoHeartRatePeakValue.text =
                    if (peakHeartRate < 0) getString(R.string.not_available)
                    else "$peakHeartRate bpm"

                // Sleep changes
                var changes = 0
                var allUnavailable = true
                data.forEachIndexed { i, _ ->
                    if (data[i].sleepStage != -1) allUnavailable = false
                    if (i != 0 && data[i - 1].sleepStage != data[i].sleepStage) changes++
                }
                infoSleepChangesValue.text =
                    if (allUnavailable) getString(R.string.not_available)
                    else changes.toString()
            }
        }
    }

    companion object {
        const val DB_VARIABILITY_BIG = 5.0
        const val DB_VARIABILITY_SMALL = 1.5
        const val SECONDS_IN_HOUR = 3600
        const val NUM_HOURS_LONG_RECORDING = 4
    }

}



