package cat.urv.cloudlab.soundless.view.other

import android.app.Dialog
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import cat.urv.cloudlab.soundless.R


class CustomDialog(
    private val fragmentActivity: FragmentActivity,
    private val lambda: AlertDialog.Builder.() -> Unit
) : DialogFragment() {

    private lateinit var onDestroyFunction: () -> Unit

    override fun onDestroy() {
        super.onDestroy()
        onDestroyFunction.invoke()
        dismiss()
        fragmentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            fragmentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else
            fragmentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        return fragmentActivity.let {
            val builder = AlertDialog.Builder(it)
            builder.lambda()
            builder.create()
        }
    }

    override fun onStart() {
        super.onStart()

        when (fragmentActivity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> {
                (dialog as AlertDialog?)!!.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.primary))
                (dialog as AlertDialog?)!!.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.primary))
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                (dialog as AlertDialog?)!!.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE)
                (dialog as AlertDialog?)!!.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE)
            }
        }

        // TO-DO: Make it with themes automatics
        //(dialog as AlertDialog?)!!.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.)
        //(dialog as AlertDialog?)!!.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.primary))
    }

    companion object {
        fun showCustomDialog(
            activity: FragmentActivity,
            tag: String? = null,
            onDestroyFunction: () -> Unit = {},
            lambda: AlertDialog.Builder.() -> Unit
        ) {
            val customDialog = CustomDialog(activity, lambda)
            customDialog.onDestroyFunction = onDestroyFunction
            customDialog.show(activity.supportFragmentManager, tag)
        }
    }
}