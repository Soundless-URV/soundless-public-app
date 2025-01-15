package cat.urv.cloudlab.soundless.view.fragment

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import cat.urv.cloudlab.soundless.R
import cat.urv.cloudlab.soundless.databinding.FragmentTutorialThirdBinding
import cat.urv.cloudlab.soundless.view.activity.MainActivity
import cat.urv.cloudlab.soundless.view.other.CustomDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TutorialThirdFragment : Fragment() {
    // View Binding
    private var _binding: FragmentTutorialThirdBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTutorialThirdBinding.inflate(layoutInflater)

        (activity as MainActivity).hideToolbar()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val navHostFragment = requireActivity().supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Skip tutorial if have already skipped it before
        val sharedPreferences = (activity as MainActivity).getSharedPreferences(
            getString(R.string.shared_preferences_filename),
            Context.MODE_PRIVATE
        )
        val skip = sharedPreferences.getBoolean(getString(R.string.key_skipped_tutorial), false)
        val actionSkipToMain = TutorialThirdFragmentDirections.actionNavTutorialThirdToNavMain()
        if (skip) navController.navigate(actionSkipToMain)

        with(binding) {
            nextButton.setOnClickListener {
                navController.navigate(actionSkipToMain)
            }
            skipButton.setOnClickListener {
                CustomDialog.showCustomDialog(requireActivity()) {
                    setMessage(getString(R.string.skip_tutorial))
                    setPositiveButton(getString(R.string.skip_forever)) { _, _ ->
                        val editor = sharedPreferences.edit()
                        editor.putBoolean(getString(R.string.key_skipped_tutorial), true)
                        editor.apply()
                        navController.navigate(actionSkipToMain)
                    }
                    setNegativeButton(getString(R.string.skip_once)) { _, _ ->
                        navController.navigate(actionSkipToMain)
                    }
                }
            }
        }
        return binding.root
    }

}