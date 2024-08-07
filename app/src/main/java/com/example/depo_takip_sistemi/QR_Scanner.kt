package com.example.depo_takip_sistemi

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

//bu sınıf qr code okuma kamerayı açar ve qr kodu okut getcode metodu qr ın çözülmüş halini dödürür
//kameranın acılması ve qr okunması icin checkcamerapermission metodunu cağırın
class QR_Scanner(private val activity: ComponentActivity) {

    var textresult by mutableStateOf("")
    private val context: Context = activity

    private val barcodelauncher = activity.registerForActivityResult(ScanContract()) { result ->
        try {
            if (result.contents == null) {
                Toast.makeText(context, "okunmadı", Toast.LENGTH_LONG).show()
            } else {
                textresult = result.contents
            }

        } catch (e: Exception) {
            println(e.message)
        }


    }

    fun showcam() {
        try {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            options.setPrompt("Scan a barcode")
            options.setCameraId(0)
            options.setBeepEnabled(false)
            options.setOrientationLocked(false)
            barcodelauncher.launch(options)
        } catch (e: Exception) {
            println(e.message)
        }
    }

    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showcam()
        }
    }

    fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showcam()
        } else if (activity.shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
            Toast.makeText(context, "Kamera izni gerekli", Toast.LENGTH_LONG).show()
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    fun getcode(): String {
        return textresult
    }
}
