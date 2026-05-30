package com.patrimoniosjc.rfidpoc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.patrimoniosjc.rfidpoc.ble.BleManager
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private var bleManager: BleManager? = null

    // Estados do Compose
    private val isConnectedState = mutableStateOf(false)
    private val statusTextState = mutableStateOf("Desconectado")
    private val logsState = mutableStateListOf<String>()
    private val lastAssetState = mutableStateOf("")

    // Buffer para reconstruir mensagens longas fragmentadas pelo BLE
    private val messageBuffer = StringBuilder()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            appendLog("Permissões concedidas. Pronto para conectar.")
        } else {
            appendLog("ERRO: Permissões negadas.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleManager = BleManager(
            context = this,
            onMessageLog = { msg -> appendLog(msg) },
            onMessageReceived = { msg ->
                handleReceivedMessage(msg)
            },
            onConnectionStateChange = { isConnected ->
                isConnectedState.value = isConnected
                statusTextState.value = if (isConnected) "Scanner Conectado" else "Desconectado"
            }
        )

        checkPermissions()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RfidScannerApp(
                        isConnected = isConnectedState.value,
                        statusText = statusTextState.value,
                        lastAsset = lastAssetState.value,
                        logs = logsState,
                        onConnectClick = {
                            if (isConnectedState.value) {
                                bleManager?.disconnect()
                            } else {
                                bleManager?.startScan()
                            }
                        },
                        onScanClick = { bleManager?.sendCommand("LED_ON") },
                        onStopClick = { bleManager?.sendCommand("LED_OFF") }
                    )
                }
            }
        }
    }

    private fun handleReceivedMessage(msg: String) {
        runOnUiThread {
            when {
                // Marcador de fim de mensagem longa
                msg == "__END__" -> {
                    val fullMessage = messageBuffer.toString()
                    messageBuffer.clear()
                    if (fullMessage.isNotEmpty()) {
                        lastAssetState.value = fullMessage
                        appendLog("[ATIVO] $fullMessage")
                    }
                }
                // Mensagem curta de controle
                msg == "SCANNER_OFF" -> {
                    appendLog("[RX] Scanner desligado.")
                    statusTextState.value = "Scanner Desligado"
                }
                // Fragmento de mensagem longa — acumula no buffer
                else -> {
                    messageBuffer.append(msg)
                }
            }
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
            logsState.add(0, "[$time] $message")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager?.disconnect()
    }
}

@Composable
fun RfidScannerApp(
    isConnected: Boolean,
    statusText: String,
    lastAsset: String,
    logs: List<String>,
    onConnectClick: () -> Unit,
    onScanClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Scanner Patrimonial",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Status: $statusText",
            style = MaterialTheme.typography.titleMedium,
            color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Botão Ligar/Desligar Scanner
        Button(
            onClick = onConnectClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) Color(0xFFD32F2F) else Color(0xFF1976D2)
            )
        ) {
            Text(if (isConnected) "Desligar Scanner" else "Ligar Scanner")
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Botões de ação
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onScanClick,
                enabled = isConnected,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
            ) {
                Text("Escanear")
            }
            Button(
                onClick = onStopClick,
                enabled = isConnected,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161))
            ) {
                Text("Desliga Scanner")
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Card de destaque do último ativo escaneado
        if (lastAsset.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Último Ativo Escaneado",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = lastAsset,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Logs
        Text(
            text = "Logs:",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(logs) { log ->
                Text(text = log, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}