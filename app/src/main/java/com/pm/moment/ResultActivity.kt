package com.pm.moment

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.MediaController
import kotlinx.android.synthetic.main.activity_result.*

import java.io.File

class ResultActivity : AppCompatActivity() {

    companion object {
        var CONCAT_VIDEO_PATH = "CONCAT_VIDEO_PATH"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = "Ваше видео"

        val path = intent.extras.getString(CONCAT_VIDEO_PATH)

        video_view.setVideoPath(path)

        video_view.setMediaController(MediaController(this))
        video_view.requestFocus(0)
        video_view.start()

        share_video.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "video/mp4"
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(path)))
            startActivity(Intent.createChooser(intent, "share"))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
