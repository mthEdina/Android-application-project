package com.android.example.cameraxappjava;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCapture;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.android.example.cameraxappjava.databinding.ActivityMainBinding;

import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.core.Preview;
import androidx.camera.core.CameraSelector;
import android.util.Log;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.VideoRecordEvent;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding viewBinding;

    private ImageCapture imageCapture;

    private VideoCapture<Recorder> videoCapture;
    private Recording recording;

    private ExecutorService cameraExecutor;
    private static final int REQUEST_CODE_PERMISSIONS = 10; // You can change the value as needed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissions();
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener(v -> takePhoto());
        viewBinding.videoCaptureButton.setOnClickListener(v -> captureVideo());

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private Bitmap loadBitmapFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            Log.e(TAG, "Error loading bitmap from URI", e);
        }
        return null;
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int desiredWidth, int desiredHeight, Uri savedUri) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = ((float) desiredWidth) / width;
        float scaleHeight = ((float) desiredHeight) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        try {
            int orientation = getOrientationFromUri(savedUri); // Get the orientation of the image
            if (orientation != 0) {
                matrix.postRotate(orientation);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting image orientation", e);
        }

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    private void saveBitmapToFile(Bitmap bitmap, String filePath) {
        try {
            FileOutputStream outputStream = new FileOutputStream(filePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap to file", e);
        }
    }

    private int getOrientationFromUri(Uri uri) throws IOException {
        ExifInterface exifInterface = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            exifInterface = new ExifInterface(getContentResolver().openInputStream(uri));
        }
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    private void takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        ImageCapture imageCapture = this.imageCapture;
        if (imageCapture == null) {
            return;
        }

        // Create time stamped name and MediaStore entry.
        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
        }

        // Create output options object which contains file + metadata
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                .build();

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults output) {
                        String msg = "Photo capture succeeded: " + output.getSavedUri();
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg);
                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                    }
                });
    }

    // Implements VideoCapture use case, including start and stop capturing.
    private void captureVideo() {
        VideoCapture<Recorder> videoCapture = this.videoCapture;
        if (videoCapture == null) {
            return;
        }

        viewBinding.videoCaptureButton.setEnabled(false);

        Recording curRecording = recording;
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop();
            recording = null;
            return;
        }

        // create and start a new recording session
        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");
        }

        MediaStoreOutputOptions mediaStoreOutputOptions = new MediaStoreOutputOptions.Builder(getContentResolver(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build();

        recording = videoCapture.getOutput().prepareRecording(this, mediaStoreOutputOptions)
                .start(ContextCompat.getMainExecutor(this), (VideoRecordEvent recordEvent) -> {
                    if (recordEvent instanceof VideoRecordEvent.Start) {
                        viewBinding.videoCaptureButton.setText(getString(R.string.stop_capture));
                        viewBinding.videoCaptureButton.setEnabled(true);
                    } else if (recordEvent instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) recordEvent;
                        if (!finalizeEvent.hasError()) {
                            String msg = "Video capture succeeded: " + finalizeEvent.getOutputResults().getOutputUri();
                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, msg);
                        } else {
                            if (recording != null) {
                                recording.close();
                                recording = null;
                            }
                            Log.e(TAG, "Video capture ends with error: " + finalizeEvent.getError());
                        }
                        viewBinding.videoCaptureButton.setText(getString(R.string.start_capture));
                        viewBinding.videoCaptureButton.setEnabled(true);
                    }
                });
    }


    private void startCamera() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(MainActivity.this).get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewBinding.viewFinder.getSurfaceProvider());

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST,
                                FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                imageCapture = new ImageCapture.Builder().build();

                // Select back camera as a default
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, preview, imageCapture, videoCapture);

            } catch (Exception exc) {
                Log.e(TAG, "Use case binding failed", exc);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private static final String TAG = "CameraXApp";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    private final ActivityResultLauncher<String[]> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                // Handle Permission granted/rejected
                boolean permissionGranted = true;
                for (Boolean value : permissions.values()) {
                    if (!value) {
                        permissionGranted = false;
                        break;
                    }
                }
                if (!permissionGranted) {
                    Toast.makeText(this, "Permission request denied", Toast.LENGTH_SHORT).show();
                } else {
                    startCamera();
                }
            });

    private static class LuminosityAnalyzer implements ImageAnalysis.Analyzer {

        private final LumaListener listener;

        private byte[] bufferToByteArray(ByteBuffer buffer) {
            buffer.rewind(); // Rewind the buffer to zero
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data); // Copy the buffer into a byte array
            return data;
        }

        public LuminosityAnalyzer(LumaListener listener) {
            this.listener = listener;
        }

        @Override
        public void analyze(ImageProxy image) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = bufferToByteArray(buffer);
            int[] pixels = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                pixels[i] = data[i] & 0xFF;
            }
            double luma = calculateAverageLuminosity(pixels);

            listener.onLumaCalculated(luma);

            image.close();
        }

        private double calculateAverageLuminosity(int[] pixels) {
            int sum = 0;
            for (int pixel : pixels) {
                sum += pixel;
            }
            return (double) sum / pixels.length;
        }
    }
}
