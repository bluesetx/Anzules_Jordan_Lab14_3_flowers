package com.example.anzules_jordan_flowerclassifier_lab14_3

import android.graphics.drawable.BitmapDrawable

import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MainActivity : AppCompatActivity() {

    private lateinit var tflite: Interpreter
    private lateinit var imageView: ImageView
    private lateinit var predictionResult: TextView
    private lateinit var predictButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        predictionResult = findViewById(R.id.predictionResult)
        predictButton = findViewById(R.id.predictButton)

        // Load the model
        try {
            tflite = Interpreter(loadModelFile("flowersmodel.tflite"))
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Load an image (for example, from assets)
        loadImageFromAssets("flowerday1.jpg") // Replace with your image name

        predictButton.setOnClickListener {
            classifyImage()
        }
    }

    @Throws(IOException::class)
    private fun loadModelFile(modelFile: String): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadImageFromAssets(fileName: String) {
        try {
            val isStream = assets.open(fileName)
            val bitmap = BitmapFactory.decodeStream(isStream)
            imageView.setImageBitmap(bitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun classifyImage() {
        // Get the Bitmap from the ImageView
        val bitmap = (imageView.drawable as BitmapDrawable).bitmap

        // Resize the bitmap to the input size of the model (assume 224x224)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        // Prepare input and output arrays
        val input = Array(1) { Array(224) { Array(224) { FloatArray(3) } } }
        val outputProbabilities = Array(1) { FloatArray(5) } // Assuming 5 classes

        // Fill the input array with pixel values
        for (y in 0 until resizedBitmap.height) {
            for (x in 0 until resizedBitmap.width) {
                val pixel = resizedBitmap.getPixel(x, y)
                input[0][y][x][0] = ((pixel shr 16) and 0xFF) / 255.0f // Red
                input[0][y][x][1] = ((pixel shr 8) and 0xFF) / 255.0f  // Green
                input[0][y][x][2] = (pixel and 0xFF) / 255.0f         // Blue
            }
        }

        // Run inference
        tflite.run(input, outputProbabilities)

        // Get the predicted class index
        var predictedClass = 0
        var maxProbability = outputProbabilities[0][0]
        for (i in 1 until outputProbabilities[0].size) {
            if (outputProbabilities[0][i] > maxProbability) {
                maxProbability = outputProbabilities[0][i]
                predictedClass = i
            }
        }

        // Display the result
        val flowerNames = arrayOf("Daisy", "Dandelion", "Rose", "Sunflower", "Tulips")
        predictionResult.text = "Predicted: ${flowerNames[predictedClass]} (${maxProbability * 100}%)"
    }
}
