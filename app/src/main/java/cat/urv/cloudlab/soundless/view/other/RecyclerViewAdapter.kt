package cat.urv.cloudlab.soundless.view.other

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cat.urv.cloudlab.soundless.R
import cat.urv.cloudlab.soundless.util.ItemClickListener
import cat.urv.cloudlab.soundless.viewmodel.RecordingMetadata
import java.text.SimpleDateFormat
import java.util.*

class RecyclerViewAdapter(
    private val recordingMetadataList: List<RecordingMetadata>
) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {
    lateinit var itemClickListener: ItemClickListener

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val dateTextView: TextView = view.findViewById(R.id.text_row_item)
        val activeRecordingTextView: TextView = view.findViewById(R.id.text_active_recording)
        val rowLayout: LinearLayout = view.findViewById(R.id.row)
        val healthAvailableIcon: ImageView = view.findViewById(R.id.health_available_icon)

        init {
            dateTextView.setOnClickListener(this)
        }
        override fun onClick(v: View) {
            if (this@RecyclerViewAdapter::itemClickListener.isInitialized) {
                itemClickListener.onItemClick(v, absoluteAdapterPosition)
            }
        }
    }

    /**
     * Set up the view creation (invoked by the layout manager).
     */
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.recycler_row, viewGroup, false)
        return ViewHolder(view)
    }

    /**
     * Replace the contents of a view (invoked by the layout manager).
     */
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Last recorded element first
        val reversedPosition = recordingMetadataList.lastIndex - position
        recordingMetadataList[reversedPosition].let {
            val dateTextView = viewHolder.dateTextView
            dateTextView.text = getDateTime(it.timestampEnd)
            if (it.isHealthDataAvailable) {
                viewHolder.healthAvailableIcon.setImageResource(R.drawable.ic_heart_dark_with_green_circle)
            }
            if (it.currentlyActive) {
                viewHolder.rowLayout.setBackgroundResource(R.drawable.recycler_row_border_with_background)
                viewHolder.healthAvailableIcon.visibility = View.GONE

                val activeRecordingTextView = viewHolder.activeRecordingTextView
                activeRecordingTextView.visibility = View.VISIBLE

                activeRecordingTextView.setTextColor(Color.WHITE)
                dateTextView.setTextColor(Color.WHITE)

                val layoutParams = viewHolder.rowLayout.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.bottomMargin += 20
            }
        }
    }

    /**
     * Return the size of the dataset (invoked by the layout manager).
     */
    override fun getItemCount() = recordingMetadataList.size

    /**
     * Get item given its position in the list.
     */
    fun getItem(position: Int): RecordingMetadata {
        // Last recorded element first
        val reversedPosition = recordingMetadataList.lastIndex - position
        return recordingMetadataList[reversedPosition]
    }

    fun setOnClickListener(itemClickListener: ItemClickListener) {
        this.itemClickListener = itemClickListener
    }

    /**
     * From a given timestamp, return a string representing that particular moment with proper date
     * formatting.
     */
    private fun getDateTime(timestamp: Long): String? {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
            val netDate = Date(timestamp)
            sdf.format(netDate)
        } catch (e: Exception) {
            e.toString()
        }
    }

}