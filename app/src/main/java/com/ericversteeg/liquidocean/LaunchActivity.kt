package com.ericversteeg.liquidocean

import android.content.Intent
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
import kotlinx.android.synthetic.main.activity_launch.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class LaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_launch)
        supportActionBar?.hide()

        launch_image.animate().setDuration(1000).rotationBy(90F).withEndAction {
            startActivity(Intent(this, FullscreenActivity::class.java))
        }.start()
    }
}
