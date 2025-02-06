package com.ericversteeg.liquidocean.activity

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ericversteeg.liquidocean.R
import com.ericversteeg.liquidocean.databinding.ActivitySigninBinding
import com.ericversteeg.liquidocean.fragment.SignInFragment
import com.ericversteeg.liquidocean.listener.SignInListener
import com.ericversteeg.liquidocean.model.SessionSettings
import com.ericversteeg.liquidocean.model.StatTracker

class SignInActivity : AppCompatActivity(), SignInListener {

    lateinit var signInFragment: SignInFragment

    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        binding.fullscreenContent.systemUiVisibility =
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
        binding.fullscreenContentControls.visibility = View.VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }

    private lateinit var binding: ActivitySigninBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySigninBinding.inflate(layoutInflater)

        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mVisible = true

        hide()

        showSignInFragment()
    }

    override fun onPause() {
        super.onPause()

        SessionSettings.instance.save(this)

        StatTracker.instance.save(this)
    }

    private fun showSignInFragment() {
        val frag = SignInFragment()

        frag.signInListener = this

        if (intent.hasExtra("mode")) {
            frag.mode = intent.getIntExtra("mode", 0)
        }

        signInFragment = frag
        supportFragmentManager.beginTransaction().replace(R.id.fullscreen_content, frag).commit()
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        binding.fullscreenContentControls.visibility = View.GONE
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, 0)
    }

    override fun onSignInBack() {
        finish()
    }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == signInFragment.signInRequestCode) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            signInFragment.handleSignInResult(task)
        }
    }*/
}