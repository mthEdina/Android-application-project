package com.android.example.cameradarwing

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class ImageAdapter(private val context: Context, private var cursor: Cursor) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val imageNameTextView: TextView = itemView.findViewById(R.id.imageNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_image, parent, false)
        return ImageViewHolder(view)
    }

    @SuppressLint("Range")
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        //cursor.moveToPosition(position)

        // for example:
        var imageName = "2024-03-30-22-33-52-230"
        holder.imageNameTextView.text = imageName

        val imageUri = Uri.parse("android.resource://${context.packageName}/drawable/$imageName")

        // Load the image into a Glide ImageView directly
        // Glide is an image-loading library for Android
        Glide.with(context)
            .asBitmap()
            .load(imageUri)
            .override(800, 800)
            .centerCrop()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                    // Convert the bitmap to Mat for OpenCV processing
                    // Mat = basic image container
                    val originalMat = Mat()
                    Utils.bitmapToMat(bitmap, originalMat)

                    // Apply the filter (e.g., grayscale)
                    // val grayMat = Mat()
                    // Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_RGB2GRAY)

                    // Apply the Canny edge detection
                    val grayMat = Mat()
                    Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_RGB2GRAY)
                    //Imgproc.GaussianBlur(grayMat, grayMat, Size(5.0, 5.0), 2.2, 2.0)
                    Imgproc.Canny(grayMat, grayMat, 25.0, 75.0)

                    // Convert the processed Mat back to Bitmap
                    val filteredBitmap = Bitmap.createBitmap(grayMat.cols(), grayMat.rows(), Bitmap.Config.ARGB_8888)
                    Utils.matToBitmap(grayMat, filteredBitmap)

                    // Display the filtered bitmap in the ImageView
                    holder.imageView.setImageBitmap(filteredBitmap)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    Log.d("ImageAdapter", "Image load cleared")
                }
            })
    }


    override fun getItemCount(): Int {
        return cursor.count
    }

}
