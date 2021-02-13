package com.ericversteeg.liquidocean.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.adapter.PaintHistoryRecyclerViewAdapter
import com.ericversteeg.liquidocean.adapter.PanelRecyclerViewAdapter
import com.ericversteeg.liquidocean.listener.PixelHistoryCallback
import com.ericversteeg.liquidocean.model.InteractiveCanvas
import kotlinx.android.synthetic.main.fragment_options.*
import kotlinx.android.synthetic.main.fragment_pixel_history.*
import org.json.JSONArray

class PixelHistoryFragment: Fragment() {

    lateinit var pixelHistoryJson: JSONArray

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pixel_history, container, false)

        // setup views here

        return view
    }

    override fun onResume() {
        super.onResume()

        context?.apply {
            if (pixelHistoryJson.length() > 0) {
                // setup recycler view
                pixel_history_recycler_view.layoutManager = LinearLayoutManager(
                    this,
                    LinearLayoutManager.VERTICAL,
                    false
                )
                pixel_history_recycler_view.adapter = PaintHistoryRecyclerViewAdapter(
                    this, pixelHistoryJson
                )

                pixel_history_recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
            }
            else {
                no_history_text.visibility = View.VISIBLE
            }
        }
    }
}