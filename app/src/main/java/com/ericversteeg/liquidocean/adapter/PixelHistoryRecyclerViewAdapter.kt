package com.ericversteeg.liquidocean.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.ericversteeg.liquidocean.R
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class PixelHistoryRecyclerViewAdapter(context: Context, pixelHistoryJson: JSONArray): RecyclerView.Adapter<PixelHistoryRecyclerViewAdapter.PaintHistoryViewHolder>() {

    private var pixelHistoryJson: JSONArray = pixelHistoryJson
    var selectedItems = BooleanArray(pixelHistoryJson.length())
    var context: Context = context

    private var selectedPos = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaintHistoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.paint_history_adapter_view, parent, false)
        val vh = PaintHistoryViewHolder(v)

        return vh
    }

    override fun onBindViewHolder(holder: PaintHistoryViewHolder, position: Int) {
        holder.backgroundView.setOnClickListener {
            selectedItems[position] = !selectedItems[position]

            setupViewHolder(holder, position)
        }

        setupViewHolder(holder, position)
    }

    private fun setupViewHolder(holder: PaintHistoryViewHolder, position: Int) {
        val jsonObj = pixelHistoryJson.getJSONObject(pixelHistoryJson.length() - 1 - position)
        val timestamp = jsonObj.getInt("timestamp").toLong() * 1000
        val date = Date((timestamp))

        // selected
        if (selectedItems[position]) {
            val simpleDateFormat =  SimpleDateFormat("MM-dd-yyyy HH:mm", Locale.ENGLISH)
            val dateStr = simpleDateFormat.format(date).toLowerCase()
            holder.nameTextView.text = ""
            holder.dateTextView.text = ""
            holder.fullDateView.text = dateStr
        }
        else {
            holder.nameTextView.text = jsonObj.getString("name") + " (" + jsonObj.getInt("level") + ")"
            holder.colorView.setBackgroundColor(jsonObj.getInt("color"))
            holder.fullDateView.text = ""

            val dateCal = GregorianCalendar()
            dateCal.time = date

            val nowCal = GregorianCalendar()
            nowCal.time = Date()

            val days = nowCal.get(Calendar.DAY_OF_YEAR) - dateCal.get(Calendar.DAY_OF_YEAR)
            val sameYear = dateCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)

            if (days == 0 && sameYear) {
                val simpleDateFormat =  SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                val dateStr = simpleDateFormat.format(date).toLowerCase()
                holder.dateTextView.text = dateStr
            }
            else if (days == 1 && sameYear) {
                holder.dateTextView.text = "Yesterday"
            }
            else if (days in 2..6 && sameYear) {
                val simpleDateFormat =  SimpleDateFormat("EEEE", Locale.ENGLISH)
                val dateStr = simpleDateFormat.format(date)
                holder.dateTextView.text = dateStr
            }
            else if (days in 7..13 && sameYear) {
                holder.dateTextView.text = "Week ago"
            }
            else if (days in 14..20 && sameYear) {
                holder.dateTextView.text = "Two weeks ago"
            }
            else if (days in 21..28 && sameYear) {
                holder.dateTextView.text = "Three weeks ago"
            }
            else if (days in 29..31 && sameYear) {
                holder.dateTextView.text = "Four weeks ago"
            }
            else if (sameYear) {
                val simpleDateFormat =  SimpleDateFormat("M", Locale.ENGLISH)
                val dateStr = simpleDateFormat.format(date)
                holder.dateTextView.text = dateStr
            }
            else {
                val simpleDateFormat =  SimpleDateFormat("MM-dd-yy", Locale.ENGLISH)
                val dateStr = simpleDateFormat.format(date).toLowerCase()
                holder.dateTextView.text = dateStr
            }
        }
    }

    override fun getItemCount(): Int {
        return pixelHistoryJson.length()
    }

    class PaintHistoryViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var nameTextView: TextView = v.findViewById(R.id.name_text)
        var colorView: View = v.findViewById(R.id.paint_color)
        var dateTextView: TextView = v.findViewById(R.id.date_text)
        var backgroundView: ConstraintLayout = v.findViewById(R.id.background_view)
        var fullDateView: TextView = v.findViewById(R.id.full_date_text)
    }
}