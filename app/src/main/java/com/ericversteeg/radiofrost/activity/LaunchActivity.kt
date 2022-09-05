package com.ericversteeg.radiofrost.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ericversteeg.radiofrost.R
import kotlinx.android.synthetic.main.activity_launch.*

class LaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_launch)
        supportActionBar?.hide()

        launch_image.animate().setDuration(1000).rotationBy(90F).withEndAction {
            startActivity(Intent(this, InteractiveCanvasActivity::class.java))
        }.start()
    }
}
