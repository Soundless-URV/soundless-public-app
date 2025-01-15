package cat.urv.cloudlab.soundless.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.navArgs
import cat.urv.cloudlab.soundless.R
import cat.urv.cloudlab.soundless.databinding.FragmentMapBinding
import cat.urv.cloudlab.soundless.util.DeviceLocation
import cat.urv.cloudlab.soundless.util.datastate.MapDataState
import cat.urv.cloudlab.soundless.view.other.CustomDialog
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import cat.urv.cloudlab.soundless.viewmodel.mapviewmodel.MapViewModel
import cat.urv.cloudlab.soundless.viewmodel.mapviewmodel.MapViewModel.MapDate
import cat.urv.cloudlab.soundless.viewmodel.mapviewmodel.MapViewModel.MapStateEvent
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@AndroidEntryPoint
class MapFragment : Fragment() {
    @Inject lateinit var deviceLocation: DeviceLocation
    private var _binding: FragmentMapBinding? = null
    private val binding
        get() = _binding!!

    private var index = 0
    private val listDates: Array<String> by lazy { arrayOf(
        getString(R.string.map_text_yesterday),
        getString(R.string.map_text_week),
        getString(R.string.map_text_month)
    )}
    private val viewModel: MapViewModel by viewModels({ requireParentFragment() })
    private val args: MapFragmentArgs by navArgs()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        unsubscribeObservers()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectDateButton = binding.openAlertDialogButton

        // Select the correct name of location
        val location = when (args.location) {
            getString(R.string.tarragona_province) -> "Tarragona_province"
            else -> args.location
        }

        // Initial text
        binding.selectedItemPreview.text = getString(R.string.map_text, args.location, "", "")

        selectDateButton.setOnClickListener {
            CustomDialog.showCustomDialog(requireActivity()) {
                setTitle(getString(R.string.select_date))

                setSingleChoiceItems(listDates, NO_CHECKED_ITEM) { dialog, which ->
                    // Reset text
                    binding.selectedItemPreview.text = getString(R.string.map_text, args.location, "", "")

                    index = which
                    dialog.dismiss()
                    val mapDate = getMapDateFromIndex(which)
                    requestMap(mapDate, location)
                }
                setNegativeButton(getString(R.string.cancel)) { _, _ ->}
            }
        }

        subscribeObservers(location)

        // Render a map initially, yesterday by default
        requestMap(MapDate.MAP_OF_YESTERDAY, location)
    }

    private fun requestMap(mapDate: MapDate, location: String) {
        viewModel.setStateEvent(MapStateEvent.GetMapFile(mapDate, location))
    }

    private fun getMapDateFromIndex(index: Int): MapDate {
        return when (index) {
            0 -> MapDate.MAP_OF_YESTERDAY
            1 -> MapDate.MAP_OF_LAST_WEEK
            2 -> MapDate.MAP_OF_LAST_MONTH
            else -> MapDate.DEFAULT
        }
    }

    private fun showNewMapAvailableDialog(location: String) {
        // Reset text
        binding.selectedItemPreview.text = getString(R.string.map_text, args.location, "", "")

        CustomDialog.showCustomDialog(requireActivity()) {
            setTitle(R.string.map_dialog_title)

            setPositiveButton(getString(R.string.ok)) { _, _ ->
                viewModel.setStateEvent(
                    MapStateEvent.DownloadNewFile(getMapDateFromIndex(index), location)
                )
            }

            setNegativeButton(getString(R.string.cancel)) { _, _ ->
                viewModel.setStateEvent(
                    MapStateEvent.CancelDownload(true)
                )
            }
        }
    }

    private fun subscribeObservers(location: String) {
        viewModel.fileUpdatesLiveData.observe(this as LifecycleOwner) { dataState ->
            when (dataState) {
                is MapDataState.MapUpdated -> {
                    binding.webView.visibility = View.VISIBLE
                    val lastModified = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(dataState.file.lastModified()),
                        ZoneId.systemDefault()
                    )
                    val ddMMyyyyFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                    binding.selectedItemPreview.text = getString(
                        R.string.map_text,
                        args.location,
                        "(" + listDates[index] + ")",
                        lastModified.format(ddMMyyyyFormatter)
                    )
                    renderWebView(dataState.file)
                }
                is MapDataState.NoMapAvailable -> {
                    binding.webView.visibility = View.INVISIBLE
                }
                else -> {}
            }
        }
        viewModel.newFileAvailableLiveData.observe(this as LifecycleOwner) { dataState ->
            if (dataState is MapDataState.NewFileAvailable) {
                showNewMapAvailableDialog(location)
            }
        }
    }

    private fun unsubscribeObservers() {
        viewModel.fileUpdatesLiveData.removeObservers(this as LifecycleOwner)
        viewModel.newFileAvailableLiveData.removeObservers(this as LifecycleOwner)
    }

    // Method to create and show the webView
    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    fun renderWebView(file: File) {
        val filepath = file.absolutePath
        val webView = binding.webView

        webView.settings.javaScriptEnabled = true
        webView.settings.allowFileAccess = true
        webView.setInitialScale(250)
        webView.scrollTo(200, 125)

        // Disallow the touch request for parent scroll on touch of child view
        webView.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        webView.loadUrl("file:///$filepath")
    }

    companion object {
        const val NO_CHECKED_ITEM = -1
    }

}