package com.android.example.cameradarwing

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

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
        cursor.moveToPosition(position)

        val imageName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME))
        holder.imageNameTextView.text = imageName

        val imageUri = Uri.withAppendedPath(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media._ID))
        )
        Glide.with(context)
            .load(imageUri)
            .centerCrop()
            .into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return cursor.count
    }

}
