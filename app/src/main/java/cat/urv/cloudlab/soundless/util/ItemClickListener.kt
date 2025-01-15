package cat.urv.cloudlab.soundless.util

import android.view.View

/**
 * Interface for a listener to react to click events in a menu, adapter, etc.
 */
interface ItemClickListener {
    fun onItemClick(view: View, position: Int)
}