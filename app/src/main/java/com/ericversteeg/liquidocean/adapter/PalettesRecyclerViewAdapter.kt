package com.ericversteeg.liquidocean.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.model.Palette
import com.ericversteeg.liquidocean.model.SessionSettings
import kotlinx.android.synthetic.main.palette_header_view.view.*
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class PalettesRecyclerViewAdapter(val context: Context?, private val palettes: MutableList<Palette>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var recyclerView: RecyclerView

    val headerViewType = 0
    val paletteViewType = 1

    private var selectedPos = RecyclerView.NO_POSITION

    var itemClickListener: OnItemClickListener<Palette>? = null

    var recentlyDeletedItem: Palette? = null
    var recentlyDeletedItemPosition: Int? = null

    var hideTitle = false
    set(value) {
        field = value
        notifyItemChanged(0)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        this.recyclerView = recyclerView
    }

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

        if (holder is HeaderViewHolder) {
            setupHeaderViewHolder(holder)
        }
        if (holder is PaletteViewHolder) {
            setupPaletteViewHolder(holder, position)
        }
    }

    private fun setupHeaderViewHolder(holder: HeaderViewHolder) {
        if (hideTitle) {
            holder.titleTextView.visibility = View.INVISIBLE
        }
        else {
            holder.titleTextView.visibility = View.VISIBLE
        }
    }

    private fun setupPaletteViewHolder(holder: PaletteViewHolder, position: Int) {
        val palette = palettes[position - 1]

        holder.nameTextView.text = palette.name
        if (SessionSettings.instance.palette.name == palette.name) {
            holder.nameTextView.setTextColor(Color.parseColor("#df7126"))
        }
        else {
            holder.nameTextView.setTextColor(Color.WHITE)
        }

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

        holder.itemView.setOnTouchListener(object: View.OnTouchListener {
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    holder.nameTextView.setTextColor(Color.parseColor("#df7126"))

                    val selectedPaletteViewHolder = selectedPaletteViewHolder()
                    if (selectedPaletteViewHolder != null) {
                        if (selectedPaletteViewHolder != holder) {
                            selectedPaletteViewHolder.nameTextView.setTextColor(Color.WHITE)
                        }
                    }
                }
                else if (event.action == MotionEvent.ACTION_CANCEL) {
                    if (SessionSettings.instance.palette.name != palette.name) {
                        holder.nameTextView.setTextColor(Color.WHITE)

                        val selectedPaletteViewHolder = selectedPaletteViewHolder()
                        selectedPaletteViewHolder?.nameTextView?.setTextColor(Color.parseColor("#df7126"))
                    }
                }

                return false
            }
        })
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

    fun selectedPaletteViewHolder(): PaletteViewHolder? {
        for (i in 0 until recyclerView.childCount) {
            val vh = recyclerView.getChildViewHolder(recyclerView.getChildAt(i))
            if (vh is PaletteViewHolder) {
                if (vh.layoutPosition == selectedPalettePos()) {
                    return vh
                }
            }
        }

        return null
    }

    private fun selectedPalettePos(): Int {
        val palettes = SessionSettings.instance.palettes
        for (i in palettes.indices) {
            val palette = palettes[i]
            if (SessionSettings.instance.palette.name == palette.name) {
                return i + 1
            }
        }

        return -1
    }

    class HeaderViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var titleTextView: TextView = v.findViewById(R.id.title_text)
    }

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