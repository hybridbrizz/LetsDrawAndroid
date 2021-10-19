package com.ericversteeg.liquidocean.listener

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ericversteeg.liquidocean.adapter.PalettesRecyclerViewAdapter

class SwipeToDeleteCallback(private val adapter: PalettesRecyclerViewAdapter): ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    lateinit var icon: Drawable
    lateinit var background: ColorDrawable

    init {
        adapter.context?.apply {
            icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_delete)!!
            background = ColorDrawable(Color.RED)
        }
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        adapter.deleteItem(position)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

        val itemView = viewHolder.itemView
        val backgroundCornerOffset = 0

        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
        val iconTop = itemView.top + iconMargin
        val iconBottom = iconTop + icon.intrinsicHeight

        // left swipe
        if (dX < 0) {
            val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
            val iconRight = itemView.right - iconMargin

            if (dX < -icon.intrinsicWidth - iconMargin * 2) {
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            }
            else {
                icon.setBounds(0, 0, 0, 0)
            }

            background.setBounds(itemView.right + dX.toInt() - backgroundCornerOffset, itemView.top, itemView.right, itemView.bottom)
        }
        else {
            icon.setBounds(0, 0, 0, 0)
            background.setBounds(0, 0, 0, 0)
        }

        background.draw(c)
        icon.draw(c)
    }
}