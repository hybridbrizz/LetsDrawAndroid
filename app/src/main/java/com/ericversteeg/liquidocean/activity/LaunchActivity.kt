package com.ericversteeg.liquidocean.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ericversteeg.liquidocean.databinding.ActivityLaunchBinding

class LaunchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLaunchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLaunchBinding.inflate(layoutInflater)

        setContentView(binding.root)
        supportActionBar?.hide()

        binding.launchImage.animate().setDuration(1000).rotationBy(90F).withEndAction {
            startActivity(Intent(this, InteractiveCanvasActivity::class.java))
        }.start()
    }
}
