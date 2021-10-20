package com.ericversteeg.liquidocean.fragment

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.adapter.PalettesRecyclerViewAdapter
import com.ericversteeg.liquidocean.listener.PalettesFragmentListener
import com.ericversteeg.liquidocean.model.Palette
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_palettes.*
import kotlinx.android.synthetic.main.fragment_pixel_history.*
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.ItemTouchHelper
import com.ericversteeg.liquidocean.listener.SwipeToDeleteCallback


class PalettesFragment: Fragment() {

    var palettesFragmentListener: PalettesFragmentListener? = null

    lateinit var adapter: PalettesRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_palettes, container, false)

        // setup views here

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*back_button.actionBtnView = back_action
        back_action.type = ActionButtonView.Type.BACK_SOLID

        back_button.setOnClickListener {
            palettesFragmentListener?.onPalettesBack()
        }*/

        add_action.type = ActionButtonView.Type.ADD
        add_button.actionBtnView = add_action
        add_button.setOnClickListener {
            add_button.visibility = View.GONE

            palettes_recycler_view.scrollTo(0, 0)

            palette_name_input.visibility = View.VISIBLE
            palette_name_input.requestFocus()

            showKeyboard(palette_name_input)
        }

        adapter = PalettesRecyclerViewAdapter(context, SessionSettings.instance.palettes)

        palette_name_input.setOnEditorActionListener(object: TextView.OnEditorActionListener {
            override fun onEditorAction(textView: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    textView?.apply {
                        if (!text.trim().matches(Regex("\\s*"))) {
                            if (text.trim().matches(Regex("[\\d\\w\\s]*"))) {
                                SessionSettings.instance.addPalette(text.toString())
                                text = ""
                            }
                            else {
                                hideNameInput()

                                return true
                            }
                        }
                        else {
                            hideNameInput()

                            return true
                        }
                    }

                    hideNameInput()

                    adapter.notifyItemInserted(SessionSettings.instance.palettes.size)

                    return true
                }
                return false
            }

        })

        palettes_recycler_view.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )

        adapter.setOnItemClickListener(object: PalettesRecyclerViewAdapter.OnItemClickListener<Palette> {
            override fun onItemClicked(item: Palette, index: Int) {
                SessionSettings.instance.selectedPaletteIndex = index

                palettesFragmentListener?.onPaletteSelected(item, index)
            }

            override fun onItemDeleted(item: Palette) {
                palettesFragmentListener?.onPaletteDeleted(item)
            }
        })

        palettes_recycler_view.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(adapter))
        itemTouchHelper.attachToRecyclerView(palettes_recycler_view)
    }

    private fun hideNameInput() {
        hideKeyboard()

        palette_name_input.visibility = View.GONE
        add_button.visibility = View.VISIBLE
    }

    private fun showKeyboard(editText: EditText) {
        context?.apply {
            val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideKeyboard() {
        context?.apply {
            val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
        }
    }

    fun undoDelete() {
        adapter.undoDelete()
    }
}