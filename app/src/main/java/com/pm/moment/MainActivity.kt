package com.pm.moment

import android.app.Activity
import android.content.Intent
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val SELECT_FIRST_VIDEO = 1
    private val SELECT_SECOND_VIDEO = 2
    private var firstVideoPath: String? = null
    private var secondVideoPath: String? = null
    private var workWithVideo: WorkWithVideo = WorkWithVideo()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        title = resources.getString(R.string.create_moment)

        first_video.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI), SELECT_FIRST_VIDEO)
        }

        second_video.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI), SELECT_SECOND_VIDEO)
        }

        make_magic.setOnClickListener {
            if (firstVideoPath == null || secondVideoPath == null) {
                Toast.makeText(this, "Выберите два видео", Toast.LENGTH_LONG).show()
            } else {
                val concatVideo = workWithVideo.init(mutableListOf(firstVideoPath, secondVideoPath))
                val intent = Intent(this, ResultActivity::class.java)
                intent.putExtra(ResultActivity.CONCAT_VIDEO_PATH, concatVideo.absolutePath)
//                intent.putExtra(ResultActivity.CONCAT_VIDEO_PATH, firstVideoPath)
                startActivity(intent)
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == SELECT_FIRST_VIDEO) {
                firstVideoPath = getPath(data.data)
                first_video_thumbnail.setImageBitmap(ThumbnailUtils.createVideoThumbnail(firstVideoPath, MediaStore.Video.Thumbnails.MINI_KIND))
            } else if (requestCode == SELECT_SECOND_VIDEO) {
                secondVideoPath = getPath(data.data)
                second_video_thumbnail.setImageBitmap(ThumbnailUtils.createVideoThumbnail(secondVideoPath, MediaStore.Video.Thumbnails.MINI_KIND))
            }
        }
    }

    fun getPath(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = managedQuery(uri, projection, null, null, null)
        if (cursor != null) {
            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(column_index)
        } else
            return null
    }

}
