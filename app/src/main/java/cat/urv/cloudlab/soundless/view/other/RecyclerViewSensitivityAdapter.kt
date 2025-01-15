package cat.urv.cloudlab.soundless.view.other

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cat.urv.cloudlab.soundless.R


class RecyclerViewSensitivityAdapter(
    private var heartSensitivity: String? = null,
    private var sleepSensitivity: String? = null,
    private var combinedSensitivity: String? = null
) : RecyclerView.Adapter<RecyclerViewSensitivityAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val valueTextView: TextView = view.findViewById(R.id.text_value_sensitivity)
        val typeTextView: TextView = view.findViewById(R.id.text_type_sensitivity)
        val sensitivityIcon: ImageView = view.findViewById(R.id.sensitivity_icon)
    }


    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.recycler_row_sensitivity, viewGroup, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return 3
    }


    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        when (position) {
            0 -> {
                viewHolder.typeTextView.text = viewHolder.itemView.context.getString(R.string.heart_sensitivity)
                viewHolder.valueTextView.text = if (heartSensitivity == null || heartSensitivity == "NaN") "..." else heartSensitivity
                viewHolder.sensitivityIcon.setImageResource(R.drawable.ic_heart_dark)
            }

            1 -> {
                viewHolder.typeTextView.text = viewHolder.itemView.context.getString(R.string.sleep_sensitivity)
                viewHolder.valueTextView.text = if (sleepSensitivity == null || sleepSensitivity == "NaN") "..." else sleepSensitivity
                viewHolder.sensitivityIcon.setImageResource(R.drawable.ic_sleep)
            }

            2 -> {
                viewHolder.typeTextView.text = viewHolder.itemView.context.getString(R.string.combined_sensitivity)
                viewHolder.valueTextView.text = if (combinedSensitivity == null || combinedSensitivity == "NaN") "..." else combinedSensitivity
                viewHolder.sensitivityIcon.setImageResource(R.drawable.ic_combined_sensitivity)
            }
        }
    }
}