package cat.urv.cloudlab.soundless.view.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cat.urv.cloudlab.soundless.R
import cat.urv.cloudlab.soundless.databinding.FragmentPopUpTelegramBinding
import cat.urv.cloudlab.soundless.view.other.CustomDialog

class PopUpTelegram : Fragment() {
    private var _binding: FragmentPopUpTelegramBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPopUpTelegramBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val telegramButton = binding.telegramButton
        val cancelButton = binding.cancelButton

        cancelButton.setOnClickListener{
            requireActivity().onBackPressed()
        }

        telegramButton.setOnClickListener {
            CustomDialog.showCustomDialog(requireActivity()) {
                setTitle(R.string.telegram_text)

                setMessage(R.string.telegram_message)

                setPositiveButton(getString(R.string.ok)) { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+qPMXGXKmg-xhYjlk"))
                    startActivity(intent)
                }

                setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            }
        }
    }
}