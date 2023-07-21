package com.android.example.cameradarwing

import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.example.cameradarwing.databinding.ActivityMainBinding
import org.opencv.android.OpenCVLoader

class ImageActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var imageCursor: Cursor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        OpenCVLoader.initDebug()

        recyclerView = viewBinding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        val projection = arrayOf<String>(
            MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DISPLAY_NAME
        )
        val selection: String = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ?"
        val selectionArgs = arrayOf(
            "CameraX-Image"
        )

        imageCursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs, null
        ) ?: throw IllegalStateException("Cursor is null")
        imageAdapter = ImageAdapter(this, imageCursor)

        // Gombok elrejtése
        viewBinding.imageCaptureButton.visibility = View.GONE
        viewBinding.videoCaptureButton.visibility = View.GONE
        viewBinding.showImagesButton.visibility = View.GONE
        viewBinding.panelButton.visibility = View.GONE

        recyclerView.adapter = imageAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        imageCursor.close()
    }
}
