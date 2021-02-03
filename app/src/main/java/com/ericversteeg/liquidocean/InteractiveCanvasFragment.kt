package com.ericversteeg.liquidocean

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.helper.SessionSettings
import com.ericversteeg.liquidocean.listener.InteractiveCanvasDrawerCallback
import kotlinx.android.synthetic.main.fragment_interactive_canvas.*
import org.json.JSONObject
import top.defaults.colorpicker.ColorObserver
import kotlin.collections.HashMap
import kotlin.math.ceil
import kotlin.math.floor


class InteractiveCanvasFragment : Fragment(), InteractiveCanvasDrawerCallback {

    var scaleFactor = 1f

    var initalColor = 0

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

        context?.apply {
            SessionSettings.instance.save(this, surface_view.interactiveCanvas)
        }
    }

    override fun onResume() {
        super.onResume()

        val sv = surface_view

        context?.apply {
            SessionSettings.instance.load(this, surface_view.interactiveCanvas)
            if (SessionSettings.instance.sentUniqueId) {
                getPaintQty()
            }
            else {
                sendDeviceId()
            }
        }
    }

    private fun sendDeviceId() {
        context?.apply {
            val uniqueId = SessionSettings.instance.uniqueId

            uniqueId?.apply {
                val requestQueue = Volley.newRequestQueue(context)

                val requestParams = HashMap<String, String>()

                requestParams["uuid"] = uniqueId

                val paramsJson = JSONObject(requestParams as Map<String, String>)

                val request = JsonObjectRequest(
                    Request.Method.POST,
                    "http://192.168.200.69:5000/api/v1/devices/register",
                    paramsJson,
                    { response ->
                        SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                        drops_amt_text.text = SessionSettings.instance.dropsAmt.toString()

                        SessionSettings.instance.sentUniqueId = true
                    },
                    { error ->
                        Log.i("Error", error.message!!)
                    })

                requestQueue.add(request)
            }
        }
    }

    private fun getPaintQty() {
        context?.apply {
            val uniqueId = SessionSettings.instance.uniqueId

            uniqueId?.apply {
                val requestQueue = Volley.newRequestQueue(context)

                val request = JsonObjectRequest(
                    Request.Method.GET,
                    "http://192.168.200.69:5000/api/v1/devices/$uniqueId/paint",
                    null,
                    { response ->
                        SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                        drops_amt_text.text = SessionSettings.instance.dropsAmt.toString()
                    },
                    { error ->
                        Log.i("Error", error.message!!)
                    })

                requestQueue.add(request)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        paint_panel_button.setOnClickListener {
            paint_panel.visibility = View.VISIBLE
            paint_panel_button.visibility = View.GONE


            drops_amt_text.text = SessionSettings.instance.dropsAmt.toString()


            paint_panel.animate().translationX(paint_panel.width.toFloat() * 0.99F).setDuration(0).withEndAction {
                paint_panel.animate().translationX(0F).setDuration(50).setInterpolator(
                    AccelerateDecelerateInterpolator()
                ).start()
                paint_warning_frame.visibility = View.VISIBLE
                paint_warning_frame.alpha = 0F
                paint_warning_frame.animate().alpha(1F).setDuration(50).start()
            }.start()

            surface_view.startPainting()
        }

        paint_yes.setOnClickListener {
            surface_view.endPainting(true)

            paint_panel_button.visibility = View.VISIBLE
            paint_panel.visibility = View.GONE

            paint_warning_frame.visibility = View.GONE
        }

        paint_no.setOnClickListener {
            if (color_picker_frame.visibility == View.VISIBLE) {
                DrawableCompat.setTint(paint_yes.drawable, Color.WHITE)
                paint_indicator_view.setPaintColor(initalColor)
                color_picker_frame.visibility = View.GONE

                paint_warning_frame.visibility = View.VISIBLE

                paint_yes.visibility = View.VISIBLE

                paint_color_accept_image.visibility = View.GONE

                surface_view.endPaintSelection()
            }
            else {
                surface_view.endPainting(false)

                paint_panel_button.visibility = View.VISIBLE
                paint_panel.visibility = View.GONE

                paint_warning_frame.visibility = View.GONE

                paint_yes.visibility = View.VISIBLE
            }
        }

        paint_indicator_view.setOnClickListener {
            color_picker_frame.visibility = View.VISIBLE
            initalColor = SessionSettings.instance.paintColor
            color_picker_view.setInitialColor(initalColor)

            paint_warning_frame.visibility = View.GONE

            paint_color_accept_image.visibility = View.VISIBLE

            paint_yes.visibility = View.GONE

            surface_view.startPaintSelection()
        }

        paint_color_accept_image.setOnClickListener {
            DrawableCompat.setTint(paint_yes.drawable, Color.WHITE)
            color_picker_frame.visibility = View.GONE

            paint_warning_frame.visibility = View.VISIBLE

            paint_yes.visibility = View.VISIBLE

            paint_color_accept_image.visibility = View.GONE

            surface_view.endPaintSelection()
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

    fun drawInteractiveCanvas(holder: SurfaceHolder) {
        drops_amt_text.text = SessionSettings.instance.dropsAmt.toString()

        val paint = Paint()
        paint.color = Color.parseColor("#FFFFFFFF")

        val canvas = holder.lockCanvas()

        val deviceViewport = surface_view.interactiveCanvas.deviceViewport!!
        val ppu = surface_view.interactiveCanvas.ppu

        canvas.drawARGB(255, 0, 0, 0)

        drawUnits(canvas, deviceViewport, ppu)

        if (surface_view.interactiveCanvas.ppu >= surface_view.interactiveCanvas.gridLineThreshold) {
            val gridLinePaint = Paint()
            gridLinePaint.strokeWidth = 1f
            gridLinePaint.color = Color.WHITE

            val gridLinePaintAlt = Paint()
            gridLinePaintAlt.strokeWidth = 1f
            gridLinePaintAlt.color = Color.WHITE

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

        holder.unlockCanvasAndPost(canvas)
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

    // interactive canvas drawer callback
    override fun notifyRedraw() {
        drawInteractiveCanvas(surface_view.holder)
    }

    override fun notifyPaintColorUpdate(color: Int) {
        color_picker_view.setInitialColor(color)
        paint_indicator_view.setPaintColor(color)
    }

    override fun notifyPaintQtyUpdate(qty: Int) {
        drops_amt_text.text = qty.toString()
    }
}