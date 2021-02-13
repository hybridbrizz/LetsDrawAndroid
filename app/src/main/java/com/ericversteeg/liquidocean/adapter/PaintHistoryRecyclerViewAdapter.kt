package com.ericversteeg.liquidocean.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.model.SessionSettings
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class PaintHistoryRecyclerViewAdapter(context: Context, pixelHistoryJson: JSONArray): RecyclerView.Adapter<PaintHistoryRecyclerViewAdapter.PaintHistoryViewHolder>() {

    private var pixelHistoryJson: JSONArray = pixelHistoryJson
    var context: Context = context

    private var selectedPos = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaintHistoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.paint_history_adapter_view, parent, false)
        val vh = PaintHistoryViewHolder(v)

        return vh
    }

    override fun onBindViewHolder(holder: PaintHistoryViewHolder, position: Int) {
        val jsonObj = pixelHistoryJson.getJSONObject(pixelHistoryJson.length() - 1 - position)
        holder.nameTextView.text = jsonObj.getString("name")
        holder.colorView.setBackgroundColor(jsonObj.getInt("color"))

        val timestamp = jsonObj.getInt("timestamp").toLong() * 1000
        val date = Date((timestamp))
        val dateCal = GregorianCalendar()
        dateCal.time = date

        val nowCal = GregorianCalendar()
        nowCal.time = date

        if (dateCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) && dateCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)) {
            val simpleDateFormat =  SimpleDateFormat("hh:mm a", Locale.ENGLISH)
            val dateStr = simpleDateFormat.format(date).toLowerCase()
            holder.dateTextView.text = dateStr
        }
        else {
            val simpleDateFormat =  SimpleDateFormat("MM-dd hh:mm a", Locale.ENGLISH)
            val dateStr = simpleDateFormat.format(date).toLowerCase()
            holder.dateTextView.text = dateStr
        }
    }

    override fun getItemCount(): Int {
        return pixelHistoryJson.length()
    }

    class PaintHistoryViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var nameTextView: TextView = v.findViewById(R.id.name_text)
        var colorView: View = v.findViewById(R.id.paint_color)
        var dateTextView: TextView = v.findViewById(R.id.date_text)
    }
}