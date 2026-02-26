package com.monitoreo.fuentedatos

import android.content.Context
import android.app.ActivityManager
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.monitoreo.fuentedatos.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.refreshButton.setOnClickListener {
            loadDeviceSnapshot()
        }

        loadDeviceSnapshot()
    }

    private fun isNetworkConnected(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun loadDeviceSnapshot() {
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val connected = isNetworkConnected(connectivityManager)

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sensorManager.getSensorList(android.hardware.Sensor.TYPE_ALL)

        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val report = buildString {
            appendLine("Actualizado: $timestamp")
            appendLine("ID del dispositivo: $deviceId")
            appendLine("Fabricante: ${Build.MANUFACTURER}")
            appendLine("Modelo: ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Batería: $batteryLevel%")
            appendLine("Conectividad activa: ${if (connected) "Sí" else "No"}")
            appendLine("Sensores detectados: ${sensors.size}")
            appendLine()
            appendLine("Primeros sensores:")
            sensors.take(8).forEachIndexed { index, sensor ->
                appendLine("${index + 1}. ${sensor.name} (${sensor.vendor})")
            }

            appendLine()
            appendLine("Consola de procesos (dispositivos conectados):")
            appendLine(getRunningProcessesReport())
        }

        binding.infoText.text = report
    }

    private fun getRunningProcessesReport(): String {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processes = activityManager.runningAppProcesses.orEmpty()

        if (processes.isEmpty()) {
            return "No se detectaron procesos activos en este momento."
        }

        return buildString {
            appendLine("Total de procesos detectados: ${processes.size}")
            appendLine("Formato: nombre | PID | UID | importancia | paquetes")
            appendLine("----------------------------------------------------")

            processes.sortedBy { it.processName.lowercase(Locale.getDefault()) }
                .forEach { process ->
                    val packages = process.pkgList?.joinToString() ?: "N/D"
                    appendLine(
                        "${process.processName} | ${process.pid} | ${process.uid} | " +
                            "${process.importance} | $packages"
                    )
                }
        }
    }
}
