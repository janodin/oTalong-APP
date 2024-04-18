package com.example.otalong

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.*

// Main activity class of the Android app
class MainActivity : AppCompatActivity() {
    private var filePathCallback: ValueCallback<Array<Uri>>? = null // Callback for file path after image selection or capture
    private lateinit var webView: WebView // WebView to display web content
    private var cameraImageUri: Uri? = null // URI for storing the image captured by camera

    // Called when the activity is starting
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Sets the activity layout

        // Initialize WebView
        webView = findViewById(R.id.webview)

        // Setup functions for webview
        setupWebView()
        requestCameraPermission() // Request camera permission from the user
    }

    // Setup WebView with necessary settings and clients
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.apply {
            settings.apply {
                // Enable JavaScript and DOM storage in WebView
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
            }
            webViewClient = WebViewClient() // Handle navigation actions in WebView
            webChromeClient = object : WebChromeClient() {
                // Handle file chooser action, e.g., for uploading files
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    this@MainActivity.filePathCallback = filePathCallback
                    openImageChooser() // Open gallery or camera for image selection
                    return true
                }
            }
            loadUrl("https://otalong.up.railway.app") // Load the initial URL
        }
    }

    // Request camera access permission from the user
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), PERMISSION_CAMERA_REQUEST_CODE)
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CAMERA_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera permission is required to use the camera", Toast.LENGTH_LONG).show()
        }
    }

    // Handle the result of image selection or camera capture
    private val imageChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        filePathCallback?.onReceiveValue(result.data?.let {
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, it)
        } ?: cameraImageUri?.let { arrayOf(it) })
        filePathCallback = null
        cameraImageUri = null
    }

    // Create intent chooser for selecting an image from gallery or capturing a new one
    private fun openImageChooser() {
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }

        // Intent for capturing an image using the camera
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            // Ensure that there's a camera activity to handle the intent
            intent.resolveActivity(packageManager)?.also {
                // Generate a content URI where the image will be saved
                cameraImageUri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    File.createTempFile("JPEG_temp_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES))
                ).also { uri ->
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri) // Set the file Uri to capture the image
                }
            }
        }

        // Create a chooser intent to select how to get the image
        val chooserIntent = Intent.createChooser(galleryIntent, "Take a new picture or select from gallery").apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        }
        imageChooser.launch(chooserIntent) // Launch the chooser intent
    }



    // Handle the back button press in WebView
    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    companion object {
        private const val PERMISSION_CAMERA_REQUEST_CODE = 100 // Request code for camera permission
    }
}
