package com.patrimoniosjc.rfidpoc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.ScrollView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.patrimoniosjc.rfidpoc.ble.BleManager
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvLogs: TextView
    private lateinit var btnConnect: Button
    private lateinit var btnLedOn: Button
    private lateinit var btnLedOff: Button
    
    private var bleManager: BleManager? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            appendLog("Permissões concedidas. Pronto para conectar.")
        } else {
            appendLog("ERRO: Permissões de Bluetooth/Localização negadas.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvLogs = findViewById(R.id.tvLogs)
        btnConnect = findViewById(R.id.btnConnect)
        btnLedOn = findViewById(R.id.btnLedOn)
        btnLedOff = findViewById(R.id.btnLedOff)

        checkPermissions()

        bleManager = BleManager(
            context = this,
            onMessageLog = { msg -> appendLog(msg) },
            onMessageReceived = { msg ->
                appendLog("[RX] $msg")
                if (msg == "OK_LED_ON") tvStatus.text = "LED Ligado"
                if (msg == "OK_LED_OFF") tvStatus.text = "LED Desligado"
            },
            onConnectionStateChange = { isConnected ->
                runOnUiThread {
                    if (isConnected) {
                        tvStatus.text = "Conectado"
                        btnConnect.text = "Desconectar"
                        btnLedOn.isEnabled = true
                        btnLedOff.isEnabled = true
                    } else {
                        tvStatus.text = "Desconectado"
                        btnConnect.text = "Conectar ESP32"
                        btnLedOn.isEnabled = false
                        btnLedOff.isEnabled = false
                    }
                }
            }
        )

        btnConnect.setOnClickListener {
            if (btnConnect.text == "Conectar ESP32") {
                bleManager?.startScan()
            } else {
                bleManager?.disconnect()
            }
        }

        btnLedOn.setOnClickListener {
            bleManager?.sendCommand("LED_ON")
        }

        btnLedOff.setOnClickListener {
            bleManager?.sendCommand("LED_OFF")
        }
    }

    private fun checkPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions)
        } else {
            appendLog("Permissões OK.")
        }
    }

    private fun appendLog(message: String) {
        runOnUiThread {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val currentText = tvLogs.text.toString()
            tvLogs.text = "[$time] $message\n$currentText"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager?.disconnect()
    }
}