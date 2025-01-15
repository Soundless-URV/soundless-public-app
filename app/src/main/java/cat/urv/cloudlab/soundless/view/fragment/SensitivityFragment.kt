package cat.urv.cloudlab.soundless.view.fragment

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import cat.urv.cloudlab.soundless.R
import cat.urv.cloudlab.soundless.databinding.FragmentSensitivityBinding
import cat.urv.cloudlab.soundless.model.repository.MainRepository
import cat.urv.cloudlab.soundless.view.other.RecyclerViewSensitivityAdapter
import cat.urv.cloudlab.soundless.viewmodel.sensitivityviewmodel.SensitivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class SensitivityFragment : Fragment() {
    private var _binding: FragmentSensitivityBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var adapter: RecyclerViewSensitivityAdapter



    @Inject lateinit var mainRepository: MainRepository

    private val viewModel: SensitivityViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSensitivityBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.recyclerViewSensitivities.layoutManager = LinearLayoutManager(activity as Activity)
        adapter = RecyclerViewSensitivityAdapter()
        binding.recyclerViewSensitivities.adapter = adapter

        // Fetch all data
        viewModel.extractSensitivity()

        unsubscribeObservers()
        subscribeObservers()
    }

    private fun subscribeObservers() {
        viewModel.sensitivityLevels.observe(this as LifecycleOwner) { data ->
            when (data) {
                is SensitivityViewModel.SensitivityResult -> {
                    adapter = RecyclerViewSensitivityAdapter(data.heartSensitivity.format(2), data.sleepSensitivity.format(2), data.combinedSensitivity.format(2))
                    binding.recyclerViewSensitivities.adapter = adapter

                    binding.extractedValueText.text = getString(R.string.number_recordings_extracted, data.numberTotalOfRecordings)

                    // Calculate mean sensitivity to show semaphore
                    val sensitivityValues = listOf(data.heartSensitivity, data.sleepSensitivity, data.combinedSensitivity).filterNot { it.isNaN() }
                    val meanSensitivity = sensitivityValues.average().takeIf { !it.isNaN() }

                    when {
                        meanSensitivity == null -> {
                            binding.semaphoreIcon1Text.visibility = View.GONE
                            binding.semaphoreText.visibility = View.GONE
                            binding.semaphoreIcon2Text.visibility = View.GONE
                        }
                        meanSensitivity < 42.5 -> {
                            binding.semaphoreIcon1Text.text = getString(R.string.red_semaphore)
                            binding.semaphoreText.text = getString(R.string.high_sensitivity)
                            binding.semaphoreIcon2Text.text = getString(R.string.red_semaphore)
                        }
                        meanSensitivity < 47.5 -> {
                            binding.semaphoreIcon1Text.text = getString(R.string.yellow_semaphore)
                            binding.semaphoreText.text = getString(R.string.average_sensitivity)
                            binding.semaphoreIcon2Text.text = getString(R.string.yellow_semaphore)
                        }
                        else -> {
                            binding.semaphoreIcon1Text.text = getString(R.string.green_semaphore)
                            binding.semaphoreText.text = getString(R.string.low_sensitivity)
                            binding.semaphoreIcon2Text.text = getString(R.string.green_semaphore)
                        }
                    }

                    // Calculate incidence of noise, heart and sleep per night
                    if (data.numberTotalOfRecordings != 0) {
                        binding.noiseIncidenceValueText.text = getString(R.string.percentage, data.numberNightsNoise * 100 / data.numberTotalOfRecordings)
                        binding.heartIncidenceValueText.text = getString(R.string.percentage, data.numberNightsHeart * 100 / data.numberTotalOfRecordings)
                        binding.sleepIncidenceValueText.text = getString(R.string.percentage, data.numberNightsSleep * 100 / data.numberTotalOfRecordings)
                    } else {
                        binding.sensitivityLayout.visibility = View.GONE
                        binding.noiseIncidenceLayout.visibility = View.GONE
                        binding.heartIncidenceLayout.visibility = View.GONE
                        binding.sleepIncidenceLayout.visibility = View.GONE
                        binding.enoughDataText.visibility = View.VISIBLE
                    }
                }

                else -> {
                    Toast.makeText(
                        activity as Activity,
                        "Error loading data. Try again later.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun unsubscribeObservers() {
        viewModel.allRecordingsData.removeObservers(this as LifecycleOwner)
    }

    private fun Double.format(scale: Int) = "%.${scale}f".format(this)
}