package com.scareware.virus

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

/** Fullscreen scary image popup every few seconds */
class ScarePopupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        val img = ImageView(this)
        img.setImageResource(R.drawable.scary_face)
        img.scaleType = ImageView.ScaleType.CENTER_CROP
        setContentView(img)

        // Auto close after 1.2s so it flashes
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 1200)
    }

    override fun onBackPressed() {
        // block
    }
}
