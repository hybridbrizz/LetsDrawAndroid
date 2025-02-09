package com.matrixwarez.pt.activity

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.matrixwarez.pt.R
import com.matrixwarez.pt.fragment.*
import com.matrixwarez.pt.helper.Utils
import com.matrixwarez.pt.listener.*
import com.matrixwarez.pt.model.InteractiveCanvasSocket
import com.matrixwarez.pt.model.Server
import com.matrixwarez.pt.model.SessionSettings
import com.matrixwarez.pt.model.StatTracker
import com.matrixwarez.pt.service.CanvasService
import com.matrixwarez.pt.view.ActionButtonView
import kotlinx.android.synthetic.main.activity_fullscreen.*
import java.util.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class InteractiveCanvasActivity : AppCompatActivity(), DataLoadingCallback, MenuButtonListener, OptionsListener,
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

    private val backgrounds = intArrayOf(
        R.drawable.gradient,
        R.drawable.gradient_2,
        R.drawable.gradient_3,
        R.drawable.gradient_4,
        R.drawable.gradient_8,
        R.drawable.gradient_9,
        R.drawable.gradient_10
    )

    var optionsFragment: OptionsFragment? = null
    var howtoFragment: HowtoFragment? = null

    var canvasFragment: InteractiveCanvasFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Utils.isTablet(this)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        setContentView(R.layout.activity_fullscreen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // load session settings
        SessionSettings.instance.load(this)

        var rIndex = (Math.random() * backgrounds.size).toInt()

        if (SessionSettings.instance.firstLaunch) {
            rIndex = 2
        }

        SessionSettings.instance.menuBackgroundResId = backgrounds[rIndex]

        mVisible = true

        hide()

        //showInteractiveCanvasFragment(false, 0)
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
//        if (!SessionSettings.instance.sentUniqueId) {
//            sendDeviceId(this)
//        }
//        else {
//            getDeviceInfo(this)
//        }

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

    fun showMenuFragment() {
        val frag = MenuFragment()
        frag.menuButtonListener = this

        supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, frag).commit()
    }

    fun showOptionsFragment(canvasFragment: InteractiveCanvasFragment? = null) {
        optionsFragment = OptionsFragment()
        optionsFragment?.optionsListener = this

        if (canvasFragment != null) {
            canvasFragment.childFragmentManager
                .beginTransaction()
                .replace(R.id.options_container, optionsFragment!!)
                .commit()
        }
        else {
            supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, optionsFragment!!).commit()
        }
    }

    private fun showStatsFragment() {
        val frag = StatsFragment()

        frag.statsFragmentListener = this

        supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, frag).commit()
    }

    private fun showHowtoFragment() {
        howtoFragment = HowtoFragment()

        howtoFragment?.listener = this

        supportFragmentManager.beginTransaction().add(R.id.fullscreen_content, howtoFragment!!).commit()
    }

    private fun showLoadingFragment(world: Boolean, realmId: Int) {
        val frag = LoadingScreenFragment()
        frag.dataLoadingCallback = this
        frag.world = world
        frag.realmId = realmId

        supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, frag).commit()
    }

    private fun showLoadingFragment(server: Server) {
        val frag = LoadingScreenFragment()
        frag.dataLoadingCallback = this
        frag.world = true
        frag.server = server
        frag.canvasService = CanvasService(server)

        supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, frag).commit()
    }

    private fun showInteractiveCanvasFragment(
        world: Boolean,
        realmId: Int
    ) {
        val frag = InteractiveCanvasFragment()
        frag.world = world
        frag.realmId = realmId
        frag.interactiveCanvasFragmentListener = this

        InteractiveCanvasSocket.instance.socketConnectCallback = frag

        supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, frag).commit()
    }

    fun showInteractiveCanvasFragment(server: Server) {
        val frag = InteractiveCanvasFragment()
        frag.server = server
        frag.world = true
        frag.interactiveCanvasFragmentListener = this

        supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, frag).commit()
    }

    private fun showTermsOfServiceFragment(server: Server) {
        val frag = TermsOfServiceFragment()
        frag.server = server
        supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, frag).commit()
    }

    // data load callback
    override fun onDataLoaded(world: Boolean, realmId: Int) {
        SessionSettings.instance.save(this)
        showInteractiveCanvasFragment(world, realmId)
    }

    override fun onDataLoaded(server: Server) {
        if (!SessionSettings.instance.agreedToTermOfService) {
            showTermsOfServiceFragment(server)
            return
        }
        SessionSettings.instance.save(this)
        showInteractiveCanvasFragment(server)
    }

    override fun onConnectionError() {
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
                showInteractiveCanvasFragment(false, 0)
            }
            MenuFragment.worldMenuIndex -> {
                showLoadingFragment(true, 1)
                SessionSettings.instance.save(this)
            }
            MenuFragment.devMenuIndex -> {
                showLoadingFragment(true, 2)
            }
            MenuFragment.leftyMenuIndex -> {
                SessionSettings.instance.rightHanded = false
                SessionSettings.instance.selectedHand = true

                SessionSettings.instance.toolboxOpen = true
            }
            MenuFragment.rightyMenuIndex -> {
                SessionSettings.instance.rightHanded = true
                SessionSettings.instance.selectedHand = true

                SessionSettings.instance.toolboxOpen = true
            }
        }
    }

    override fun onServerSelected(server: Server) {
        showLoadingFragment(server)
    }

    override fun onResetSinglePlay() {

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
        howtoFragment?.apply {
            supportFragmentManager.beginTransaction().remove(this).commit()
        }
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
}
