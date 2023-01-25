package com.example.appcamera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract.Directory
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.appcamera.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout.TabGravity
import java.io.DataOutput
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture:ImageCapture?= null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        outputDirectory = outputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        if(allPermissionGranted()){
            startCamera()
        }else{
            ActivityCompat.requestPermissions(
                this,Constants.REQUIRED_PERMISSIONS,
                Constants.REQUEST_CODE_PERMISSIONS
            )
        }

        binding.btnTakePhoto.setOnClickListener{
            takePhoto()
        }
    }

    // Functions **************************

    

    private fun outputDirectory(): File{
        val mediaDir = externalMediaDirs.firstOrNull()?.let { mFile ->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }

        return if(mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun takePhoto(){
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(Constants.FILE_NAME_FORMAT,
                Locale.getDefault()).format(System.currentTimeMillis())+".jpg")
        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOption, ContextCompat.getMainExecutor(this),
            object :ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val saveUri = Uri.fromFile(photoFile)
                    val msg = "Foto Guardada"

                    Toast.makeText(this@MainActivity, "$msg $saveUri", Toast.LENGTH_LONG).show()
                    
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d(Constants.TAG, "onError: ${exception.message}", exception)
                }

            }
        )
    }

    private fun startCamera(){

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also { mPreview ->
                mPreview.setSurfaceProvider(
                    binding.viewFinder.surfaceProvider
                )
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,preview,imageCapture
                )
            }catch (e: Exception){
                Log.d(Constants.TAG, "Fallo de inicio de camara:", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if(allPermissionGranted()){
            startCamera()
        } else
        {
            Toast.makeText(this,
            "Permisos no autorizados por el usuario.",
            Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                baseContext, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}