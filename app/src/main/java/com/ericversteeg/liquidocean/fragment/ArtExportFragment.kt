package com.ericversteeg.liquidocean.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.listener.ArtExportFragmentListener
import com.ericversteeg.liquidocean.model.InteractiveCanvas
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.fragment_art_export.*

class ArtExportFragment: Fragment() {

    lateinit var art: List<InteractiveCanvas.RestorePoint>

    var ppu = 10

    var listener: ArtExportFragmentListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_art_export, container, false)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        back_action_export.type = ActionButtonView.Type.BACK
        back_button_export.actionBtnView = back_action_export

        share_action.type = ActionButtonView.Type.EXPORT_SOLID
        share_button.actionBtnView = share_action

        save_action.type = ActionButtonView.Type.SAVE
        save_button.actionBtnView = save_action

        back_button_export.setOnClickListener {
            listener?.onArtExportBack()
        }

        art_view.art = art

        share_button.setOnClickListener {
            context?.apply {
                art_view.shareArt(this)
            }
        }

        save_button.setOnClickListener {
            context?.apply {
                art_view.saveArt(this)
            }
        }
    }
}