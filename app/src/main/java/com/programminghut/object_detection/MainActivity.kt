
package com.programminghut.object_detection

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.programminghut.object_detection.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class  MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var model: SsdMobilenetV11Metadata1

    private lateinit var cameraExecutor: ExecutorService
    private val CAMERA_PERMISSION_CODE = 100

    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR))
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)

        // Request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            startCamera()
        }

        // Load the model
        model = SsdMobilenetV11Metadata1.newInstance(this)

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Camera provider is now guaranteed to be available
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Set up the preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.previewView).surfaceProvider)
            }

            // Set up image analysis for object detection
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, { imageProxy ->
                        processImageProxy(imageProxy)
                    })
                }

            // Choose the back camera as default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e("MainActivity", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val bitmap = imageProxyToBitmap(imageProxy)
        getPredictions(bitmap)
        imageProxy.close()  // Don't forget to close the imageProxy when done
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 90, out)
        val imageBytes = out.toByteArray()

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun getPredictions(bitmap: Bitmap) {
        val image = TensorImage.fromBitmap(bitmap)
        val processedImage = imageProcessor.process(image)
        val outputs = model.process(processedImage)

        val locations = outputs.locationsAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray
        val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        val width = mutableBitmap.width
        val height = mutableBitmap.height

        // Draw bounding boxes and calculate object sizes
        scores.forEachIndexed { index, score ->
            if (score > 0.5) {
                val locIndex = index * 4
                val left = locations[locIndex + 1] * width
                val top = locations[locIndex] * height
                val right = locations[locIndex + 3] * width
                val bottom = locations[locIndex + 2] * height

                // Draw bounding box
                canvas.drawRect(left, top, right, bottom, paint)

                // Calculate and display size (width and height)
                val objWidth = right - left
                val objHeight = bottom - top
                paint.style = Paint.Style.FILL
                paint.textSize = 40f
                canvas.drawText("W: %.2f, H: %.2f".format(objWidth, objHeight), left, top - 10, paint)
            }
        }

        // Set the updated image with bounding boxes and sizes
        runOnUiThread {
            imageView.setImageBitmap(mutableBitmap)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
        cameraExecutor.shutdown()
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Log.e("MainActivity", "Camera permission denied")
        }
    }
}

