package cat.urv.cloudlab.soundless.view.fragment

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import cat.urv.cloudlab.soundless.R
import cat.urv.cloudlab.soundless.databinding.FragmentSettingsBinding
import cat.urv.cloudlab.soundless.util.auth.FitbitAuthenticator
import cat.urv.cloudlab.soundless.util.auth.PKCEAuthenticator
import cat.urv.cloudlab.soundless.view.activity.MainActivity
import cat.urv.cloudlab.soundless.view.other.CustomDialog
import cat.urv.cloudlab.soundless.viewmodel.repositoryviewmodel.RepositoryStateEvent
import cat.urv.cloudlab.soundless.viewmodel.repositoryviewmodel.RepositoryViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*


@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private val authenticator = FitbitAuthenticator()

    private var _binding: FragmentSettingsBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: RepositoryViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = (activity as MainActivity).getSharedPreferences(
            getString(R.string.shared_preferences_filename),
            Context.MODE_PRIVATE
        )
        val sharedPreferencesEditor = sharedPreferences.edit()

        with(binding) {
            val skippedTutorial = sharedPreferences.getBoolean(getString(R.string.key_skipped_tutorial), false)
            switchTutorialActive.isChecked = !skippedTutorial
            switchTutorialActive.setOnCheckedChangeListener { _, isChecked ->
                sharedPreferencesEditor.putBoolean(getString(R.string.key_skipped_tutorial), !isChecked).apply()
            }

            btnFitbitConnect.setOnClickListener {
                val isAccessGranted = authenticator.isAccessGranted(requireContext())
                if (!isAccessGranted) {
                    launchFitbitAuthProcess()
                } else {
                    CustomDialog.showCustomDialog(requireActivity()) {
                        setMessage(R.string.update_fitbit_ask)
                        setPositiveButton(R.string.yes) { _, _ ->
                            launchFitbitAuthProcess()
                        }
                        setNegativeButton(R.string.no) { _, _ ->
                            refreshFragment()
                        }
                    }
                }
            }

            val input = EditText(requireContext())
            input.inputType = InputType.TYPE_CLASS_TEXT
            input.hint = getString(R.string.paste_here)
            val container = FrameLayout(requireActivity())
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = resources.getDimensionPixelSize(R.dimen.dialog_margin)
            params.marginStart = resources.getDimensionPixelSize(R.dimen.dialog_margin)
            input.layoutParams = params
            container.addView(input)

            btnFitbitCode.setOnClickListener {
                val dialogMessage: String =
                    try {
                        val currentCode = authenticator.getAuthorizationCode(requireContext())
                        val currentCodeAbbr = currentCode.substring(0..6) + "..."
                        getString(
                            R.string.current_fitbit_code,
                            currentCodeAbbr
                        )
                    } catch (e: Exception) {
                        when (e) {
                            is PKCEAuthenticator.InvalidTokenException,
                            is PKCEAuthenticator.NotAuthenticatedYetException -> {
                                getString(R.string.no_current_fitbit_code)
                            }
                            else -> {
                                getString(R.string.no_current_fitbit_code)
                            }
                        }
                    }

                CustomDialog.showCustomDialog(requireActivity()) {
                    setTitle(R.string.insert_fitbit_code)
                    setMessage(dialogMessage)
                    if (container.parent != null) {
                        (container.parent as ViewGroup).removeView(container)
                    }
                    setView(container)
                    setPositiveButton(R.string.ok) { _, _ ->
                        // Continue the OAuth flow by asking for access and refresh tokens
                        val fitbitAuthCode = input.text.toString()

                        if (fitbitAuthCode.isNotEmpty()
                            && fitbitAuthCode.isNotBlank()
                            && fitbitAuthCode.length > 2
                        ) {
                            CoroutineScope(Dispatchers.Default).launch {
                                authenticator.saveAuthorizationCode(requireContext(), fitbitAuthCode)
                                authenticator.requestFitbitAccess(
                                    requireContext(),
                                    fitbitAuthCode
                                )
                            }
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.code_inserted),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        refreshFragment()
                    }
                    setNegativeButton(R.string.cancel) { _, _ ->
                        refreshFragment()
                    }
                }
            }

            btnDeleteRecordings.setOnClickListener {
                CustomDialog.showCustomDialog(requireActivity()) {
                    setTitle(R.string.delete_recordings_question)
                    setMessage(R.string.delete_recordings_message)
                    setPositiveButton(R.string.delete_recordings_confirm) { _, _ ->
                        deleteRecordingsLocally()
                        try {
                            recreateFirebaseUser()
                        } catch (e: NotAuthenticatedToFirebase) {
                            // If not authenticated, do not recreate user
                        } catch (e: InvalidFirebaseAuthTransaction) {
                            // TODO handle user recreation in the future (e.g. SQLite table for handling pending recreations)
                        }
                        CustomDialog.showCustomDialog(requireActivity()) {
                            setMessage(R.string.delete_recordings_successful)
                            // Refresh Fragment using Navigation component in order to clean up all
                            // Dialogs and avoid invalid state errors. There should be better
                            // alternatives to this. TODO look into why app crashes sometimes
                            refreshFragment()
                        }
                    }
                    setNegativeButton(R.string.delete_all_data_cancel) { _, _ ->
                        refreshFragment()
                    }
                }
            }
        }

        // If arguments contain Fitbit authorization code, then continue the OAuth flow by asking
        // for access and refresh tokens
        val fitbitAuthCode = arguments?.getString("fitbitAuthCode")
        arguments?.clear()

        // This sometimes gets called when it shouldn't, as fitbitAuthCode may contain something
        // from old requests, but it does not matter: API will return an HTTP 400 and that's it
        if (fitbitAuthCode != null
            && fitbitAuthCode.isNotEmpty()
            && fitbitAuthCode.isNotBlank()
            && fitbitAuthCode.length > 2
        ) {
            CoroutineScope(Dispatchers.Default).launch {
                authenticator.saveAuthorizationCode(requireContext(), fitbitAuthCode)
                authenticator.requestFitbitAccess(
                    requireContext(),
                    fitbitAuthCode
                )
            }
        }
    }

    private fun launchFitbitAuthProcess() {
        val authenticator: PKCEAuthenticator = FitbitAuthenticator()
        val uri = authenticator.generateAuthorizationURI(requireContext())
        val customIntent = CustomTabsIntent.Builder().build()
        customIntent.launchUrl(requireContext(), uri)
    }

    /**
     * Launch a navigation action to the Fragment itself.
     */
    private fun refreshFragment() {
        val navHostFragment = requireActivity().supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(R.id.action_nav_settings_self)
    }

    private fun deleteRecordingsLocally() {
        viewModel.setStateEvent(RepositoryStateEvent.DeleteLocalMetadataEvent)
    }

    /**
     * Remove old Firebase user and create a new one.
     *
     * @throws NotAuthenticatedToFirebase if user is not authenticated in the first place
     * @throws InvalidFirebaseAuthTransaction if the process was not completed, e.g. if no Internet
     */
    private fun recreateFirebaseUser() {
        val auth = Firebase.auth
        if (auth.currentUser == null) {
            throw NotAuthenticatedToFirebase()
        }
        val user = auth.currentUser!!
        user.delete().addOnCompleteListener { taskDeleteUser ->
            if (taskDeleteUser.isSuccessful) {
                auth.signInAnonymously().addOnCompleteListener { taskNewUser ->
                    if (taskNewUser.isSuccessful) {
                        println("Old user deleted. New user: ${auth.currentUser?.uid}")
                    } else {
                        throw InvalidFirebaseAuthTransaction()
                    }
                }
            } else {
                throw InvalidFirebaseAuthTransaction()
            }
        }
    }

    class NotAuthenticatedToFirebase(): Exception()
    class InvalidFirebaseAuthTransaction(): Exception()
}