package com.vicentcodes.hydrosense
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class SensorDataAdapter(private val sensorData: List<Pair<Long?, Double?>?>) :
    RecyclerView.Adapter<SensorDataAdapter.SensorViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return SensorViewHolder(view)
    }

    override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
        val item = sensorData[position]

        // Formato para el valor de pH con un decimal
        val decimalFormat = DecimalFormat("#.#")
        val formattedPh = decimalFormat.format(item?.second ?: 0.0)

        // Formato para la fecha en el estilo "25 abril a las \n21:30"
        val sdf = SimpleDateFormat("d MMMM 'a las' \nHH:mm", Locale.getDefault())
        val dateStr = sdf.format(item!!.first?.let { Date(it) })

        holder.dateTextView.text = dateStr
        holder.phTextView.text = formattedPh
    }

    override fun getItemCount(): Int {
        return sensorData.size
    }

    inner class SensorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.findViewById(R.id.textView5)
        val phTextView: TextView = view.findViewById(R.id.textView4)
    }
}
