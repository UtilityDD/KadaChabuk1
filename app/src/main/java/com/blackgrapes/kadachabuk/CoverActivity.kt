package com.blackgrapes.kadachabuk

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class CoverActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cover)

        val coverLayout = findViewById<ConstraintLayout>(R.id.cover_layout)
        coverLayout.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(
                this,
                R.animator.flip_in_from_middle,
                R.animator.flip_out_to_middle
            )
            startActivity(intent, options.toBundle())
            finish()
        }
    }
}