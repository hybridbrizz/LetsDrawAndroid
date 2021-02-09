package com.ericversteeg.liquidocean.fragment

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.RequiresApi
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.*
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.view.ActionButtonView
import com.plattysoft.leonids.ParticleSystem
import com.plattysoft.leonids.modifiers.AlphaModifier
import kotlinx.android.synthetic.main.fragment_interactive_canvas.*
import top.defaults.colorpicker.ColorObserver
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor


class InteractiveCanvasFragment : Fragment(), InteractiveCanvasDrawerCallback, PaintQtyListener, RecentColorsListener, SocketStatusCallback {

    var scaleFactor = 1f

    var initalColor = 0

    var topLeftParticleSystem: ParticleSystem? = null
    var topRightParticleSystem: ParticleSystem? = null
    var bottomLeftParticleSystem: ParticleSystem? = null
    var bottomRightParticleSystem: ParticleSystem? = null

    var world = false
    var singlePlayBackgroundType: ActionButtonView.Type? = null

    var interactiveCanvasFragmentListener: InteractiveCanvasFragmentListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_interactive_canvas, container, false)

        // setup views here

        return view
    }

    override fun onPause() {
        super.onPause()

        stopEmittingParticles()

        // unregister listeners
        SessionSettings.instance.paintQtyListeners.remove(this)

        context?.apply {
            surface_view.interactiveCanvas.saveUnits(this)
            surface_view.interactiveCanvas.drawCallbackListener = null
        }
    }

    override fun onResume() {
        super.onResume()

        if (world) {
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    context?.apply {
                        var connected = Utils.isNetworkAvailable(this)
                        if (!connected) {
                            (context as Activity).runOnUiThread {
                                showDisconnectedMessage(0)
                            }
                        }
                        else {
                            sendApiStatusCheck()
                        }
                    }
                }
            }, 0, 1000 * 60)
        }
    }

    private fun sendApiStatusCheck() {
        val requestQueue = Volley.newRequestQueue(context)
        context?.apply {
            val request = JsonObjectRequest(
                Request.Method.GET,
                Utils.baseUrlApi + "/api/v1/status",
                null,
                { response ->
                    (context as Activity).runOnUiThread {
                        sendSocketStatusCheck()
                    }
                },
                { error ->
                    (context as Activity).runOnUiThread {
                        showDisconnectedMessage(1)
                    }
                })

            requestQueue.add(request)
        }
    }

    private fun sendSocketStatusCheck() {
        surface_view.interactiveCanvas.checkSocketStatus()
    }

    // socket check callback
    override fun onSocketStatusError() {
        (context as Activity?)?.runOnUiThread {
            showDisconnectedMessage(2)
        }
    }

    private fun showDisconnectedMessage(type: Int) {
        AlertDialog.Builder(context)
            .setMessage("Lost connection to world server (code=$type)")
            // The dialog is automatically dismissed when a dialog button is clicked.
            .setPositiveButton(
                android.R.string.ok,
                object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, id: Int) {
                        interactiveCanvasFragmentListener?.onInteractiveCanvasBack()
                        dialog?.dismiss()
                    }
                })
            .setOnDismissListener {
                interactiveCanvasFragmentListener?.onInteractiveCanvasBack()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // set single play background type, must call before setting interactiveCanvas.world
        if (!world) {
            singlePlayBackgroundType?.apply {
                surface_view.interactiveCanvas.singlePlayBackgroundType = this
            }
        }

        // must call before darkIcons
        surface_view.interactiveCanvas.world = world

        SessionSettings.instance.darkIcons = (surface_view.interactiveCanvas.getGridLineColor() == Color.BLACK)

        SessionSettings.instance.paintQtyListeners.add(paint_qty_bar)
        SessionSettings.instance.paintQtyListeners.add(this)

        SessionSettings.instance.paintColor = surface_view.interactiveCanvas.getGridLineColor()

        surface_view.interactiveCanvas.recentColorsListener = this
        surface_view.interactiveCanvas.socketStatusCallback = this

        paint_qty_bar.world = world

        color_picker_view.setSelectorColor(Color.WHITE)

        back_button.actionBtnView = back_action
        back_action.type = ActionButtonView.Type.BACK

        paint_panel_button.actionBtnView = paint_panel_action_view
        paint_panel_action_view.type = ActionButtonView.Type.PAINT

        paint_yes.type = ActionButtonView.Type.YES
        paint_yes.colorMode = ActionButtonView.ColorMode.COLOR

        paint_no.type = ActionButtonView.Type.NO
        paint_no.colorMode = ActionButtonView.ColorMode.COLOR

        close_paint_panel.type = ActionButtonView.Type.PAINT_CLOSE

        paint_color_accept_image.type = ActionButtonView.Type.YES
        paint_color_accept_image.colorMode = ActionButtonView.ColorMode.NONE

        recent_colors.type = ActionButtonView.Type.RECENT_COLORS
        recent_colors_button.actionBtnView = recent_colors

        setupRecentColors(surface_view.interactiveCanvas.recentColorsList.toTypedArray())

        color_picker_view.subscribe(object : ColorObserver {
            override fun onColor(color: Int, fromUser: Boolean, shouldPropagate: Boolean) {
                paint_indicator_view.setPaintColor(color)

                /* if (Utils.isColorDark(color)) {
                    paint_yes.setImageDrawable(resources.getDrawable(R.drawable.ic_done_white_border))
                }
                else {
                    paint_yes.setImageDrawable(resources.getDrawable(R.drawable.ic_done_white))
                    DrawableCompat.setTint(paint_yes.drawable, color)
                } */
            }
        })

        paint_panel.setOnClickListener {

        }

        paint_panel_button.setOnClickListener {
            paint_panel.visibility = View.VISIBLE
            paint_panel_button.visibility = View.GONE

            paint_panel.animate().translationX(paint_panel.width.toFloat() * 0.99F).setDuration(0).withEndAction {
                paint_panel.animate().translationX(0F).setDuration(50).setInterpolator(
                    AccelerateDecelerateInterpolator()
                ).withEndAction {

                    startParticleEmitters()

                }.start()

                paint_warning_frame.visibility = View.VISIBLE
                paint_warning_frame.alpha = 0F
                paint_warning_frame.animate().alpha(1F).setDuration(50).start()
            }.start()

            surface_view.startPainting()

            recent_colors_button.visibility = View.VISIBLE
            recent_colors_container.visibility = View.GONE

            back_button.visibility = View.GONE
        }

        paint_yes.setOnClickListener {
            surface_view.endPainting(true)

            paint_yes.visibility = View.GONE
            paint_no.visibility = View.GONE
            close_paint_panel.visibility = View.VISIBLE

            surface_view.startPainting()
        }

        paint_no.setOnClickListener {
            if (color_picker_frame.visibility == View.VISIBLE) {
                paint_indicator_view.setPaintColor(initalColor)
                color_picker_frame.visibility = View.GONE

                paint_warning_frame.visibility = View.VISIBLE

                paint_yes.visibility = View.VISIBLE

                paint_color_accept_image.visibility = View.GONE

                recent_colors_button.visibility = View.VISIBLE
                recent_colors_container.visibility = View.GONE

                surface_view.endPaintSelection()

                paint_no.colorMode = ActionButtonView.ColorMode.COLOR

                if (surface_view.interactiveCanvas.restorePoints.size == 0) {
                    paint_yes.visibility = View.GONE
                    paint_no.visibility = View.GONE
                    close_paint_panel.visibility = View.VISIBLE
                }
                else {
                    paint_yes.visibility = View.VISIBLE
                    paint_no.visibility = View.VISIBLE
                    close_paint_panel.visibility = View.GONE
                }

                startParticleEmitters()
            }
            else {
                surface_view.endPainting(false)

                paint_yes.visibility = View.GONE
                paint_no.visibility = View.GONE
                close_paint_panel.visibility = View.VISIBLE

                recent_colors_button.visibility = View.VISIBLE
                recent_colors_container.visibility = View.GONE

                surface_view.startPainting()
            }
        }

        close_paint_panel.setOnClickListener {
            surface_view.endPainting(false)

            paint_panel.visibility = View.GONE
            paint_warning_frame.visibility = View.GONE

            paint_panel_button.visibility = View.VISIBLE

            recent_colors_button.visibility = View.GONE
            recent_colors_container.visibility = View.GONE

            back_button.visibility = View.VISIBLE

            stopEmittingParticles()
        }

        paint_indicator_view.setOnClickListener {
            // accept selected color
            if (color_picker_frame.visibility == View.VISIBLE) {
                color_picker_frame.visibility = View.GONE

                paint_warning_frame.visibility = View.VISIBLE

                paint_yes.visibility = View.VISIBLE

                paint_color_accept_image.visibility = View.GONE

                surface_view.endPaintSelection()

                paint_no.colorMode = ActionButtonView.ColorMode.COLOR

                recent_colors_container.visibility = View.GONE
                recent_colors_button.visibility = View.VISIBLE

                if (surface_view.interactiveCanvas.restorePoints.size == 0) {
                    paint_yes.visibility = View.GONE
                    paint_no.visibility = View.GONE
                    close_paint_panel.visibility = View.VISIBLE
                }
                else {
                    paint_yes.visibility = View.VISIBLE
                    paint_no.visibility = View.VISIBLE
                    close_paint_panel.visibility = View.GONE
                }

                startParticleEmitters()
            }
            // start color selection mode
            else {
                color_picker_frame.visibility = View.VISIBLE
                initalColor = SessionSettings.instance.paintColor
                color_picker_view.setInitialColor(initalColor)

                paint_warning_frame.visibility = View.GONE

                paint_color_accept_image.visibility = View.VISIBLE

                paint_yes.visibility = View.GONE
                close_paint_panel.visibility = View.GONE
                paint_no.visibility = View.VISIBLE

                paint_no.colorMode = ActionButtonView.ColorMode.NONE

                recent_colors_button.visibility = View.GONE
                recent_colors_container.visibility = View.GONE

                surface_view.startPaintSelection()

                stopEmittingParticles()
            }
        }

        // recent colors
        recent_colors_button.setOnClickListener {
            recent_colors_container.visibility = View.VISIBLE
            recent_colors_button.visibility = View.GONE
        }

        // back button
        back_button.setOnClickListener {
            interactiveCanvasFragmentListener?.onInteractiveCanvasBack()
        }

        context?.apply {
            // paint_panel.layoutParams = ConstraintLayout.LayoutParams(Utils.dpToPx(this, 200), ConstraintLayout.LayoutParams.MATCH_PARENT)
        }

        surface_view.interactiveCanvas.drawCallbackListener = this

        val holder = surface_view.holder
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder?) {
                if (holder != null) {
                    drawInteractiveCanvas(holder)
                }
            }

            override fun surfaceDestroyed(p0: SurfaceHolder?) {

            }

            override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {

            }
        })

        // gesture recognizer
        val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor

                // Don't let the object get too small or too large.
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f))

                return true
            }
        }
    }

    private fun setupRecentColors(recentColors: Array<Int>?) {
        if (recentColors != null) {
            var i = 0
            for (v in recent_colors_container.children) {
                (v as ActionButtonView).type = ActionButtonView.Type.RECENT_COLOR

                if (i < recentColors.size) {
                    v.representingColor = recentColors[recentColors.size - 1 - i]
                }

                v.setOnClickListener {
                    v.representingColor?.apply {
                        SessionSettings.instance.paintColor = this
                        notifyPaintColorUpdate(SessionSettings.instance.paintColor)

                        recent_colors_container.visibility = View.GONE
                        recent_colors_button.visibility = View.VISIBLE
                    }
                }

                i++
            }
        }
        else {
            for (v in recent_colors_container.children) {
                (v as ActionButtonView).type = ActionButtonView.Type.RECENT_COLOR
            }
        }
    }

    fun drawInteractiveCanvas(holder: SurfaceHolder) {
        val paint = Paint()
        paint.color = Color.parseColor("#FFFFFFFF")

        val canvas = holder.lockCanvas()

        val deviceViewport = surface_view.interactiveCanvas.deviceViewport!!
        val ppu = surface_view.interactiveCanvas.ppu

        canvas.drawARGB(255, 0, 0, 0)

        drawUnits(canvas, deviceViewport, ppu)
        drawGridLines(canvas, deviceViewport, ppu)

        holder.unlockCanvasAndPost(canvas)
    }

    private fun drawGridLines(canvas: Canvas, deviceViewport: RectF, ppu: Int) {
        if (surface_view.interactiveCanvas.ppu >= surface_view.interactiveCanvas.gridLineThreshold) {
            val gridLinePaint = Paint()
            gridLinePaint.strokeWidth = 1f
            gridLinePaint.color = Color.WHITE

            val gridLinePaintAlt = Paint()
            gridLinePaintAlt.strokeWidth = 1f
            gridLinePaintAlt.color = Color.WHITE

            if (!world) {
                gridLinePaint.color = surface_view.interactiveCanvas.getGridLineColor()
                gridLinePaintAlt.color = surface_view.interactiveCanvas.getGridLineColor()
            }



            val unitsWide = canvas.width / surface_view.interactiveCanvas.ppu
            val unitsTall = canvas.height / surface_view.interactiveCanvas.ppu

            val gridXOffsetPx = (ceil(deviceViewport.left) - deviceViewport.left) * ppu
            val gridYOffsetPx = (ceil(deviceViewport.top) - deviceViewport.top) * ppu

            for (y in 0..unitsTall) {
                if (y % 2 == 0) {
                    canvas.drawLine(
                        0F,
                        y * ppu.toFloat() + gridYOffsetPx,
                        canvas.width.toFloat(),
                        y * ppu.toFloat() + gridYOffsetPx,
                        gridLinePaint
                    )
                }
                else {
                    canvas.drawLine(
                        0F,
                        y * ppu.toFloat() + gridYOffsetPx,
                        canvas.width.toFloat(),
                        y * ppu.toFloat() + gridYOffsetPx,
                        gridLinePaintAlt
                    )
                }
            }

            for (x in 0..unitsWide) {
                if (x % 2 == 0) {
                    canvas.drawLine(
                        x * ppu.toFloat() + gridXOffsetPx,
                        0F,
                        x * ppu.toFloat() + gridXOffsetPx,
                        canvas.height.toFloat(),
                        gridLinePaint
                    )
                }
                else {
                    canvas.drawLine(
                        x * ppu.toFloat() + gridXOffsetPx,
                        0F,
                        x * ppu.toFloat() + gridXOffsetPx,
                        canvas.height.toFloat(),
                        gridLinePaintAlt
                    )
                }
            }
        }

    }

    private fun drawUnits(canvas: Canvas, deviceViewport: RectF, ppu: Int) {
        val interactiveCanvas = surface_view.interactiveCanvas

        interactiveCanvas.deviceViewport?.apply {
            val startUnitIndexX = floor(left).toInt()
            val endUnitIndexX = ceil(right).toInt()
            val startUnitIndexY = floor(top).toInt()
            val endUnitIndexY = ceil(bottom).toInt()

            val unitsWide = canvas.width / surface_view.interactiveCanvas.ppu

            val rangeX = endUnitIndexX - startUnitIndexX
            val rangeY = endUnitIndexY - startUnitIndexY

            val paint = Paint()
            paint.color = Color.BLACK

            for (x in 0..rangeX) {
                for (y in 0..rangeY) {
                    val unitX = x + startUnitIndexX
                    val unitY = y + startUnitIndexY

                    if (unitX >= 0 && unitX < interactiveCanvas.cols && unitY >= 0 && unitY < interactiveCanvas.rows) {
                        paint.color = interactiveCanvas.arr[unitY][unitX]
                    }
                    else {
                        paint.color = Color.BLACK
                    }
                    val rect = interactiveCanvas.getScreenSpaceForUnit(
                        x + startUnitIndexX,
                        y + startUnitIndexY
                    )

                    canvas.drawRect(rect, paint)
                }
            }
        }
    }

    private fun startParticleEmitters() {
        topLeftParticleSystem = ParticleSystem(activity, 80, R.drawable.particle_semi, 1000)
        topLeftParticleSystem?.setSpeedModuleAndAngleRange(0f, 0.1f, 345, 45)
        topLeftParticleSystem?.setRotationSpeed(144f)
        topLeftParticleSystem?.setAcceleration(0.00005f, 90)
        topLeftParticleSystem?.addModifier(AlphaModifier(0, 255, 0, 1000))
        topLeftParticleSystem?.emit(top_left_anchor, 16)

        topRightParticleSystem = ParticleSystem(activity, 80, R.drawable.particle_semi, 1000)
        topRightParticleSystem?.setSpeedModuleAndAngleRange(0f, 0.1f, 135, 195)
        topRightParticleSystem?.setRotationSpeed(144f)
        topRightParticleSystem?.setAcceleration(0.00005f, 90)
        topRightParticleSystem?.addModifier(AlphaModifier(0, 255, 0, 1000))
        topRightParticleSystem?.emit(top_right_anchor, 16)

        bottomLeftParticleSystem = ParticleSystem(activity, 80, R.drawable.particle_semi, 1000)
        bottomLeftParticleSystem?.setSpeedModuleAndAngleRange(0f, 0.1f, 315, 0)
        bottomLeftParticleSystem?.setRotationSpeed(144f)
        bottomLeftParticleSystem?.setAcceleration(0.00005f, 90)
        bottomLeftParticleSystem?.addModifier(AlphaModifier(0, 255, 0, 1000))
        bottomLeftParticleSystem?.emit(bottom_left_anchor, 16)

        bottomRightParticleSystem = ParticleSystem(activity, 80, R.drawable.particle_semi, 1000)
        bottomRightParticleSystem?.setSpeedModuleAndAngleRange(0f, 0.1f, 180, 225)
        bottomRightParticleSystem?.setRotationSpeed(144f)
        bottomRightParticleSystem?.setAcceleration(0.00005f, 90)
        bottomRightParticleSystem?.addModifier(AlphaModifier(0, 255, 0, 1000))
        bottomRightParticleSystem?.emit(bottom_right_anchor, 16)

    }

    private fun stopEmittingParticles() {
        topLeftParticleSystem?.stopEmitting()
        topRightParticleSystem?.stopEmitting()
        bottomLeftParticleSystem?.stopEmitting()
        bottomRightParticleSystem?.stopEmitting()
    }

    // interactive canvas drawer callback
    override fun notifyRedraw() {
        drawInteractiveCanvas(surface_view.holder)
    }

    override fun notifyPaintColorUpdate(color: Int) {
        color_picker_view.setInitialColor(color)
        paint_indicator_view.setPaintColor(color)
    }

    override fun notifyPaintingStarted() {
        close_paint_panel.visibility = View.GONE
        paint_yes.visibility = View.VISIBLE
        paint_no.visibility = View.VISIBLE
    }

    override fun notifyPaintingEnded() {
        close_paint_panel.visibility = View.VISIBLE
        paint_yes.visibility = View.GONE
        paint_no.visibility = View.GONE
    }

    override fun notifyCloseRecentColors() {
        recent_colors_button.visibility = View.VISIBLE
        recent_colors_container.visibility = View.GONE
    }

    // paint qty listener
    override fun paintQtyChanged(qty: Int) {
        drops_amt_text.text = qty.toString()
    }

    override fun onNewRecentColors(colors: Array<Int>) {
        setupRecentColors(colors)
    }
}