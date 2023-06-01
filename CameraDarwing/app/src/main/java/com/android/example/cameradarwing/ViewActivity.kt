package com.android.example.cameradarwing

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


class ViewActivity : AppCompatActivity() {

    private lateinit var customView: CustomView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        customView = CustomView(this)
        setContentView(customView)
    }

    class CustomView : View {

        private val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 5f
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        private val saveButtonRect = Rect()
        private val clearButtonRect = Rect()
        private val colorPickerButtonRect = Rect()
        private val buttonMargin = 20
        private val path = Path()


        private var drawingBitmap: Bitmap? = null

        private var currentColor = Color.BLACK

        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
        constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)

            canvas?.drawPath(path, paint)

            canvas?.drawRect(saveButtonRect, paint)
            paint.textSize = 40f
            canvas?.drawText(
                "Save",
                saveButtonRect.centerX().toFloat() - 60,
                saveButtonRect.centerY().toFloat(),
                paint
            )

            canvas?.drawRect(clearButtonRect, paint)
            canvas?.drawText(
                "Delete",
                clearButtonRect.centerX().toFloat() - 60,
                clearButtonRect.centerY().toFloat(),
                paint
            )

            paint.color = currentColor
            canvas?.drawRect(colorPickerButtonRect, paint)
            canvas?.drawText(
                "Color palette",
                colorPickerButtonRect.centerX().toFloat() - 60,
                colorPickerButtonRect.centerY().toFloat(),
                paint
            )
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x
            val y = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (saveButtonRect.contains(x.toInt(), y.toInt())) {
                        saveDrawing()
                    } else if (clearButtonRect.contains(x.toInt(), y.toInt())) {
                        clearDrawing()
                    } else if (colorPickerButtonRect.contains(x.toInt(), y.toInt())) {
                        showColorPickerDialog()
                    } else {
                        path.moveTo(x, y)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!saveButtonRect.contains(x.toInt(), y.toInt()) && !clearButtonRect.contains(
                            x.toInt(),
                            y.toInt()
                        )
                    ) {
                        path.lineTo(x, y)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    // Nothing to do here for now
                }
            }

            invalidate()
            return true
        }

        private fun saveDrawing() {
            if (path.isEmpty) {
                Toast.makeText(context, "Nothing to save.", Toast.LENGTH_SHORT).show()
                return
            }

            if (drawingBitmap == null) {
                drawingBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            }

            val canvas = Canvas(drawingBitmap!!)
            canvas.drawPath(path, paint)
            path.reset()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Drawing_$timeStamp.png"
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val directory = File(path, "CameraX-Image")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)

            try {
                val outputStream: OutputStream?
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CameraX-Image")
                    }
                    val imageUri =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    outputStream = imageUri?.let { resolver.openOutputStream(it) }
                } else {
                    val directory = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "CameraX-Image"
                    )
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    val file = File(directory, fileName)
                    outputStream = FileOutputStream(file)
                }

                drawingBitmap?.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(file))
                outputStream?.flush()
                outputStream?.close()

                Toast.makeText(
                    context,
                    "Drawing saved to Pictures/CameraX-Image/$fileName",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: IOException) {
                Toast.makeText(context, "Failed to save drawing", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/png"),
                null
            )
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)

            // Set the positions and sizes of the buttons
            val buttonWidth = 200
            val buttonHeight = 100

            // Save button
            saveButtonRect.set(
                width - buttonWidth - buttonMargin,
                height - buttonHeight - buttonMargin,
                width - buttonMargin,
                height - buttonMargin
            )

            // Clear button
            clearButtonRect.set(
                buttonMargin,
                height - buttonHeight - buttonMargin,
                buttonMargin + buttonWidth,
                height - buttonMargin
            )

            // Color picker button
            colorPickerButtonRect.set(
                (width - buttonWidth) / 2 - buttonWidth,
                height - buttonHeight - buttonMargin,
                (width - buttonWidth) / 2 + buttonWidth,
                height - buttonMargin
            )
        }


        private fun clearDrawing() {
            path.reset()
            invalidate()
        }

        private fun showColorPickerDialog() {
            val colorPickerDialog = AlertDialog.Builder(context)
                .setTitle("Choose Color")
                .setPositiveButton("OK") { _, _ ->
                    paint.color = currentColor
                    invalidate()
                }
                .setNegativeButton("Cancel", null)
                .setView(createColorPickerView())
                .create()

            colorPickerDialog.show()
        }

        private fun createColorPickerView(): View {
            val colorPickerView = RecyclerView(context)
            colorPickerView.layoutManager = GridLayoutManager(context, 4)
            colorPickerView.adapter = ColorAdapter(getColorList())

            return colorPickerView
        }

        private fun getColorList(): List<Int> {
            // Provide a list of colors for the RecyclerView items
            // For simplicity, I'm using a predefined list, but you can customize it as needed
            return listOf(
                Color.RED,
                Color.BLUE,
                Color.GREEN,
                Color.YELLOW,
                Color.CYAN,
                Color.MAGENTA,
                Color.BLACK,
                Color.WHITE
            )
        }

        private inner class ColorAdapter(private val colorList: List<Int>) :
            RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
                val itemView =
                    View.inflate(parent.context, R.layout.color_item, null)
                return ColorViewHolder(itemView)
            }

            override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
                val color = colorList[position]
                holder.setColor(color)
            }

            override fun getItemCount(): Int = colorList.size

            inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                private val colorView: View = itemView.findViewById(R.id.colorView)

                fun setColor(color: Int) {
                    colorView.setBackgroundColor(color)
                    colorView.setOnClickListener {
                        // The user selected a color
                        currentColor = color
                        paint.color = currentColor
                        invalidate()
                    }
                }
            }
        }
    }
}
