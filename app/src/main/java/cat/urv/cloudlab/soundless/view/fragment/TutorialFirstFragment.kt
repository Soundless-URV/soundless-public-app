package cat.urv.cloudlab.soundless.view.fragment

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import cat.urv.cloudlab.soundless.R
import cat.urv.cloudlab.soundless.databinding.FragmentTutorialFirstBinding
import cat.urv.cloudlab.soundless.util.permissions.PermissionsHandler
import cat.urv.cloudlab.soundless.view.activity.MainActivity
import cat.urv.cloudlab.soundless.view.other.CustomDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TutorialFirstFragment : Fragment() {
    // View Binding
    private var _binding: FragmentTutorialFirstBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTutorialFirstBinding.inflate(layoutInflater)

        (activity as MainActivity).hideToolbar()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val navHostFragment = requireActivity().supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Ask for permissions on startup
        val recordingPermissionsGranted =
            PermissionsHandler.checkSoundlessPermissions(activity as Activity)
        if (!recordingPermissionsGranted)
            PermissionsHandler.requestSoundlessPermissions(activity as Activity)

        // Skip tutorial if have already skipped it before
        val sharedPreferences = (activity as MainActivity).getSharedPreferences(
            getString(R.string.shared_preferences_filename),
            MODE_PRIVATE
        )
        val skip = sharedPreferences.getBoolean(getString(R.string.key_skipped_tutorial), false)
        val actionSkipToMain = TutorialFirstFragmentDirections.actionNavTutorialFirstToNavMain()
        if (skip) navController.navigate(actionSkipToMain)

        with(binding) {
            tutorialFirstStepDesc.text = HtmlCompat.fromHtml(
                getString(R.string.tutorial_text_1_2),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            tutorialFirstStepDesc.linksClickable = true
            tutorialFirstStepDesc.movementMethod = LinkMovementMethod.getInstance()
            nextButton.setOnClickListener {
                val actionNextTutorial = TutorialFirstFragmentDirections
                    .actionNavTutorialFirstToNavTutorialSecond()
                navController.navigate(actionNextTutorial)
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