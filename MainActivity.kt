package com.example.matrixdevicescan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.random.Random
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var outputText: TextView
    private lateinit var matrixCanvas: View
    private val paint = Paint()
    private val drops = mutableListOf<Int>()
    private val chars = "01ABCDEF".toCharArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        outputText = findViewById(R.id.outputText)
        matrixCanvas = findViewById(R.id.matrixCanvas)
        findViewById<Button>(R.id.scanButton).setOnClickListener { requestPermissionsAndScan() }

        setupMatrixEffect()
        startMatrixAnimation()
    }

    private fun setupMatrixEffect() {
        paint.color = Color.GREEN
        paint.textSize = 16f
        val columns = windowManager.defaultDisplay.width / 16
        for (i in 0 until columns) {
            drops.add(Random.nextInt(windowManager.defaultDisplay.height / 16))
        }
    }

    private fun startMatrixAnimation() {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                withContext(Dispatchers.Main) {
                    matrixCanvas.invalidate()
                }
                Thread.sleep(50)
            }
        }

        matrixCanvas.setOnTouchListener { _, _ -> true } // Prevent interaction
        matrixCanvas.setWillNotDraw(false)
        matrixCanvas.setBackgroundColor(Color.BLACK)
        matrixCanvas.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        matrixCanvas.setOnDrawCanvas { canvas ->
            canvas.drawColor(Color.argb(10, 0, 0, 0))
            for (i in drops.indices) {
                val y = drops[i] * 16
                canvas.drawText(
                    chars[Random.nextInt(chars.size)].toString(),
                    (i * 16).toFloat(),
                    y.toFloat(),
                    paint
                )
                drops[i] = if (y > windowManager.defaultDisplay.height && Random.nextFloat() > 0.975f) 0 else drops[i] + 1
            }
        }
    }

    private fun requestPermissionsAndScan() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission)
        } else {
            scanDeviceInfo()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) scanDeviceInfo() else outputText.text = "Permiso de ubicación denegado"
    }

    private fun scanDeviceInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            val info = StringBuilder()
            info.append("MATRIX DEVICE SCAN\n")
            info.append("=================\n\n")

            // Información básica
            info.append("SYSTEM CORE:\n")
            info.append("Model: ${Build.MODEL}\n")
            info.append("Brand: ${Build.MANUFACTURER}\n")
            info.append("Device: ${Build.DEVICE}\n")
            info.append("Android Version: ${Build.VERSION.RELEASE}\n")
            info.append("API Level: ${Build.VERSION.SDK_INT}\n")

            // Información de red
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            info.append("\nNETWORK LINK:\n")
            info.append("Online: ${networkCapabilities != null}\n")
            networkCapabilities?.let {
                info.append("Type: ${when {
                    it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                    it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                    else -> "Other"
                }}\n")
                info.append("Internet: ${it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}\n")
            }

            // Información de batería
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            info.append("\nPOWER SYSTEM:\n")
            info.append("Level: ${batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%\n")
            info.append("Charging: ${batteryManager.isCharging}\n")

            // Fecha y hora
            info.append("\nTEMPORAL SYSTEM:\n")
            info.append("Current: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}\n")
            info.append("Timezone: ${TimeZone.getDefault().id}\n")

            withContext(Dispatchers.Main) {
                outputText.text = info.toString()
            }
        }
    }
}

// Extensión para manejar el dibujo en el Canvas
fun View.setOnDrawCanvas(onDraw: (Canvas) -> Unit) {
    setOnDrawListener(object : View.OnDrawListener {
        override fun onDraw(canvas: Canvas) {
            onDraw(canvas)
        }
    })
}

interface View.OnDrawListener {
    fun onDraw(canvas: Canvas)
}