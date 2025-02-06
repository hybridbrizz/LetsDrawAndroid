package com.ericversteeg.liquidocean.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.ericversteeg.liquidocean.adapter.PixelHistoryRecyclerViewAdapter
import com.ericversteeg.liquidocean.databinding.FragmentPixelHistoryBinding
import org.json.JSONArray

class PixelHistoryFragment: Fragment() {

    lateinit var pixelHistoryJson: JSONArray
    
    private var _binding: FragmentPixelHistoryBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPixelHistoryBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()

        context?.apply {
            if (pixelHistoryJson.length() > 0) {
                // setup recycler view
                binding.pixelHistoryRecyclerView.layoutManager = LinearLayoutManager(
                    this,
                    LinearLayoutManager.VERTICAL,
                    false
                )
                binding.pixelHistoryRecyclerView.adapter = PixelHistoryRecyclerViewAdapter(
                    this, pixelHistoryJson
                )

                binding.pixelHistoryRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
            }
            else {
                binding.noHistoryText.visibility = View.VISIBLE
            }
        }
    }
}