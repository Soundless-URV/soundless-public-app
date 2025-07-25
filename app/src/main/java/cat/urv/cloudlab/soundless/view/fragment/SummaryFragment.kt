package cat.urv.cloudlab.soundless.view.fragment

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import cat.urv.cloudlab.soundless.R
import cat.urv.cloudlab.soundless.databinding.FragmentSummaryBinding
import cat.urv.cloudlab.soundless.model.repository.health.fitbit.FitbitClient
import cat.urv.cloudlab.soundless.util.*
import cat.urv.cloudlab.soundless.util.auth.FitbitAuthenticator
import cat.urv.cloudlab.soundless.util.auth.PKCEAuthenticator
import cat.urv.cloudlab.soundless.util.datastate.RepositoryDataState
import cat.urv.cloudlab.soundless.util.datastate.TokenStatusDataState
import cat.urv.cloudlab.soundless.view.activity.MainActivity
import cat.urv.cloudlab.soundless.view.other.CustomDialog
import cat.urv.cloudlab.soundless.view.other.RecyclerViewAdapter
import cat.urv.cloudlab.soundless.viewmodel.RecordingMetadata
import cat.urv.cloudlab.soundless.viewmodel.repositoryviewmodel.RepositoryStateEvent
import cat.urv.cloudlab.soundless.viewmodel.repositoryviewmodel.RepositoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt

private typealias MetadataFilter = (RecordingMetadata) -> Boolean

@AndroidEntryPoint
class SummaryFragment : Fragment(), ItemClickListener {
    // View Binding
    private var _binding: FragmentSummaryBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var adapter: RecyclerViewAdapter

    // Filters for Metadata coming from repository
    private var dateFilter: MetadataFilter = { true }   // true means all are accepted
    private var locationFilter: MetadataFilter = { true }
    private var exposureFilter: MetadataFilter = { true }

    private val viewModel: RepositoryViewModel by viewModels()

    private lateinit var progressController: ProgressController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.setStateEvent(RepositoryStateEvent.IsRecordingEvent)
        _binding = FragmentSummaryBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = (activity as MainActivity).getSharedPreferences(
            getString(R.string.shared_preferences_filename),
            Context.MODE_PRIVATE
        )

        // View config
        binding.recyclerViewSummaries.layoutManager = LinearLayoutManager(activity as Activity)
        binding.filterDateBtn.setOnClickListener(FilterDateBtnListener())
        binding.btnCorrelateHealth.setOnClickListener {
            if (!isInternetAvailable(requireContext())) {
                showToast(getString(R.string.internet_not_available))
                return@setOnClickListener
            }
            val authenticator = FitbitAuthenticator()
            val healthAnalysisActive = authenticator.isAccessGranted(requireContext())
            if (!healthAnalysisActive) {
                showToast(getString(R.string.health_not_enabled))
                return@setOnClickListener
            }
            try {
                val fitbitUserID = authenticator.getUserID(requireContext())
                val fitbitAccessToken = authenticator.getAccessToken(requireContext())
                val fitbitClient = FitbitClient(fitbitUserID, fitbitAccessToken)
                viewModel.setStateEvent(RepositoryStateEvent.GetHealthDataEvent(fitbitClient,
                    false)
                )
            } catch (e: Exception) {
                when (e) {
                    is PKCEAuthenticator.NotAuthenticatedYetException -> {
                        showToast(getString(R.string.health_not_enabled))
                    }
                    is PKCEAuthenticator.InvalidTokenException,
                    is PKCEAuthenticator.TokenNeedsRefreshException -> {
                        var accessRenewed = false
                        CoroutineScope(Dispatchers.IO).launch {
                            authenticator.renewAccess(requireContext()).onEach { renewalState ->
                                when (renewalState) {
                                    is TokenStatusDataState.AccessRenewed -> {
                                        val fitbitUserID =
                                            authenticator.getUserID(requireContext())
                                        val fitbitAccessToken =
                                            authenticator.getAccessToken(requireContext())
                                        val fitbitClient =
                                            FitbitClient(fitbitUserID, fitbitAccessToken)
                                        viewModel.setStateEvent(
                                            RepositoryStateEvent
                                                .GetHealthDataEvent(fitbitClient,
                                                    false
                                                )
                                        )
                                        accessRenewed = true
                                    }
                                    is TokenStatusDataState.AccessNotRenewed -> {
                                        authenticator.resetAccess(requireContext())
                                    }
                                    else -> {}
                                }
                            }.collect()
                        }.invokeOnCompletion {
                            // We can't display a Toast outside of main thread
                            CoroutineScope(Dispatchers.Main).launch {
                                if (!accessRenewed) {
                                    showToast(getString(R.string.health_not_enabled))
                                }
                            }
                        }
                    } else -> {
                        showToast(getString(R.string.health_not_implemented))
                    }
                }
            }
        }

        // Bind the observers and fire the initial state event
        unsubscribeProgressObservers()
        subscribeObservers()
        viewModel.setStateEvent(RepositoryStateEvent.GetMetadataEvent)
    }

    private fun showToast(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    inner class FilterDateBtnListener: View.OnClickListener {
        private var active = false
        private val startDate: Calendar = Calendar.getInstance()
        private val endDate: Calendar = Calendar.getInstance()

        // Two fragments will be displayed: 1st for choosing Start date and 2nd for End date
        private val startDateFragment = DatePickerFragment(
                getString(R.string.start_date)
            ) { _, startYear, startMonth, startDay ->

            startDate.set(startYear, startMonth, startDay, 0, 0, 0)

            // Once initial date has been set, end date fragment is displayed
            val endDateFragment = DatePickerFragment(
                    getString(R.string.end_date),
                    startDate
                ) { _, endYear, endMonth, endDay ->

                endDate.set(endYear, endMonth, endDay, 23, 59, 59)

                // Now both dates have been selected so we apply the date filter
                if (isDateRangeValid(startDate, endDate)) {
                    resetDateFilter()
                    dateFilter = {
                        (startDate.time.time <= it.timestampEnd) and
                        (endDate.time.time   >= it.timestampEnd)
                    }
                    // Get data again and pass it by active filters
                    viewModel.setStateEvent(RepositoryStateEvent.GetMetadataEvent)
                    active = true
                    binding.filterDateBtn.backgroundTintList =
                        ContextCompat.getColorStateList(activity as Activity, R.color.primary)
                    binding.filterDateBtn.setTextColor(
                        ContextCompat.getColorStateList(activity as Activity, R.color.secondary_light)
                    )
                    binding.filterDateBtn.text = "$startDay/${startMonth + 1}-$endDay/${endMonth + 1}"
                } else {
                    Toast.makeText(
                        activity as Activity,
                        "Incorrect date range.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            endDateFragment.show(
                (activity as AppCompatActivity).supportFragmentManager,
                "endDate"
            )
        }

        override fun onClick(v: View?) {
            if (active) {
                resetDateFilter()
                viewModel.setStateEvent(RepositoryStateEvent.GetMetadataEvent)
                active = false
                binding.filterDateBtn.backgroundTintList =
                    ContextCompat.getColorStateList(activity as Activity, R.color.secondary)
                binding.filterDateBtn.setTextColor(
                    ContextCompat.getColorStateList(activity as Activity, R.color.primary)
                )
                binding.filterDateBtn.text = getString(R.string.filter_date_btn_text)
            } else {
                startDateFragment.show(
                    (activity as AppCompatActivity).supportFragmentManager,
                    "startDate"
                )
            }
        }

        private fun isDateRangeValid(startDate: Calendar, endDate: Calendar): Boolean {
            return startDate <= endDate
        }
    }

    inner class ProgressController(private var steps: UInt) {
        private var stepsDone: UInt = 0u
        var isComplete = false
            private set

        fun performStep() {
            if (stepsDone >= steps) isComplete = true
            else stepsDone++
        }

        /**
         * From 0 to 100.
         */
        fun getProgress(): Int {
            if (steps == 0u) return 100
            return (stepsDone.toFloat() / steps.toFloat() * 100).roundToInt()
        }
    }

    /**
     * This sets up the observers so that changes in the ViewModel are made visible in the UI.
     * Following MVVM/MVI pattern.
     */
    private fun subscribeObservers() {
        viewModel.repositoryMetadataLiveData.observe(this as LifecycleOwner) { dataState ->
            when (dataState) {
                is RepositoryDataState.Success<List<RecordingMetadata>> -> {
                    val data = dataState.data
                        .filter(dateFilter)
                        .filter(locationFilter)
                        .filter(exposureFilter)
                    adapter = RecyclerViewAdapter(data)
                    adapter.setOnClickListener(this)
                    binding.recyclerViewSummaries.adapter = adapter
                }
                is RepositoryDataState.Error -> {
                    Toast.makeText(
                        activity as Activity,
                        "Error loading data. Try again later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    // Do nothing
                }
            }
        }
        viewModel.progressLiveData.observe(this as LifecycleOwner) { dataState ->
            when (dataState) {
                is RepositoryDataState.SetSteps -> {
                    progressController = ProgressController(dataState.steps.toUInt())
                    with(binding) {
                        progressCircularBackground.visibility = View.VISIBLE
                        btnCorrelateHealth.visibility = View.INVISIBLE
                        progressCircular.visibility = View.VISIBLE
                        progressCircular.progress = 0
                    }
                }
                is RepositoryDataState.Progress -> {
                    if (this::progressController.isInitialized) {
                        progressController.performStep()
                        with(binding) {
                            progressCircular.progress = progressController.getProgress()
                        }
                    }
                }
                is RepositoryDataState.Success -> {
                    with(binding) {
                        progressCircular.visibility = View.INVISIBLE
                        btnCorrelateHealth.visibility = View.VISIBLE
                        progressCircularBackground.visibility = View.INVISIBLE
                    }

                    CustomDialog.showCustomDialog(requireActivity(), tag = UUID.randomUUID().toString()) {
                        setMessage(R.string.health_data_imported)
                        setPositiveButton(R.string.ok) { _, _ -> }
                    }
                    viewModel.setStateEvent(RepositoryStateEvent.GetMetadataEvent)
                }
                is RepositoryDataState.NoState -> {
                    with(binding) {
                        progressCircular.visibility = View.INVISIBLE
                        btnCorrelateHealth.visibility = View.VISIBLE
                        progressCircularBackground.visibility = View.INVISIBLE
                    }
                }
                else -> {
                    // Do nothing
                }
            }
        }
    }

    /**
     * Listener that describes what will happen once an item is clicked.
     * @param view Recycler view
     * @param position Position of the item in the adapter
     */
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onItemClick(view: View, position: Int) {
        val item = adapter.getItem(position)
        val navHostFragment = requireActivity()
            .supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment)
                as NavHostFragment
        val navController = navHostFragment.navController

        // If recording is currently going on, item click means going back to main fragment
        if (item.currentlyActive) {
            val actionBackToMain = SummaryFragmentDirections.actionNavReportsToNavMain()
            navController.navigate(actionBackToMain)

        // If recording was ended in the past, item click means going to a fragment where info
        // about the recording is shown
        } else {
            val actionGoToRecordingInfo =
                SummaryFragmentDirections.actionNavReportsToNavRecordingInfo(item.uuid)
            navController.navigate(actionGoToRecordingInfo)
        }
    }

    /**
     * Reset predicate (filter) to its initial state. It will return true for all summaries.
     */
    private fun resetDateFilter() {
        dateFilter = { true }
    }

    /**
     * Reset all predicates (filters) to their initial state. They will return true for all summaries.
     */
    private fun resetAllFilters() {
        dateFilter = { true }
        locationFilter = { true }
        exposureFilter = { true }
    }

    /**
     * Given several filters for an [RecordingMetadata], returns a single filter that returns
     * true if ANY of the filters are true when applied to that [RecordingMetadata].
     *
     * @param filters A variable number of filters to be joined into one
     */
    private fun joinFilters(vararg filters: MetadataFilter): MetadataFilter {
        val joint: MetadataFilter = { summary ->
            var isAnyFilterTrue = false
            for (filter in filters) {
                isAnyFilterTrue = isAnyFilterTrue or filter(summary)
                if (isAnyFilterTrue) break
            }
            isAnyFilterTrue
        }
        return joint
    }

    /**
     * Alias to be able to join filters with syntax: filter1 + filter2 + filter3.
     */
    private operator fun MetadataFilter.plus(filter: MetadataFilter): MetadataFilter {
        return joinFilters(this, filter)
    }

    /**
     * This reverts a filter that had been applied previously.
     * Example: we were filtering (Innocuous + Tolerable) dB filters, now we just want (Tolerable).
     * To do that, we are conceptually doing (Innocuous + Tolerable - Innocuous).
     *
     * Notice this is not really the purest most performant approach, but filters are only applied
     * once, i.e. we are not doing
     *      summaries = getSummaries()
     *      summaries = Innocuous(summaries)
     *      summaries = Tolerable(summaries)
     *      summaries = -Innocuous(summaries)
     *
     * but rather,
     *      summaries = getSummaries()
     *      summaries = (Innocuous + Tolerable - Innocuous)(summaries)
     *
     * which is not extremely bad considering we will be working with a small-ish dataset (only
     * summaries by current user).
     *
     * @param filterToRevert Filter we no longer want to be applied
     * @param allAppliedFilters Whole set of filters
     */
    private fun revertFilter(
        allAppliedFilters: MetadataFilter,
        filterToRevert: MetadataFilter
    ): MetadataFilter {
        return { summary -> !filterToRevert(summary) and allAppliedFilters(summary) }
    }

    /**
     * Alias to be able to revert filters with syntax: allAppliedFilters - filterToRevert.
     */
    private operator fun MetadataFilter.minus(filterToRevert: MetadataFilter): MetadataFilter {
        return revertFilter(this, filterToRevert)
    }

    /**
     * Dialog to choose start and end dates.
     *
     * @param title Title displayed on top of the dialog
     * @param minDate If not null, minimum day that you can pick in the calendar
     * @param listener Event triggered when picking a date
     */
    class DatePickerFragment(
        private val title: String? = null,
        private val minDate: Calendar? = null,
        private val listener: DatePickerDialog.OnDateSetListener? = null
    ) : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            if (title == null)
                dismiss()

            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(
                activity as Activity,
                listener,
                year, month, day
            )
            datePickerDialog.setTitle(title)
            if (minDate != null) {
                datePickerDialog.datePicker.minDate = minDate.timeInMillis
            }
            return datePickerDialog
        }
    }

    private fun unsubscribeProgressObservers() {
        viewModel.progressLiveData.removeObservers(this as LifecycleOwner)
    }
}