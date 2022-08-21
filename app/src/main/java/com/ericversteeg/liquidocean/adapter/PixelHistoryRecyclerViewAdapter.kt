package com.ericversteeg.liquidocean.adapter

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.model.Server
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.service.CanvasService
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class PixelHistoryRecyclerViewAdapter(context: Context, val server: Server, pixelHistoryJson: JSONArray): RecyclerView.Adapter<PixelHistoryRecyclerViewAdapter.PaintHistoryViewHolder>() {

    private val canvasService = CanvasService(server)

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

        if (server.isAdmin) {
            val jsonObj = pixelHistoryJson.getJSONObject(pixelHistoryJson.length() - 1 - position)

            holder.backgroundView.setOnLongClickListener {
                AlertDialog.Builder(context, R.style.AlertDialogTheme)
                    .setMessage("Ban ${jsonObj.getString("name")}?")
                    .setPositiveButton("Yes") { _, _ ->
                        AlertDialog.Builder(context, R.style.AlertDialogTheme)
                            .setMessage("Confirm ban on ${jsonObj.getString("name")}?")
                            .setPositiveButton("Yes") { _, _ ->
                                canvasService.banDeviceIps(jsonObj.getInt("device_id")) { response ->
                                    if (response == null) {
                                        Toast.makeText(context, "Ban failed (server error).", Toast.LENGTH_LONG).show()
                                        return@banDeviceIps
                                    }

                                    Toast.makeText(context, "Banned ${jsonObj.getString("name")} (${response.get("ips").asInt} IPs).", Toast.LENGTH_LONG).show()
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

                false
            }
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
            holder.nameTextView1.text = ""
            holder.nameTextView2.text = ""
            holder.nameTextView3.text = ""
            holder.dateTextView.text = ""
            holder.fullDateView.text = dateStr
        }
        else {
            var name = jsonObj.getString("name")

            if (name.length > 10) {
                name = "${name.substring(0 until 7)}..."
            }

            holder.nameTextView1.text = name
            holder.nameTextView2.text = " (${jsonObj.getInt("level")})"

            if (name == SessionSettings.instance.firstContributorName) {
                //FAD55D
                holder.nameTextView1.setTextColor(Color.parseColor("#DECB52"))
            }
            else if (name == SessionSettings.instance.secondContributorName) {
                holder.nameTextView1.setTextColor(Color.parseColor("#AFB3B1"))
            }
            else if (name == SessionSettings.instance.thirdContributorName) {
                holder.nameTextView1.setTextColor(Color.parseColor("#BD927B"))
            }
            else {
                holder.nameTextView1.setTextColor(Color.WHITE)
            }

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
                val simpleDateFormat =  SimpleDateFormat("MMMM", Locale.ENGLISH)
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
        var nameTextView1: TextView = v.findViewById(R.id.name_text_1)
        var nameTextView2: TextView = v.findViewById(R.id.name_text_2)
        var nameTextView3: TextView = v.findViewById(R.id.name_text_3)
        var colorView: View = v.findViewById(R.id.paint_color)
        var dateTextView: TextView = v.findViewById(R.id.date_text)
        var backgroundView: ConstraintLayout = v.findViewById(R.id.background_view)
        var fullDateView: TextView = v.findViewById(R.id.full_date_text)
    }
}