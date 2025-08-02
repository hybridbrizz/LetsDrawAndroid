package com.matrixwarez.pt.activity

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.matrixwarez.pt.R
import com.matrixwarez.pt.compose.MenuItem
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var hideSplashScreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

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

        supportActionBar?.hide()

        //window.navigationBarColor = Color.DarkGray.toArgb()

        //exitFullscreen()
        //goFullscreen()

        //showInteractiveCanvasFragment(false, 0)
        showMenuFragment(true)

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { view, allInsets ->
            val insets = allInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            if (view.paddingTop == 0) {
                view.setPadding(0, insets.top, 0, 0)
            }
            WindowInsetsCompat.CONSUMED
        }

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                delay(3000)
            }
            hideSplashScreen = true
            Log.d("Splash Screen", "Splash screen timeout.")
        }

        // Set up an OnPreDrawListener to the root view.
        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    // Check whether the initial data is ready.
                    return if (hideSplashScreen) {
                        // The content is ready. Start drawing.
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        // The content isn't ready. Suspend.
                        false
                    }
                }
            }
        )
    }

    override fun onPause() {
        super.onPause()

        if (SessionSettings.instance.firstLaunch) {
            SessionSettings.instance.firstLaunch = false
        }

        SessionSettings.instance.save(this)
        StatTracker.instance.save(this)
    }

    fun showMenuFragment(initial: Boolean = false) {
        if (!Utils.isTablet(this)) {
            portraitLock()
        }

        val frag = MenuFragment()
        frag.menuButtonListener = this
        if (initial) {
            frag.publicServerItemReadyOnScreen.observe(this, object: Observer<Boolean> {
                override fun onChanged(value: Boolean) {
                    if (value) {
                        Log.d("Splash Screen", "Server thumbnails are ready")
                        lifecycleScope.launch {
//                        withContext(Dispatchers.Default) {
//                            delay(500)
//                        }
                            Log.d("Splash Screen", "Hide splash screen")
                            hideSplashScreen = true
                        }
                        frag.publicServerItemReadyOnScreen.removeObserver(this)
                    }
                }
            })
        }

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

    fun showHowtoFragment() {
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
        goFullscreen()

        val frag = LoadingScreenFragment()
        frag.dataLoadingCallback = this
        frag.world = true

        if (server.public) {
            server.uuid = SessionSettings.instance.publicServerUniqueIds[server.id.toString()] ?: ""
        }

        frag.server = server

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
        frag.tempServer = server
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
        Log.d("On Data Loaded", "Callback")
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

    private var blockLoadingFragment = false

    override fun onServerSelected(server: Server) {
        if (!blockLoadingFragment) {
            Log.d("On Data Loaded", "Callback")
            if (!SessionSettings.instance.agreedToTermOfService) {
                showTermsOfServiceFragment(server)
                return
            }
            SessionSettings.instance.save(this)
            showInteractiveCanvasFragment(server)
        }
        blockLoadingFragment = true
    }

    override fun clearBlockLoadingFragment() {
        blockLoadingFragment = false
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

    fun goFullscreen() {
//        // Hide UI first
//        supportActionBar?.hide()
//        fullscreen_content_controls.visibility = View.GONE
//        mVisible = false
//
//        // Schedule a runnable to remove the status and navigation bar after a delay
//        mHideHandler.removeCallbacks(mShowPart2Runnable)
//        mHideHandler.postDelayed(mHidePart2Runnable, 0)
    }

    fun exitFullscreen() {
//        fullscreen_content.systemUiVisibility =
//            View.SYSTEM_UI_FLAG_VISIBLE
    }

    fun colorActionBar(color: Int) {
        findViewById<View>(R.id.root).background = ColorDrawable(color)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
    }
}

fun Activity.portraitLock() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}

fun Activity.isPortrait(): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
}
