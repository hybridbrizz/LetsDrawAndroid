package com.ericversteeg.liquidocean

import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ericversteeg.liquidocean.fragment.*
import com.ericversteeg.liquidocean.helper.TrustAllSSLCertsDebug
import com.ericversteeg.liquidocean.helper.Utils
import com.ericversteeg.liquidocean.listener.*
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.model.StatTracker
import com.ericversteeg.liquidocean.view.ActionButtonView
import kotlinx.android.synthetic.main.activity_fullscreen.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class FullscreenActivity : AppCompatActivity(), DataLoadingCallback, MenuButtonListener, OptionsListener,
    InteractiveCanvasFragmentListener, StatsFragmentListener, AchievementListener, HowtoFragmentListener {
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

    private val backgrounds = intArrayOf(R.drawable.gradient, R.drawable.gradient_2, R.drawable.gradient_3, R.drawable.gradient_4, R.drawable.gradient_5,
        R.drawable.gradient_6, R.drawable.gradient_7, R.drawable.gradient_8, R.drawable.gradient_9, R.drawable.gradient_10)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // load session settings
        SessionSettings.instance.load(this)

        var rIndex = (Math.random() * backgrounds.size).toInt()

        if (SessionSettings.instance.firstLaunch) {
            rIndex = 8
        }

        SessionSettings.instance.menuBackgroundResId = backgrounds[rIndex]

        mVisible = true

        hide()

        showMenuFragment()

        //TrustAllSSLCertsDebug.trust()

        /*StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork() // or .detectAll() for all detectable problems
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build()
        )*/

        if (SessionSettings.instance.artShowcase == null) {
            SessionSettings.instance.defaultArtShowcase(resources)
        }

        // load stat tracker
        StatTracker.instance.load(this)
        StatTracker.instance.activity = this
        StatTracker.instance.achievementListener = this

        // after device settings have been loaded
        if (!SessionSettings.instance.sentUniqueId) {
            sendDeviceId()
        }
        else {
            getDeviceInfo()
        }

        ActionButtonView(this)
    }

    override fun onPause() {
        super.onPause()

        if (SessionSettings.instance.firstLaunch) {
            SessionSettings.instance.firstLaunch = false
        }

        SessionSettings.instance.save(this)

        StatTracker.instance.save(this)
    }

    private fun showStatsFragment() {
        val frag = StatsFragment()

        frag.statsFragmentListener = this

        supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, frag).commit()
    }

    private fun showHowtoFragment() {
        val frag = HowtoFragment()

        frag.listener = this

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

    private fun showLoadingFragment(world: Boolean, realmId: Int) {
        val frag = LoadingScreenFragment()
        frag.dataLoadingCallback = this
        frag.world = world
        frag.realmId = realmId

        supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, frag).commit()
    }

    private fun showInteractiveCanvasFragment(
        world: Boolean,
        realmId: Int,
        backgroundOption: ActionButtonView.Type? = null
    ) {
        val frag = InteractiveCanvasFragment()
        frag.world = world
        frag.realmId = realmId
        frag.interactiveCanvasFragmentListener = this

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

        val request = object: JsonObjectRequest(
            Request.Method.GET,
            Utils.baseUrlApi + "/api/v1/devices/$uniqueId/info",
            null,
            { response ->
                SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                SessionSettings.instance.xp = response.getInt("xp")

                SessionSettings.instance.displayName = response.getString("name")

                StatTracker.instance.numPixelsPaintedWorld = response.getInt("wt")
                StatTracker.instance.numPixelsPaintedSingle = response.getInt("st")

                // server-side event sync
                StatTracker.instance.reportEvent(this@FullscreenActivity, StatTracker.EventType.PAINT_RECEIVED, response.getInt("tp"))
                StatTracker.instance.reportEvent(this@FullscreenActivity, StatTracker.EventType.PIXEL_OVERWRITE_IN, response.getInt("oi"))
                StatTracker.instance.reportEvent(this@FullscreenActivity, StatTracker.EventType.PIXEL_OVERWRITE_OUT, response.getInt("oo"))

                StatTracker.instance.displayAchievements(this@FullscreenActivity)
            },
            { error ->

            }) {

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                headers["key1"] = Utils.key1
                return headers
            }
        }

        requestQueue.add(request)
    }

    private fun sendDeviceId() {
        val requestQueue = Volley.newRequestQueue(this)

        val uniqueId = SessionSettings.instance.uniqueId

        uniqueId?.apply {
            val requestParams = HashMap<String, String>()

            requestParams["uuid"] = uniqueId

            val paramsJson = JSONObject(requestParams as Map<String, String>)

            val request = object: JsonObjectRequest(
                Request.Method.POST,
                Utils.baseUrlApi + "/api/v1/devices/register",
                paramsJson,
                { response ->
                    SessionSettings.instance.dropsAmt = response.getInt("paint_qty")
                    SessionSettings.instance.xp = response.getInt("xp")

                    StatTracker.instance.numPixelsPaintedWorld = response.getInt("wt")
                    StatTracker.instance.numPixelsPaintedSingle = response.getInt("st")
                    StatTracker.instance.totalPaintAccrued = response.getInt("tp")
                    StatTracker.instance.numPixelOverwritesIn = response.getInt("oi")
                    StatTracker.instance.numPixelOverwritesOut = response.getInt("oo")

                    SessionSettings.instance.sentUniqueId = true
                },
                { error ->

                }) {

                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Content-Type"] = "application/json; charset=utf-8"
                    headers["key1"] = Utils.key1
                    return headers
                }
            }

            request.tag = "download"
            requestQueue.add(request)
        }
    }

    // data load callback
    override fun onDataLoaded(world: Boolean, realmId: Int) {
        showInteractiveCanvasFragment(world, realmId)
    }

    override fun onConnectionError(type: Int) {
        showMenuFragment()
    }

    // menu buttons

    override fun onMenuButtonSelected(index: Int, route: Int) {
        when (index) {
            MenuFragment.playMenuIndex -> {

            }
            MenuFragment.optionsMenuIndex -> {
                showOptionsFragment()
            }
            MenuFragment.statsMenuIndex -> {
                showStatsFragment()
            }
            MenuFragment.howtoMenuIndex -> {
                showHowtoFragment()
            }
            MenuFragment.singleMenuIndex -> {
                showInteractiveCanvasFragment(false, 0, null)
            }
            MenuFragment.worldMenuIndex -> {
                showLoadingFragment(true, 1)
            }
            MenuFragment.devMenuIndex -> {
                showLoadingFragment(true, 2)
            }
            MenuFragment.leftyMenuIndex -> {
                SessionSettings.instance.rightHanded = false
                SessionSettings.instance.selectedHand = true

                if (route == MenuFragment.singleMenuIndex) {
                    showInteractiveCanvasFragment(false, 0, null)
                }
                else if (route == MenuFragment.worldMenuIndex) {
                    showLoadingFragment(true, 1)
                }
                else if (route == MenuFragment.devMenuIndex) {
                    showLoadingFragment(true, 2)
                }
            }
            MenuFragment.rightyMenuIndex -> {
                SessionSettings.instance.rightHanded = true
                SessionSettings.instance.selectedHand = true

                if (route == MenuFragment.singleMenuIndex) {
                    showInteractiveCanvasFragment(false, 0, null)
                }
                else if (route == MenuFragment.worldMenuIndex) {
                    showLoadingFragment(true, 1)
                }
                else if (route == MenuFragment.devMenuIndex) {
                    showLoadingFragment(true, 2)
                }
            }
        }
    }

    override fun onSingleBackgroundOptionSelected(backgroundOption: ActionButtonView.Type) {
        showInteractiveCanvasFragment(false, 0, backgroundOption)
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

    override fun onHowtoBack() {
        showMenuFragment()
    }

    override fun onDisplayAchievement(
        info: Map<String, Any>,
        displayInterval: Long
    ) {
        val eventType = info["event_type"] as StatTracker.EventType
        val value = info["threshold"] as Int
        val thresholdsPassed = info["thresholds_passed"] as Int

        when (eventType) {
            StatTracker.EventType.PAINT_RECEIVED -> {
                achievement_name.text = "Total Paint Accrued"
            }
            StatTracker.EventType.PIXEL_OVERWRITE_IN -> {
                achievement_name.text = "Pixels Overwritten By Others"
            }
            StatTracker.EventType.PIXEL_OVERWRITE_OUT -> {
                achievement_name.text = "Pixels Overwritten By Me"
            }
            StatTracker.EventType.PIXEL_PAINTED_WORLD -> {
                achievement_name.text = "Pixels Painted World"
            }
            StatTracker.EventType.PIXEL_PAINTED_SINGLE -> {
                achievement_name.text = "Pixels Painted Single"
            }
            StatTracker.EventType.WORLD_XP -> {
                achievement_name.text = "World XP"
            }
        }

        if (eventType != StatTracker.EventType.WORLD_XP) {
            achievement_desc.text = "Passed the ${value} threshold"
        }
        else {
            achievement_desc.text = "Congrats on reaching level ${StatTracker.instance.getWorldLevel()}!"
        }

        achievement_icon.setType(eventType, thresholdsPassed)

        achievement_banner.visibility = View.VISIBLE

        Timer().schedule(object: TimerTask() {
            override fun run() {
                runOnUiThread {
                    achievement_banner.visibility = View.GONE
                }
            }

        }, 5000)
    }
}
