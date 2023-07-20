package com.android.example.cameradarwing

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    class CustomView(context: Context) : View(context) {

        private val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 5f
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        //gombok pozicioja
        private val saveButtonRect = Rect()
        private val clearButtonRect = Rect()


        private val path = Path()

        // Bitmap a rajz tárolásához
        private var drawingBitmap: Bitmap? = null

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)

            // Rajzolás a panelra
            canvas?.drawPath(path, paint)

            // Rajzolás a mentés gombra
            canvas?.drawRect(saveButtonRect, paint)
            paint.textSize = 40f
            canvas?.drawText(
                "Save",
                saveButtonRect.centerX().toFloat() - 60,
                saveButtonRect.centerY().toFloat(),
                paint
            )

            // Rajzolás a törlés gombra
            canvas?.drawRect(clearButtonRect, paint)
            canvas?.drawText(
                "Delete",
                clearButtonRect.centerX().toFloat() - 60,
                clearButtonRect.centerY().toFloat(),
                paint
            )
        }


        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x
            val y = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (saveButtonRect.contains(x.toInt(), y.toInt())) {
                        Log.d("mentes", "saveDraewing meghivasa")
                        saveDrawing()
                        //saveSignature()
                    } else if (clearButtonRect.contains(x.toInt(), y.toInt())) {
                        clearDrawing()
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
            // Bitmap létrehozása vagy frissítése a rajzzal
            if (drawingBitmap == null) {
                drawingBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            }

            // Új canvas, amely a bitmapre rajzol
            val canvas = Canvas(drawingBitmap!!)

            // Rajzolás az útvonalak átmásolásával a canvas-ra
            canvas.drawPath(path, paint)

            // A path resetelése
            path.reset()

            // Mentési útvonal létrehozása
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Drawing_$timeStamp.png"
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val directory = File(path, "CameraX-Image") // Hozzáadva: Az új mappa létrehozása vagy megléte
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)
            Log.d("mentes", fileName)
            Log.d("mentes", file.toString())

            // Készítsd el a PNG fájlt a bitmapből
            try {
                val outputStream: OutputStream?
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CameraX-Image")
                    }
                    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    outputStream = imageUri?.let { resolver.openOutputStream(it) }
                } else {
                    // Ha az Android verzió kevesebb, mint 29, használj FileProvider-t
                    val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CameraX-Image")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    val file = File(directory, fileName)
                    outputStream = FileOutputStream(file)
                }

                drawingBitmap?.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(file))
                outputStream?.flush()
                outputStream?.close()

                // Értesítés a felhasználónak a sikeres mentésről
                Toast.makeText(
                    context,
                    "Drawing saved to Pictures/CameraX-Image/$fileName",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: IOException) {
                // Hibaüzenet, ha nem sikerült a mentés
                Toast.makeText(context, "Failed to save drawing", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
            Log.d("mentes", "elmentve/nem")
        }

        fun saveSignature(): Bitmap? {
            val bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            draw(canvas)
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CameraX-Image")
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(file))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return bitmap
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)

            // Elhelyezés és méret beállítása a mentés gombnak
            val saveButtonWidth = 200
            val saveButtonHeight = 100
            val saveButtonLeft = width - saveButtonWidth - 50
            val saveButtonTop = height - saveButtonHeight - 50
            saveButtonRect.set(
                saveButtonLeft,
                saveButtonTop,
                saveButtonLeft + saveButtonWidth,
                saveButtonTop + saveButtonHeight
            )

            // Elhelyezés és méret beállítása a törlés gombnak
            val clearButtonWidth = 200
            val clearButtonHeight = 100
            val clearButtonLeft = 50
            val clearButtonTop = height - clearButtonHeight - 50
            clearButtonRect.set(
                clearButtonLeft,
                clearButtonTop,
                clearButtonLeft + clearButtonWidth,
                clearButtonTop + clearButtonHeight
            )
        }


        fun clearDrawing() {
            path.reset()
            invalidate()
        }
    }
}
