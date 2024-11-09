package com.dicoding.asclepius.view

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.asclepius.R
import com.dicoding.asclepius.adapter.NewsAdapter
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.dicoding.asclepius.response.NewsArticle
import com.dicoding.asclepius.response.NewsResponse
import com.dicoding.asclepius.retrofit.ApiConfig
import com.dicoding.asclepius.viewmodel.MainViewModel
import com.yalantis.ucrop.UCrop
import org.tensorflow.lite.task.vision.classifier.Classifications
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class MainActivity : AppCompatActivity(), ImageClassifierHelper.ClassifierListener {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var currentImageUri: Uri? = null
    private lateinit var imageClassifierHelper: ImageClassifierHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageClassifierHelper = ImageClassifierHelper(context = this, classifierListener = this)

        viewModel.currentImageUri.observe(this) { uri ->
            uri?.let {
                binding.previewImageView.setImageURI(it)
            }
        }

        binding.galleryButton.setOnClickListener { startGallery() }
        binding.analyzeButton.setOnClickListener { analyzeImage() }

    }

    private fun startGallery() {
        // TODO: Mendapatkan gambar dari Gallery.
        currentImageUri = null
        binding.previewImageView.setImageURI(null)
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setImageUri(uri)
            startCropActivity(uri)
        } else {
            Log.d("Photo Picker", "No media selected")
        }
    }

    private fun startCropActivity(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_image.jpg"))

        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)
            setToolbarTitle("Crop Image")
        }

        UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .withAspectRatio(1f, 1f)
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (resultCode) {
            RESULT_OK -> {
                if (requestCode == UCrop.REQUEST_CROP) {
                    val resultUri = UCrop.getOutput(data!!)
                    resultUri?.let {
                        currentImageUri = it
                        viewModel.setImageUri(it)
                    }
                }
            }
            RESULT_CANCELED -> {
                binding.previewImageView.setImageResource(R.drawable.ic_place_holder)
                //binding.previewImageView.setImageURI(null)
                viewModel.setImageUri(null)
                showToast("Crop Dibatalkan")
            }
            UCrop.RESULT_ERROR -> {
                val cropError = UCrop.getError(data!!)
                cropError?.printStackTrace()
            }
        }
    }

    //private fun showImage() {
        // TODO: Menampilkan gambar sesuai Gallery yang dipilih.
        //currentImageUri?.let {
            //Log.d("Image URI", "showImage: $it")
            //binding.previewImageView.setImageURI(it)
        //}
    //}

    private fun analyzeImage() {
        // TODO: Menganalisa gambar yang berhasil ditampilkan.
        viewModel.currentImageUri.value?.let { uri ->
            imageClassifierHelper.classifyStaticImage(uri)
        } ?: showToast("Pilih gambar terlebih dahulu.")
    }

    override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
        var highestLabel = ""
        var highestScore = 0.0

        results?.forEach { classification ->
            classification.categories.forEach { category ->
                if (category.score > highestScore) {
                    highestScore = category.score.toDouble()
                    highestLabel = category.label
                }
            }
        }

        val resultText = if (highestLabel.isNotEmpty()) {
            "Label : $highestLabel \nConfidence Score : ${"%.2f".format(highestScore * 100)}%"
        } else {
            "No result"
        }

        moveToResult(resultText)
    }

    override fun onError(error: String) {
        val errorMessage = when (error) {
            "NO_IMAGE_SELECTED" -> "Tidak ada gambar, silahkan pilih gambar di galeri"
            "INVALID_IMAGE_FORMAT" -> "Format pada gambar tidak didukung, silahkan pilih gambar lain"
            "CLASSIFICATION_FAILED" -> "Proses klasifikasi gagal, silahkan coba lagi"
            else -> "An unexpected error occurred : $error"
        }
        showToast(errorMessage)
        Log.e("Image Processing Error", errorMessage)
    }

    private fun moveToResult(result: String) {
        val uri = viewModel.currentImageUri.value // Get the Uri from ViewModel
        if (uri != null) {
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra("RESULT_TEXT", result)
                putExtra("RESULT_IMAGE_URI", uri.toString())
            }
            startActivity(intent)
        } else {
            showToast("Gambar tidak tersedia")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}