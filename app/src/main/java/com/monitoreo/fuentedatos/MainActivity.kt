package com.monitoreo.fuentedatos

import android.app.ActivityManager
import android.content.Context
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.monitoreo.fuentedatos.databinding.ActivityMainBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainHandler = Handler(Looper.getMainLooper())
    private var reportingEnabled = false

    private val reportRunnable = object : Runnable {
        override fun run() {
            val snapshot = buildSnapshotData()
            renderSnapshot(snapshot)
            sendSnapshotToConsole(snapshot)

            if (reportingEnabled) {
                mainHandler.postDelayed(this, REPORT_INTERVAL_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.refreshButton.setOnClickListener {
            val snapshot = buildSnapshotData()
            renderSnapshot(snapshot)
            sendSnapshotToConsole(snapshot)
        }

        binding.toggleConsoleButton.setOnClickListener {
            if (reportingEnabled) {
                stopReporting()
            } else {
                startReporting()
            }
        }

        val initialSnapshot = buildSnapshotData()
        renderSnapshot(initialSnapshot)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopReporting()
    }

    private fun startReporting() {
        val consoleUrl = binding.consoleUrlInput.text.toString().trim()
        if (consoleUrl.isBlank()) {
            Toast.makeText(this, "Ingresa la URL de la consola", Toast.LENGTH_SHORT).show()
            return
        }

        reportingEnabled = true
        binding.toggleConsoleButton.text = "Detener monitoreo"
        binding.consoleStatusText.text = "Estado consola: monitoreo activo (cada 15 s)"
        mainHandler.removeCallbacks(reportRunnable)
        mainHandler.post(reportRunnable)
    }

    private fun stopReporting() {
        reportingEnabled = false
        binding.toggleConsoleButton.text = "Iniciar monitoreo"
        binding.consoleStatusText.text = "Estado consola: monitoreo detenido"
        mainHandler.removeCallbacks(reportRunnable)
    }

    private fun isNetworkConnected(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun buildSnapshotData(): SnapshotData {
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val connected = isNetworkConnected(connectivityManager)

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sensorManager.getSensorList(android.hardware.Sensor.TYPE_ALL)

        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses
            ?.sortedByDescending { it.importance }
            ?.take(10)
            ?.map {
                ProcessInfo(
                    name = it.processName,
                    pid = it.pid,
                    importance = it.importance
                )
            }
            .orEmpty()

        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        return SnapshotData(
            timestamp = timestamp,
            deviceId = deviceId,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            batteryLevel = batteryLevel,
            networkConnected = connected,
            sensors = sensors.take(8).map { "${it.name} (${it.vendor})" },
            sensorCount = sensors.size,
            runningProcesses = runningProcesses
        )
    }

    private fun renderSnapshot(snapshot: SnapshotData) {
        val report = buildString {
            appendLine("Actualizado: ${snapshot.timestamp}")
            appendLine("ID del dispositivo: ${snapshot.deviceId}")
            appendLine("Fabricante: ${snapshot.manufacturer}")
            appendLine("Modelo: ${snapshot.model}")
            appendLine("Android: ${snapshot.androidVersion} (SDK ${snapshot.sdkInt})")
            appendLine("Batería: ${snapshot.batteryLevel}%")
            appendLine("Conectividad activa: ${if (snapshot.networkConnected) "Sí" else "No"}")
            appendLine("Sensores detectados: ${snapshot.sensorCount}")
            appendLine()
            appendLine("Primeros sensores:")
            snapshot.sensors.forEachIndexed { index, sensor ->
                appendLine("${index + 1}. $sensor")
            }
            appendLine()
            appendLine("Procesos activos (top 10):")
            snapshot.runningProcesses.forEachIndexed { index, process ->
                appendLine("${index + 1}. ${process.name} | PID ${process.pid} | importancia ${process.importance}")
            }
        }

        binding.infoText.text = report
    }

    private fun sendSnapshotToConsole(snapshot: SnapshotData) {
        val consoleUrl = binding.consoleUrlInput.text.toString().trim()
        if (consoleUrl.isBlank()) {
            binding.consoleStatusText.text = "Estado consola: sin URL configurada"
            return
        }

        val payload = snapshot.toJson().toString()
        thread {
            try {
                val connection = (URL(consoleUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 5_000
                    readTimeout = 5_000
                    doOutput = true
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(payload)
                }

                val code = connection.responseCode
                runOnUiThread {
                    binding.consoleStatusText.text = "Estado consola: reporte enviado (HTTP $code)"
                }
                connection.disconnect()
            } catch (error: Exception) {
                runOnUiThread {
                    binding.consoleStatusText.text = "Estado consola: error al enviar (${error.localizedMessage})"
                }
            }
        }
    }

    private data class SnapshotData(
        val timestamp: String,
        val deviceId: String,
        val manufacturer: String,
        val model: String,
        val androidVersion: String,
        val sdkInt: Int,
        val batteryLevel: Int,
        val networkConnected: Boolean,
        val sensors: List<String>,
        val sensorCount: Int,
        val runningProcesses: List<ProcessInfo>
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("timestamp", timestamp)
                put("deviceId", deviceId)
                put("manufacturer", manufacturer)
                put("model", model)
                put("androidVersion", androidVersion)
                put("sdkInt", sdkInt)
                put("batteryLevel", batteryLevel)
                put("networkConnected", networkConnected)
                put("sensorCount", sensorCount)
                put("sensors", JSONArray().apply {
                    sensors.forEach { put(it) }
                })
                put("runningProcesses", JSONArray().apply {
                    runningProcesses.forEach { process ->
                        put(JSONObject().apply {
                            put("name", process.name)
                            put("pid", process.pid)
                            put("importance", process.importance)
                        })
                    }
                })
            }
        }
    }

    private data class ProcessInfo(
        val name: String,
        val pid: Int,
        val importance: Int
    )

    companion object {
        private const val REPORT_INTERVAL_MS = 15_000L
    }
}
