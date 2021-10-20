package com.ericversteeg.liquidocean.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.model.Palette
import com.ericversteeg.liquidocean.model.SessionSettings
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class PalettesRecyclerViewAdapter(val context: Context?, private val palettes: MutableList<Palette>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val headerViewType = 0
    val paletteViewType = 1

    private var selectedPos = RecyclerView.NO_POSITION

    var itemClickListener: OnItemClickListener<Palette>? = null

    var recentlyDeletedItem: Palette? = null
    var recentlyDeletedItemPosition: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == headerViewType) {
            val v = inflater.inflate(R.layout.palette_header_view, parent, false)
            HeaderViewHolder(v)
        } else {
            val v = inflater.inflate(R.layout.palette_adapter_view, parent, false)
            PaletteViewHolder(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        /*holder.backgroundView.setOnClickListener {
            selectedItems[position] = !selectedItems[position]

            setupViewHolder(holder, position)
        }*/

        if (holder is PaletteViewHolder) {
            setupPaletteViewHolder(holder, position)
        }
    }

    private fun setupPaletteViewHolder(holder: PaletteViewHolder, position: Int) {
        val palette = palettes[position - 1]

        holder.nameTextView.text = palette.name

        if (palette.name == "Recent Color") {
            holder.numColorsTextView.text = String.format("%d colors", SessionSettings.instance.numRecentColors)
        }
        else if (palette.colors.size == 1) {
            holder.numColorsTextView.text = String.format("%d color", palette.colors.size)
        }
        else {
            holder.numColorsTextView.text = String.format("%d colors", palette.colors.size)
        }

        holder.itemView.setOnClickListener {
            itemClickListener?.onItemClicked(palette, position - 1)
        }
    }

    override fun getItemCount(): Int {
        return palettes.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            headerViewType
        } else {
            paletteViewType
        }
    }

    fun deleteItem(position: Int) {
        recentlyDeletedItem = palettes[position - 1]
        recentlyDeletedItemPosition = position

        palettes.removeAt(position - 1)
        notifyItemRemoved(position)

        itemClickListener?.onItemDeleted(recentlyDeletedItem!!)
    }

    fun undoDelete() {
        palettes.add(recentlyDeletedItemPosition!! - 1, recentlyDeletedItem!!)
        notifyItemInserted(recentlyDeletedItemPosition!!)
    }

    class HeaderViewHolder(v: View) : RecyclerView.ViewHolder(v)

    class PaletteViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var nameTextView: TextView = v.findViewById(R.id.palette_name)
        var numColorsTextView: TextView = v.findViewById(R.id.palette_num_colors)
    }

    fun setOnItemClickListener(listener: OnItemClickListener<Palette>) {
        itemClickListener = listener
    }

    interface OnItemClickListener<T> {
        fun onItemClicked(item: T, index: Int)
        fun onItemDeleted(item: T)
    }
}