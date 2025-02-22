package com.ericversteeg.liquidocean.fragment

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.ericversteeg.liquidocean.adapter.PalettesRecyclerViewAdapter
import com.ericversteeg.liquidocean.databinding.FragmentPalettesBinding
import com.ericversteeg.liquidocean.helper.PanelThemeConfig
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.PalettesFragmentListener
import com.ericversteeg.liquidocean.listener.SwipeToDeleteCallback
import com.ericversteeg.liquidocean.model.Palette
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.view.ActionButtonView


class PalettesFragment: Fragment() {

    var palettesFragmentListener: PalettesFragmentListener? = null

    lateinit var adapter: PalettesRecyclerViewAdapter

    lateinit var panelThemeConfig: PanelThemeConfig
    
    private var _binding: FragmentPalettesBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPalettesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*back_button.actionBtnView = back_action
        back_action.type = ActionButtonView.Type.BACK_SOLID

        back_button.setOnClickListener {
            palettesFragmentListener?.onPalettesBack()
        }*/

        binding.addAction.type = ActionButtonView.Type.ADD

        binding.addButton.actionBtnView = binding.addAction
        binding.addButton.setOnClickListener {
            binding.addButton.visibility = View.GONE
            adapter.hideTitle = true

            binding.palettesRecyclerView.scrollTo(0, 0)

            binding.paletteNameInput.visibility = View.VISIBLE
            binding.paletteNameInput.requestFocus()

            showKeyboard(binding.paletteNameInput)
        }

        if (panelThemeConfig.actionButtonColor == Color.BLACK) {
            binding.addAction.colorMode = ActionButtonView.ColorMode.BLACK
        }
        else {
            binding.addAction.colorMode = ActionButtonView.ColorMode.WHITE
        }

        adapter = PalettesRecyclerViewAdapter(context, SessionSettings.instance.palettes, panelThemeConfig)

        binding.paletteNameInput.setOnEditorActionListener(object: TextView.OnEditorActionListener {
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

        binding.palettesRecyclerView.layoutManager = LinearLayoutManager(
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

        binding.palettesRecyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(adapter))
        itemTouchHelper.attachToRecyclerView(binding.palettesRecyclerView)

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
        (binding.palettesRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(SessionSettings.instance.selectedPaletteIndex + 1, Utils.dpToPx(context, 100))
    }

    private fun hideNameInput() {
        hideKeyboard()

        binding.paletteNameInput.visibility = View.GONE
        binding.addButton.visibility = View.VISIBLE

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