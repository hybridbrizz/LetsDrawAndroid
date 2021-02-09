package com.ericversteeg.liquidocean

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.fragment.*
import com.ericversteeg.liquidocean.helper.TrustAllSSLCerts
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.*
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.model.StatTracker
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.activity_fullscreen.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class FullscreenActivity : AppCompatActivity(), DataLoadingCallback, MenuButtonListener, OptionsListener, InteractiveCanvasFragmentListener, StatsFragmentListener {
    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreen_content.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreen_content_controls.visibility = View.VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mVisible = true

        hide()

        showMenuFragment()

        TrustAllSSLCerts.trust()

        // load session settings
        SessionSettings.instance.load(this)

        // load stat tracker
        StatTracker.instance.load(this)

        // after device settings have been loaded
        getDeviceInfo()
    }

    override fun onPause() {
        super.onPause()

        SessionSettings.instance.save(this)

        StatTracker.instance.save(this)
    }

    private fun showStatsFragment() {
        val frag = StatsFragment()

        frag.statsFragmentListener = this

        supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, frag).commit()
    }

    private fun showMenuFragment() {
        val frag = MenuFragment()
        frag.menuButtonListener = this

        supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, frag).commit()
    }

    private fun showOptionsFragment() {
        val frag = OptionsFragment()
        frag.optionsListener = this

        supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, frag).commit()
    }

    private fun showLoadingFragment(world: Boolean) {
        val frag = LoadingScreenFragment()
        frag.dataLoadingCallback = this
        frag.world = world

        supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, frag).commit()
    }

    private fun showInteractiveCanvasFragment(world: Boolean, backgroundOption: ActionButtonView.Type? = null) {
        val frag = InteractiveCanvasFragment()
        frag.world = world
        frag.interactiveCanvasFragmentListener = this

        if (backgroundOption != null) {
            frag.singlePlayBackgroundType = backgroundOption
        }

        supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, frag).commit()
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreen_content_controls.visibility = View.GONE
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, 0)
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    private fun getDeviceInfo() {
        val requestQueue = Volley.newRequestQueue(this)

        val uniqueId = SessionSettings.instance.uniqueId

        uniqueId?.apply {
            val request = JsonObjectRequest(
                Request.Method.GET,
                Utils.baseUrlApi + "/api/v1/devices/$uniqueId/info",
                null,
                { response ->
                    SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                    SessionSettings.instance.xp = response.getInt("xp")

                    StatTracker.instance.numPixelsPaintedWorld = response.getInt("wt")
                    StatTracker.instance.numPixelsPaintedSingle = response.getInt("st")
                    StatTracker.instance.totalPaintAccrued = response.getInt("tp")
                    StatTracker.instance.numPixelOverwritesIn = response.getInt("oi")
                    StatTracker.instance.numPixelOverwritesOut = response.getInt("oo")
                },
                { error ->

                })

            requestQueue.add(request)
        }
    }

    // data load callback
    override fun onDataLoaded(world: Boolean) {
        showInteractiveCanvasFragment(world)
    }

    override fun onConnectionError(type: Int) {
        showMenuFragment()
    }

    // menu buttons

    override fun onMenuButtonSelected(index: Int) {
        when (index) {
            MenuFragment.playMenuIndex -> {

            }
            MenuFragment.optionsMenuIndex -> {
                showOptionsFragment()
            }
            MenuFragment.statsMenuIndex -> {
                showStatsFragment()
            }
            MenuFragment.exitMenuIndex -> {
                finish()
            }
            MenuFragment.singleMenuIndex -> {
                showInteractiveCanvasFragment(false, null)
            }
            MenuFragment.worldMenuIndex -> {
                showLoadingFragment(true)
            }
        }
    }

    override fun onSingleBackgroundOptionSelected(backgroundOption: ActionButtonView.Type) {
        showInteractiveCanvasFragment(false, backgroundOption)
    }

    override fun onResetSinglePlay() {
        showMenuFragment()
    }

    override fun onOptionsBack() {
        showMenuFragment()
    }

    override fun onInteractiveCanvasBack() {
        showMenuFragment()
    }

    override fun onStatsBack() {
        showMenuFragment()
    }
}
