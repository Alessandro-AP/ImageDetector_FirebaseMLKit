package com.project.imagedetector


import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.project.imagedetector.databinding.ActivityMainBinding
import com.project.imagedetector.utils.Constants.realObjects
import com.project.imagedetector.utils.ImageUtils.Companion.renderImage
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // to link the layout components.

    private var uri: Uri = Uri.EMPTY // to refer an image in our storage.
    private var currentImagePath: String = ""

    private var currentObjectToFind : String = ""
    private var result  = mutableListOf<String>()

    lateinit var inputImage : InputImage
    lateinit var imageLabeler: ImageLabeler


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        randomiseObject()
        binding.photoIV.setOnClickListener {
            invokeCamera()
        }

        binding.submitButton.setOnClickListener {
            if(uri != Uri.EMPTY) {
                imageAnalyser()
            }
            else
                Toast.makeText(applicationContext,"You need to take a picture first!", Toast.LENGTH_LONG).show()
        }

    }

    /**
     * Define an ActivityResultLauncher object that contains an ActivityResultContract
     * that allows us to take a picture and save it to the provided content-Uri.
     *
     * The callback function will be invoked when the result is received.
     */
    private val getCameraImage =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                Log.i(CAMERA, "Image location: $uri")
                inputImage = InputImage.fromFilePath(applicationContext,uri)
                renderImage(currentImagePath, binding.photoIV)
            }
        }

    /**
     * Retrieve the image uri using the file provider and launch an intent to start the camera.
     */
    private fun invokeCamera() {
        try {
            val file = createImageFile()
            uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", file)
        } catch (e: Exception) {
            Log.e(CAMERA, "Error: ${e.message}")
        }
        getCameraImage.launch(uri)
    }

    /**
     * Return a temporary file, with '.jpg' extension, created in the external file directory.
     */
    private fun createImageFile(): File {
        val imageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "file_${Date().time}",
            ".jpg",
            imageDirectory
        ).apply {
            currentImagePath = absolutePath
        }
    }

    /**
     * Reset the picture field and delete the picture if necessary.
     */
    private fun clearFields() {
        if (currentImagePath != "") {
            val file = File(currentImagePath)
            if (file.exists())
                file.delete()
        }
        binding.photoIV.setImageDrawable(
            ContextCompat.getDrawable(
                applicationContext,
                R.drawable.camera_placeholder
            )
        )

        randomiseObject()
        binding.resultTextView.text = ""
        binding.resultTextView.visibility = View.GONE

        // Reset variables
        uri = Uri.EMPTY
        currentImagePath = ""
        result = mutableListOf()
    }

    private fun imageAnalyser() {
        imageLabeler
            .process(inputImage)
            .addOnSuccessListener { labels ->
                for(label in labels) {
                    result.add("${label.text} ${(label.confidence * 100F).toLong()}%")
                }
                println(result.joinToString("\n"))
                binding.resultTextView.text = result.joinToString("\n")
                binding.resultTextView.movementMethod =  ScrollingMovementMethod()
                binding.resultTextView.visibility = View.VISIBLE
                checkWin()
                result = mutableListOf()
            }
            .addOnFailureListener{
                Log.d(CLOUD_VISION, "Error : ${it.message}")
            }

    }

    private fun randomiseObject(){
        currentObjectToFind = realObjects[Calendar.getInstance().time.seconds % realObjects.size]
        binding.subtitle.text = currentObjectToFind
    }

    private fun checkWin(){
        if (result.any {  it.lowercase().contains(currentObjectToFind.lowercase()) })
            showDialog(true)
        else
            showDialog(false)
    }

    private fun showDialog(success: Boolean) {
        val view = if (success)
            View.inflate(this@MainActivity, R.layout.success_dialog, null)
        else
            View.inflate(this@MainActivity, R.layout.lose_dialog, null)

        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setView(view)

        val dialog = builder.create()
        dialog.show()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(true)

        val button: Button = view.findViewById(R.id.btn_confirm) as Button
        button.setOnClickListener {
            clearFields()
            dialog.dismiss()
        }
    }

    companion object {
        private const val CAMERA = "CAMERA"
        private const val CLOUD_VISION = "CLOUD_VISION"
    }

}