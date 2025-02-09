package com.matrixwarez.pt.fragment

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.matrixwarez.pt.R
import com.matrixwarez.pt.adapter.PalettesRecyclerViewAdapter
import com.matrixwarez.pt.listener.PalettesFragmentListener
import com.matrixwarez.pt.model.Palette
import com.matrixwarez.pt.model.SessionSettings
import com.matrixwarez.pt.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_palettes.*
import kotlinx.android.synthetic.main.fragment_pixel_history.*
import androidx.recyclerview.widget.ItemTouchHelper
import com.matrixwarez.pt.helper.PanelThemeConfig
import com.matrixwarez.pt.helper.Utils
import com.matrixwarez.pt.listener.SwipeToDeleteCallback
import kotlinx.android.synthetic.main.fragment_interactive_canvas.*


class PalettesFragment: Fragment() {

    var palettesFragmentListener: PalettesFragmentListener? = null

    lateinit var adapter: PalettesRecyclerViewAdapter

    lateinit var panelThemeConfig: PanelThemeConfig

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
            adapter.hideTitle = true

            palettes_recycler_view.scrollTo(0, 0)

            palette_name_input.visibility = View.VISIBLE
            palette_name_input.requestFocus()

            showKeyboard(palette_name_input)
        }

        if (panelThemeConfig.actionButtonColor == Color.BLACK) {
            add_action.colorMode = ActionButtonView.ColorMode.BLACK
        }
        else {
            add_action.colorMode = ActionButtonView.ColorMode.WHITE
        }

        adapter = PalettesRecyclerViewAdapter(context, SessionSettings.instance.palettes, panelThemeConfig)

        palette_name_input.setOnEditorActionListener(object: TextView.OnEditorActionListener {
            override fun onEditorAction(textView: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    textView?.apply {
                        if (!text.trim().matches(Regex("\\s*"))) {
                            if (text.trim().matches(Regex("[\\d\\w\\s]*"))) {
                                val name = text.trim().toString()
                                if (paletteWithName(name) == null) {
                                    SessionSettings.instance.addPalette(name)
                                    text = ""

                                    hideNameInput()

                                    adapter.notifyItemInserted(SessionSettings.instance.palettes.size)

                                    return true
                                }
                            }
                        }
                    }

                    textView?.text = ""
                    hideNameInput()

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

        scrollToSelectedPalette()

        Utils.setViewLayoutListener(view, object: Utils.ViewLayoutListener {
            override fun onViewLayout(view: View) {
                setBackground(view)
            }
        })
    }

    private fun setBackground(view: View) {
        context?.apply {
            val backgroundDrawable = ContextCompat.getDrawable(this, SessionSettings.instance.panelResIds[SessionSettings.instance.panelBackgroundResIndex]) as BitmapDrawable

            val scale = view.width / backgroundDrawable.bitmap.width.toFloat()

            val newWidth = (backgroundDrawable.bitmap.width * scale).toInt()
            val newHeight = (backgroundDrawable.bitmap.height * scale).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(backgroundDrawable.bitmap, newWidth,
                newHeight, false)
            val resizedBitmap = Bitmap.createBitmap(scaledBitmap, 0, scaledBitmap.height / 2 - view.height / 2, view.width, view.height)
            val resizedBitmapDrawable = BitmapDrawable(resizedBitmap)

            view.setBackgroundDrawable(resizedBitmapDrawable)
        }
    }

    fun scrollToSelectedPalette() {
        (palettes_recycler_view.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(SessionSettings.instance.selectedPaletteIndex + 1, Utils.dpToPx(context, 100))
    }

    private fun hideNameInput() {
        hideKeyboard()

        palette_name_input.visibility = View.GONE
        add_button.visibility = View.VISIBLE

        adapter.hideTitle = false
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

    fun paletteWithName(name: String): Palette? {
        for (palette in SessionSettings.instance.palettes) {
            if (palette.name == name) {
                return palette
            }
        }

        return null
    }
}