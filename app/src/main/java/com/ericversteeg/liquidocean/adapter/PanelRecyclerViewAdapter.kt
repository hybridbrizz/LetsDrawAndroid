package com.ericversteeg.liquidocean.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.helper.PanelThemeConfig
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.view.ActionButtonView
import com.ericversteeg.liquidocean.view.ClickableImageView

class PanelRecyclerViewAdapter(context: Context, panelResIds: MutableList<Int>): RecyclerView.Adapter<PanelRecyclerViewAdapter.PanelBackgroundViewHolder>() {

    private var panelResIds: MutableList<Int> = panelResIds
    var context: Context = context

    private var selectedPos = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanelBackgroundViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.panel_adapter_view, parent, false)
        val vh = PanelBackgroundViewHolder(v)

        return vh
    }

    override fun onBindViewHolder(holder: PanelBackgroundViewHolder, position: Int) {
        val resId = panelResIds[position]
        val themeConfig = PanelThemeConfig.buildConfig(resId)

        holder.image.setImageDrawable(ContextCompat.getDrawable(context, resId))

        holder.image.setOnClickListener {
            SessionSettings.instance.panelBackgroundResIndex = position
            holder.selectedIcon.visibility = View.VISIBLE
            ImageViewCompat.setImageTintList(holder.selectedIcon, ColorStateList.valueOf(themeConfig.actionButtonColor))

            notifyItemChanged(selectedPos)
            selectedPos = position
            notifyItemChanged(selectedPos)
        }

        if (SessionSettings.instance.panelBackgroundResIndex == position) {
            holder.selectedIcon.visibility = View.VISIBLE
            ImageViewCompat.setImageTintList(holder.selectedIcon, ColorStateList.valueOf(themeConfig.actionButtonColor))
            selectedPos = position
        }
        else {
            holder.selectedIcon.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return panelResIds.size
    }

    class PanelBackgroundViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var image: ClickableImageView = v.findViewById(R.id.panel_image)
        var selectedIcon: ImageView = v.findViewById(R.id.selected_icon)
    }
}